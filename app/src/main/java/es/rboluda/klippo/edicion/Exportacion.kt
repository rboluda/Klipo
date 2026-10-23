package es.rboluda.klippo.edicion

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.Serializable
import java.util.Locale

// Recorte de inicio y fin en milisegundos (Serializable para guardarlo al girar la pantalla)
data class Recorte(val inicioMs: Long, val finMs: Long) : Serializable {
    val duracionMs: Long get() = finMs - inicioMs
}

// Todo lo que el usuario ha elegido en el editor. Siempre se traduce a un único
// comando FFmpeg al exportar. La Fase 3 irá añadiendo emojis y pixelado.
data class ParametrosEdicion(
    val recorte: Recorte? = null,
    val encuadre: Encuadre? = null,
) : Serializable {
    val hayCambios: Boolean get() = recorte != null || encuadre != null

    // Texto estable que identifica estos parámetros (nombre del archivo en caché)
    fun clave(): String = listOfNotNull(
        recorte?.let { "r${it.inicioMs}-${it.finMs}" },
        encuadre?.let { "e${it.formato.name}" + listOf(it.x, it.y, it.ancho, it.alto).joinToString("-") { v -> decimal(v) } },
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
        val duracionMs = parametros.recorte?.duracionMs ?: duracion(original)
        try {
            Ffmpeg.ejecutar(context, argumentos(original, temporal, parametros), duracionMs * 1000, alProgreso)
            check(temporal.renameTo(destino)) { "No se pudo guardar el vídeo exportado" }
        } finally {
            temporal.delete()
        }
        return destino
    }

    private fun argumentos(entrada: File, salida: File, parametros: ParametrosEdicion): List<String> =
        buildList {
            // -ss antes de -i: busca rápido y, al recodificar, el corte es exacto al fotograma
            parametros.recorte?.let { addAll(listOf("-ss", segundos(it.inicioMs))) }
            addAll(listOf("-i", entrada.absolutePath))
            parametros.recorte?.let { addAll(listOf("-t", segundos(it.duracionMs))) }

            val filtros = filtrosVideo(parametros)
            if (filtros.isEmpty()) {
                addAll(listOf("-map", "0:v:0"))
            } else {
                addAll(listOf("-filter_complex", "[0:v]${filtros.joinToString(",")}[v]", "-map", "[v]"))
            }
            addAll(
                listOf(
                    "-map", "0:a:0?",
                    // H.264 + AAC: se reproduce y comparte en cualquier sitio
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-b:a", "160k",
                    "-movflags", "+faststart",
                    "-f", "mp4", salida.absolutePath,
                )
            )
        }

    // Cadena de filtros de vídeo. FFmpeg ya ha girado el fotograma según sus metadatos,
    // así que iw/ih coinciden con lo que se ve en el editor.
    private fun filtrosVideo(parametros: ParametrosEdicion): List<String> = buildList {
        parametros.encuadre?.let { e ->
            // Ancho y alto pares: libx264 con yuv420p no admite dimensiones impares
            add(
                "crop=" +
                    "trunc(iw*${decimal(e.ancho)}/2)*2:" +
                    "trunc(ih*${decimal(e.alto)}/2)*2:" +
                    "trunc(iw*${decimal(e.x)}):" +
                    "trunc(ih*${decimal(e.y)})"
            )
        }
    }

    private fun duracion(video: File): Long {
        val lector = MediaMetadataRetriever()
        return try {
            lector.setDataSource(video.absolutePath)
            lector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            lector.release()
        }
    }

    private fun segundos(ms: Long): String = String.format(Locale.ROOT, "%.3f", ms / 1000.0)
}

// Número con punto decimal y 4 cifras, sea cual sea el idioma del móvil
internal fun decimal(valor: Float): String = String.format(Locale.ROOT, "%.4f", valor)
