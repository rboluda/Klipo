package es.rboluda.klippo.almacenamiento

import es.rboluda.klippo.descarga.Descarga
import es.rboluda.klippo.descarga.EstadoDescarga
import org.json.JSONObject
import java.io.File

// meta.json de cada descarga: permite reconstruir la lista tras cerrar la app
object MetaDescarga {

    private const val ARCHIVO = "meta.json"

    fun escribir(descarga: Descarga) {
        val json = JSONObject()
            .put("url", descarga.url)
            .put("creado", descarga.creado)
            .put("titulo", descarga.titulo)
        descarga.carpeta.mkdirs()
        File(descarga.carpeta, ARCHIVO).writeText(json.toString())
    }

    // Solo devuelve descargas completadas (carpeta con meta.json y un MP4)
    fun leerCompletada(carpeta: File): Descarga? {
        val meta = File(carpeta, ARCHIVO).takeIf { it.exists() } ?: return null
        val video = carpeta.listFiles { f -> f.extension == "mp4" }?.maxByOrNull { it.length() }
            ?: return null
        return try {
            val json = JSONObject(meta.readText())
            Descarga(
                id = carpeta.name,
                url = json.getString("url"),
                carpeta = carpeta,
                creado = json.optLong("creado", carpeta.lastModified()),
                titulo = json.optString("titulo").ifBlank { null },
                estado = EstadoDescarga.Completada,
                progreso = 1f,
                archivo = video,
            )
        } catch (e: Exception) {
            null
        }
    }
}
