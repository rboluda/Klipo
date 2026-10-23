package es.rboluda.klippo.descarga

import java.io.File

sealed interface EstadoDescarga {
    data object Descargando : EstadoDescarga
    data object Completada : EstadoDescarga
    data object Cancelada : EstadoDescarga
    data class Fallida(val mensaje: String) : EstadoDescarga
}

// Una descarga: vive en su propia carpeta dentro de cacheDir/descargas/<id>/
data class Descarga(
    val id: String,
    val url: String,
    val carpeta: File,
    // Momento de inicio (ms desde epoch); ordena la lista
    val creado: Long = System.currentTimeMillis(),
    val titulo: String? = null,
    val estado: EstadoDescarga = EstadoDescarga.Descargando,
    // De 0 a 1; negativo mientras aún no se sabe (analizando el enlace)
    val progreso: Float = -1f,
    val etaSegundos: Long = -1,
    val archivo: File? = null,
)
