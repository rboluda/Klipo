package es.rboluda.klippo.descarga

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Estado del motor (yt-dlp + FFmpeg) visible para la UI
sealed interface EstadoMotor {
    data object Iniciando : EstadoMotor
    data class Listo(val version: String?) : EstadoMotor
    data class Error(val mensaje: String) : EstadoMotor
}

enum class ResultadoActualizacion { ACTUALIZADO, YA_AL_DIA, ERROR }

// Envoltorio sobre youtubedl-android para que el resto de la app no dependa de él
class MotorDescarga(private val context: Context) {

    private val _estado = MutableStateFlow<EstadoMotor>(EstadoMotor.Iniciando)
    val estado: StateFlow<EstadoMotor> = _estado.asStateFlow()

    // La primera vez descomprime Python y yt-dlp (varios segundos): siempre en segundo plano
    fun iniciar(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            _estado.value = try {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
                EstadoMotor.Listo(YoutubeDL.getInstance().version(context))
            } catch (e: Exception) {
                EstadoMotor.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    // Suspende hasta que el motor termine de arrancar; devuelve true si está listo
    suspend fun esperarListo(): Boolean =
        estado.first { it !is EstadoMotor.Iniciando } is EstadoMotor.Listo

    // Descarga el vídeo en [carpeta] y devuelve el MP4 resultante.
    // Bloquea hasta terminar; se cancela con cancelar(id), que lanza CanceledException.
    suspend fun descargar(
        id: String,
        url: String,
        carpeta: File,
        // Cookies de sesión (Instagram, X) si el usuario ha iniciado sesión
        cookies: File?,
        alProgreso: (progreso: Float, etaSegundos: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        check(esperarListo()) { "El motor no está listo" }
        carpeta.mkdirs()
        val peticion = YoutubeDLRequest(url).apply {
            addOption("-f", FORMATO_MP4)
            // Si solo hay WebM/MKV, se cambia el contenedor a MP4 sin recodificar
            addOption("--remux-video", "mp4")
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("-o", File(carpeta, "video.%(ext)s").absolutePath)
            // yt-dlp las lee y, al terminar, guarda en el mismo archivo las que se renueven
            cookies?.let { addOption("--cookies", it.absolutePath) }
            // Guarda el título al terminar (after_move no activa el modo simulación)
            addCommands(
                listOf("--print-to-file", "after_move:%(title)s", File(carpeta, ARCHIVO_TITULO).absolutePath)
            )
        }
        YoutubeDL.getInstance().execute(peticion, id) { progreso, eta, _ ->
            alProgreso((progreso / 100f).coerceIn(0f, 1f), eta)
        }
        carpeta.listFiles { f -> f.extension == "mp4" }?.maxByOrNull { it.length() }
            ?: error("No se encontró el vídeo descargado")
    }

    fun leerTitulo(carpeta: File): String? =
        File(carpeta, ARCHIVO_TITULO).takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }

    fun cancelar(id: String) {
        YoutubeDL.getInstance().destroyProcessById(id)
    }

    // Descarga la última versión estable de yt-dlp desde GitHub
    suspend fun actualizar(): ResultadoActualizacion = withContext(Dispatchers.IO) {
        if (!esperarListo()) return@withContext ResultadoActualizacion.ERROR
        try {
            val resultado = YoutubeDL.getInstance()
                .updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            _estado.value = EstadoMotor.Listo(YoutubeDL.getInstance().version(context))
            if (resultado == YoutubeDL.UpdateStatus.DONE) {
                ResultadoActualizacion.ACTUALIZADO
            } else {
                ResultadoActualizacion.YA_AL_DIA
            }
        } catch (e: Exception) {
            ResultadoActualizacion.ERROR
        }
    }

    companion object {
        // Mejor calidad en MP4, prefiriendo H.264 (se comparte y edita sin problemas);
        // si no existe, cualquier MP4 y, en último caso, lo mejor disponible
        private const val FORMATO_MP4 =
            "bv*[ext=mp4][vcodec^=avc]+ba[ext=m4a]/bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b"
        private const val ARCHIVO_TITULO = "titulo.txt"

        // Extrae la línea "ERROR: ..." de la salida de yt-dlp para mostrarla al usuario
        fun mensajeDeError(e: Throwable): String {
            val texto = e.message.orEmpty()
            val linea = texto.lineSequence().lastOrNull { it.startsWith("ERROR:") }
            return (linea?.removePrefix("ERROR:") ?: texto.lineSequence().firstOrNull())
                ?.trim()
                ?.ifBlank { null }
                ?: e.javaClass.simpleName
        }
    }
}
