package es.rboluda.klippo.almacenamiento

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Copia un vídeo de la caché a Movies/Klippo vía MediaStore (no necesita permisos)
object GuardarEnGaleria {

    private val FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    suspend fun guardar(context: Context, video: File): Uri = withContext(Dispatchers.IO) {
        val nombre = "klippo_${LocalDateTime.now().format(FORMATO_FECHA)}.mp4"
        val valores = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, nombre)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Klippo")
            // Oculto para la galería hasta terminar de copiar
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val coleccion = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(coleccion, valores) ?: error("No se pudo crear el vídeo en la galería")
        try {
            resolver.openOutputStream(uri)?.use { salida ->
                video.inputStream().use { it.copyTo(salida) }
            } ?: error("No se pudo escribir en la galería")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
