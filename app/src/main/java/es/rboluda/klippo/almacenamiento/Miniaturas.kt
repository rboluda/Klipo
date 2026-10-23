package es.rboluda.klippo.almacenamiento

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

// Miniatura JPG de cada descarga, generada en local desde el propio vídeo
object Miniaturas {

    private const val ARCHIVO = "miniatura.jpg"
    private const val LADO_MAX = 480

    fun archivo(carpeta: File): File = File(carpeta, ARCHIVO)

    // Devuelve la miniatura (creándola si no existe) o null si el vídeo no se puede leer
    fun obtener(carpeta: File, video: File): File? {
        val destino = archivo(carpeta)
        if (destino.exists()) return destino
        val lector = MediaMetadataRetriever()
        return try {
            lector.setDataSource(video.absolutePath)
            // Un fotograma al segundo 1 suele ser más representativo que el primero (a menudo negro)
            val fotograma = lector.getScaledFrameAtTime(
                1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, LADO_MAX, LADO_MAX,
            ) ?: return null
            destino.outputStream().use { fotograma.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            fotograma.recycle()
            destino
        } catch (e: Exception) {
            destino.delete()
            null
        } finally {
            lector.release()
        }
    }
}
