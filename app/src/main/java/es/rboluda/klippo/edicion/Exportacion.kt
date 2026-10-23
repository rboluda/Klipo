package es.rboluda.klippo.edicion

import android.content.Context
import java.io.File
import java.io.Serializable
import java.util.Locale

// Recorte de inicio y fin en milisegundos (Serializable para guardarlo al girar la pantalla)
data class Recorte(val inicioMs: Long, val finMs: Long) : Serializable {
    val duracionMs: Long get() = finMs - inicioMs
}

// Todo lo que el usuario ha elegido en el editor. La Fase 3 añadirá encuadre, emojis y pixelado;
// siempre se traduce a un único comando FFmpeg al exportar.
data class ParametrosEdicion(val recorte: Recorte? = null) {
    val hayCambios: Boolean get() = recorte != null
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
        val recorte = parametros.recorte
        val nombre = buildString {
            append(id)
            recorte?.let { append("_${it.inicioMs}_${it.finMs}") }
        }
        val destino = File(carpeta(context).apply { mkdirs() }, "$nombre.mp4")
        if (destino.exists()) return destino

        // Se escribe en un temporal y se renombra al terminar: nunca queda un MP4 a medias
        val temporal = File(destino.parentFile, "$nombre.tmp")
        val duracionUs = (recorte?.duracionMs ?: 0L) * 1000
        try {
            Ffmpeg.ejecutar(context, argumentos(original, temporal, parametros), duracionUs, alProgreso)
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
            addAll(
                listOf(
                    "-map", "0:v:0", "-map", "0:a:0?",
                    // H.264 + AAC: se reproduce y comparte en cualquier sitio
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-b:a", "160k",
                    "-movflags", "+faststart",
                    "-f", "mp4", salida.absolutePath,
                )
            )
        }

    private fun segundos(ms: Long): String = String.format(Locale.ROOT, "%.3f", ms / 1000.0)
}
