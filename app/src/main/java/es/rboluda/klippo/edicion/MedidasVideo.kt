package es.rboluda.klippo.edicion

import android.media.MediaMetadataRetriever
import java.io.File

// Medidas del vídeo ya girado (como se ve en pantalla y como lo procesa FFmpeg)
data class MedidasVideo(val duracionMs: Long, val ancho: Int, val alto: Int) {
    val aspecto: Float get() = if (ancho > 0 && alto > 0) ancho.toFloat() / alto else 0f

    companion object {
        // Lee el propio archivo (sin red); bloquea, llamar fuera del hilo principal
        fun leer(video: File): MedidasVideo {
            val lector = MediaMetadataRetriever()
            return try {
                lector.setDataSource(video.absolutePath)
                fun entero(clave: Int) = lector.extractMetadata(clave)?.toLongOrNull() ?: 0L
                var ancho = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
                var alto = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
                // Los vídeos grabados en vertical a veces vienen girados por metadatos
                val rotacion = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
                if (rotacion == 90 || rotacion == 270) ancho = alto.also { alto = ancho }
                MedidasVideo(entero(MediaMetadataRetriever.METADATA_KEY_DURATION), ancho, alto)
            } catch (e: Exception) {
                MedidasVideo(0, 0, 0)
            } finally {
                lector.release()
            }
        }
    }
}
