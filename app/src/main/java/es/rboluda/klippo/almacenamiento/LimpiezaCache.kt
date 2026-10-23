package es.rboluda.klippo.almacenamiento

import java.io.File

// Borra de la caché lo que tenga más de [edadMaximaMs].
// Cada carpeta de descarga se trata como un bloque: se borra entera o no se toca.
object LimpiezaCache {

    const val UNA_HORA_MS = 60 * 60 * 1000L

    fun limpiar(
        cacheDir: File,
        carpetaDescargas: File,
        idsActivos: Set<String>,
        edadMaximaMs: Long = UNA_HORA_MS,
    ) {
        val limite = System.currentTimeMillis() - edadMaximaMs

        carpetaDescargas.listFiles()?.forEach { carpeta ->
            if (carpeta.name !in idsActivos && ultimaModificacion(carpeta) < limite) {
                carpeta.deleteRecursively()
            }
        }

        // Resto de la caché (temporales del motor, exportaciones futuras): archivo a archivo
        cacheDir.listFiles()?.filter { it != carpetaDescargas }?.forEach { entrada ->
            entrada.walkBottomUp().forEach { f ->
                if (f.isFile && f.lastModified() < limite) f.delete()
                else if (f.isDirectory && f.list().isNullOrEmpty()) f.delete()
            }
        }
    }

    // Fecha del archivo más reciente de la carpeta (una descarga larga sigue "viva")
    private fun ultimaModificacion(carpeta: File): Long =
        carpeta.walkTopDown().maxOfOrNull { it.lastModified() } ?: 0L
}
