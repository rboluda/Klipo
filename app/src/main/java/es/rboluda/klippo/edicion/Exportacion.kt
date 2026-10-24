package es.rboluda.klippo.edicion

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable
import java.util.Locale

// Recorte de inicio y fin en milisegundos (Serializable para guardarlo al girar la pantalla)
data class Recorte(val inicioMs: Long, val finMs: Long) : Serializable {
    val duracionMs: Long get() = finMs - inicioMs
}

// Todo lo que el usuario ha elegido en el editor. Siempre se traduce a un único
// comando FFmpeg al exportar.
data class ParametrosEdicion(
    val recorte: Recorte? = null,
    val encuadre: Encuadre? = null,
    // Emojis y textos superpuestos
    val emojis: List<EmojiSuperpuesto> = emptyList(),
    val pixelados: List<ZonaPixelada> = emptyList(),
    // Quitar el sonido del vídeo exportado
    val silenciar: Boolean = false,
) : Serializable {
    val hayCambios: Boolean
        get() = recorte != null || encuadre != null || emojis.isNotEmpty() || pixelados.isNotEmpty() ||
            silenciar

    // Texto estable que identifica estos parámetros (nombre del archivo en caché)
    fun clave(): String = listOfNotNull(
        recorte?.let { "r${it.inicioMs}-${it.finMs}" },
        encuadre?.let { e ->
            "e${e.formato.name}" + listOf(e.x, e.y, e.ancho, e.alto).joinToString("-") { decimal(it) }
        },
        emojis.takeIf { it.isNotEmpty() }?.joinToString("|") { m ->
            m.texto + (m.estilo?.name ?: "") + listOf(m.x, m.y, m.tamano).joinToString("-") { decimal(it) }
        },
        "mudo".takeIf { silenciar },
        pixelados.takeIf { it.isNotEmpty() }?.joinToString("|") { p ->
            "p" + listOf(p.zona.x, p.zona.y, p.zona.ancho, p.zona.alto).joinToString("-") { decimal(it) }
        },
    ).joinToString("_")
}

object Exportacion {

    private const val CARPETA = "exportaciones"

    fun carpeta(context: Context): File = File(context.cacheDir, CARPETA)

    /**
     * Genera el vídeo editado en la caché y lo devuelve. Si ya existe uno con los mismos
     * parámetros (p. ej. al volver atrás y exportar otra vez), se reutiliza.
     */
    suspend fun exportar(
        context: Context,
        id: String,
        original: File,
        parametros: ParametrosEdicion,
        alProgreso: (Float) -> Unit,
    ): File {
        val nombre = "${id}_${parametros.clave().hashCode().toUInt().toString(16)}"
        val destino = File(carpeta(context).apply { mkdirs() }, "$nombre.mp4")
        if (destino.exists()) return destino

        // Se escribe en un temporal y se renombra al terminar: nunca queda un MP4 a medias
        val temporal = File(destino.parentFile, "$nombre.tmp")
        val emojisPng = mutableListOf<File>()
        try {
            val medidas = withContext(Dispatchers.IO) { MedidasVideo.leer(original) }
            val emojis = withContext(Dispatchers.IO) {
                parametros.emojis.mapIndexed { i, emoji ->
                    val png = File(destino.parentFile, "${nombre}_emoji$i.png").also { emojisPng += it }
                    RenderEmoji.renderizar(emoji, medidas, png)
                }
            }
            val duracionMs = parametros.recorte?.duracionMs ?: medidas.duracionMs
            Ffmpeg.ejecutar(
                context,
                argumentos(original, temporal, parametros, emojis, medidas),
                duracionMs * 1000,
                alProgreso,
            )
            check(temporal.renameTo(destino)) { "No se pudo guardar el vídeo exportado" }
        } finally {
            temporal.delete()
            emojisPng.forEach { it.delete() }
        }
        return destino
    }

