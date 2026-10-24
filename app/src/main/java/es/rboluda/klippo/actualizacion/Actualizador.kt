package es.rboluda.klippo.actualizacion

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import es.rboluda.klippo.BuildConfig
import es.rboluda.klippo.ajustes.AjustesRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class VersionNueva(val version: String, val urlApk: String)

sealed interface EstadoActualizacion {
    data object Ninguna : EstadoActualizacion
    data class Disponible(val nueva: VersionNueva) : EstadoActualizacion
    // Descargando el APK; progreso de 0 a 1
    data class Descargando(val nueva: VersionNueva, val progreso: Float) : EstadoActualizacion
    data class Error(val nueva: VersionNueva, val mensaje: String) : EstadoActualizacion
}

// Comprueba en GitHub Releases si hay una versión más nueva, la descarga y la instala.
// Solo funciona en las releases firmadas (las de depuración llevan otra firma y Android
// no dejaría instalar encima).
class Actualizador(private val context: Context, private val ajustes: AjustesRepositorio) {

    private val _estado = MutableStateFlow<EstadoActualizacion>(EstadoActualizacion.Ninguna)
    val estado: StateFlow<EstadoActualizacion> = _estado.asStateFlow()

    val activo: Boolean get() = !BuildConfig.DEBUG

    // Al abrir la app: como mucho una petición a GitHub al día
    suspend fun comprobarSiToca() {
        if (!activo || !ajustes.buscarActualizaciones.first()) return
        val ahora = System.currentTimeMillis()
        if (ahora - ajustes.ultimaComprobacion() < UN_DIA_MS) return
        ajustes.guardarUltimaComprobacion(ahora)
        runCatching { comprobar() }
    }

    // Devuelve la versión nueva o null si ya está al día; lanza excepción si falla la red
    suspend fun comprobar(): VersionNueva? = withContext(Dispatchers.IO) {
        val conexion = URL(URL_ULTIMA).openConnection() as HttpURLConnection
        val json = try {
            conexion.setRequestProperty("Accept", "application/vnd.github+json")
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            check(conexion.responseCode == 200) { "GitHub respondió ${conexion.responseCode}" }
            JSONObject(conexion.inputStream.bufferedReader().readText())
        } finally {
            conexion.disconnect()
        }
        val version = json.getString("tag_name").removePrefix("v")
        val activos = json.getJSONArray("assets")
        val urlApk = (0 until activos.length())
            .map { activos.getJSONObject(it).getString("browser_download_url") }
            .firstOrNull { it.endsWith(".apk") }
        val nueva = if (urlApk != null && esMasNueva(version, BuildConfig.VERSION_NAME)) {
            VersionNueva(version, urlApk)
        } else {
            null
        }
        // No se pisa una descarga en curso
        if (_estado.value !is EstadoActualizacion.Descargando) {
            _estado.value = nueva?.let { EstadoActualizacion.Disponible(it) } ?: EstadoActualizacion.Ninguna
        }
        nueva
    }

    // Descarga el APK a la caché y abre el instalador del sistema (el usuario confirma)
    suspend fun descargarEInstalar(nueva: VersionNueva) {
        if (_estado.value is EstadoActualizacion.Descargando) return
        _estado.value = EstadoActualizacion.Descargando(nueva, 0f)
        try {
            val apk = descargar(nueva)
            instalar(apk)
            _estado.value = EstadoActualizacion.Disponible(nueva)
        } catch (e: Exception) {
            _estado.value = EstadoActualizacion.Error(nueva, e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun descargar(nueva: VersionNueva): File = withContext(Dispatchers.IO) {
        val destino = File(File(context.cacheDir, "actualizacion").apply { mkdirs() }, "klippo.apk")
        // GitHub redirige el APK a su almacenamiento; HttpURLConnection sigue la redirección https
        val conexion = URL(nueva.urlApk).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 30_000
            check(conexion.responseCode == 200) { "Descarga fallida (${conexion.responseCode})" }
            val total = conexion.contentLengthLong
            conexion.inputStream.use { entrada ->
                destino.outputStream().use { salida ->
                    val bufer = ByteArray(64 * 1024)
                    var leidos = 0L
                    while (true) {
                        val n = entrada.read(bufer)
                        if (n < 0) break
                        salida.write(bufer, 0, n)
                        leidos += n
                        if (total > 0) {
                            _estado.value = EstadoActualizacion.Descargando(nueva, leidos.toFloat() / total)
                        }
                    }
                }
            }
        } finally {
            conexion.disconnect()
        }
        destino
    }

    // PackageInstaller: el sistema pide confirmación (y, la primera vez, permiso para
    // instalar apps desde Klippo) a través de InstalacionReceiver
    private suspend fun instalar(apk: File) = withContext(Dispatchers.IO) {
        val instalador = context.packageManager.packageInstaller
        val parametros = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val idSesion = instalador.createSession(parametros)
        instalador.openSession(idSesion).use { sesion ->
            sesion.openWrite("klippo.apk", 0, apk.length()).use { salida ->
                apk.inputStream().use { it.copyTo(salida) }
                sesion.fsync(salida)
            }
            val resultado = PendingIntent.getBroadcast(
                context,
                idSesion,
                Intent(context, InstalacionReceiver::class.java),
                // MUTABLE: el sistema añade al intent el estado de la instalación
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            sesion.commit(resultado.intentSender)
        }
    }

    companion object {
        private const val URL_ULTIMA = "https://api.github.com/repos/rboluda/Klippo/releases/latest"
        private const val UN_DIA_MS = 24 * 60 * 60 * 1000L

        // Las versiones acaban en la marca de compilación (yyyyMMdd.HHmm): se comparan como texto
        fun esMasNueva(candidata: String, actual: String): Boolean =
            candidata.substringAfterLast('-') > actual.substringAfterLast('-')
    }
}