    private fun argumentos(
        entrada: File,
        salida: File,
        parametros: ParametrosEdicion,
        emojis: List<EmojiRenderizado>,
        medidas: MedidasVideo,
    ): List<String> = buildList {
        // -ss y -t delante de -i: se aplican solo al vídeo (no a las imágenes de los emojis);
        // al recodificar, el corte es exacto al fotograma
        parametros.recorte?.let {
            addAll(listOf("-ss", segundos(it.inicioMs), "-t", segundos(it.duracionMs)))
        }
        addAll(listOf("-i", entrada.absolutePath))
        // Cada emoji es una entrada más (1, 2, 3...); overlay repite la imagen todo el vídeo
        emojis.forEach { addAll(listOf("-i", it.png.absolutePath)) }

        val grafo = grafoVideo(parametros, emojis, medidas)
        if (grafo == null) {
            addAll(listOf("-map", "0:v:0"))
        } else {
            addAll(listOf("-filter_complex", grafo.first, "-map", "[${grafo.second}]"))
        }
        if (grafo == null && parametros.recorte == null) {
            // Solo se quita el sonido: el vídeo se copia tal cual, sin recodificar (instantáneo)
            addAll(listOf("-c:v", "copy"))
        } else {
            // H.264: se reproduce y comparte en cualquier sitio
            addAll(listOf("-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p"))
        }
        if (parametros.silenciar) {
            add("-an")
        } else {
            addAll(listOf("-map", "0:a:0?", "-c:a", "aac", "-b:a", "160k"))
        }
        addAll(listOf("-movflags", "+faststart", "-f", "mp4", salida.absolutePath))
    }

    /**
     * Un único filter_complex con todos los pasos, en este orden:
     * pixelado → emojis (ambos sobre el fotograma completo) → encuadre. Devuelve el grafo y la
     * etiqueta final, o null si no hay nada que aplicar. FFmpeg ya ha girado el fotograma según
     * sus metadatos, así que iw/ih coinciden con lo que se ve en el editor.
     */
    private fun grafoVideo(
        parametros: ParametrosEdicion,
        emojis: List<EmojiRenderizado>,
        medidas: MedidasVideo,
    ): Pair<String, String>? {
        val pasos = mutableListOf<String>()
        var actual = "0:v"
        var contador = 0
        fun etiqueta() = "v${contador++}"
        fun paso(filtro: String) {
            val salida = etiqueta()
            pasos += "$filtro[$salida]"
            actual = salida
        }

        // Pixelado: se copia el fotograma, se recorta la zona, se reduce y se vuelve a ampliar
        // sin suavizar (bloques) y se pega encima en el mismo sitio
        parametros.pixelados.forEach { p ->
            val z = ZonaEnPixeles.de(p.zona, medidas)
            val base = etiqueta()
            val copia = etiqueta()
            val bloques = etiqueta()
            pasos += "[$actual]split[$base][$copia]"
            pasos += "[$copia]crop=${z.ancho}:${z.alto}:${z.x}:${z.y}," +
                "scale=${(z.ancho / z.bloque).coerceAtLeast(1)}:${(z.alto / z.bloque).coerceAtLeast(1)}:flags=neighbor," +
                "scale=${z.ancho}:${z.alto}:flags=neighbor[$bloques]"
            paso("[$base][$bloques]overlay=${z.x}:${z.y}")
        }
        emojis.forEachIndexed { i, e -> paso("[$actual][${i + 1}:v]overlay=${e.x}:${e.y}") }
        parametros.encuadre?.let { e ->
            // Ancho y alto pares: libx264 con yuv420p no admite dimensiones impares
            paso(
                "[$actual]crop=" +
                    "trunc(iw*${decimal(e.ancho)}/2)*2:" +
                    "trunc(ih*${decimal(e.alto)}/2)*2:" +
                    "trunc(iw*${decimal(e.x)}):" +
                    "trunc(ih*${decimal(e.y)})"
            )
        }
        return if (pasos.isEmpty()) null else pasos.joinToString(";") to actual
    }

    private fun segundos(ms: Long): String = String.format(Locale.ROOT, "%.3f", ms / 1000.0)
}

// Número con punto decimal y 4 cifras, sea cual sea el idioma del móvil
internal fun decimal(valor: Float): String = String.format(Locale.ROOT, "%.4f", valor)
