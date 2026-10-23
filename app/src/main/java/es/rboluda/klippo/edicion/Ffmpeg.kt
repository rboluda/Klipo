package es.rboluda.klippo.edicion

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.File

// Ejecuta el FFmpeg que trae youtubedl-android (la librería no expone forma de hacerlo).
// Requiere que FFmpeg.getInstance().init() ya se haya llamado (lo hace MotorDescarga).
object Ffmpeg {

    // Líneas "clave=valor" que FFmpeg escribe con -progress
    private val LINEA_PROGRESO = Regex("""^[a-z_0-9]+=.*""")

    /**
     * Lanza ffmpeg con [argumentos] y avisa del avance (0..1) según [duracionUs] de salida.
     * Lanza una excepción con las últimas líneas de error si FFmpeg falla.
     * Al cancelar la corrutina se mata el proceso.
     */
    suspend fun ejecutar(
        context: Context,
        argumentos: List<String>,
        duracionUs: Long,
        alProgreso: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val binario = File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        val paquetes = File(context.noBackupFilesDir, "youtubedl-android/packages")
        val comando = listOf(
            binario.absolutePath, "-hide_banner", "-nostdin", "-loglevel", "error",
            "-nostats", "-progress", "pipe:1", "-y",
        ) + argumentos
        val proceso = ProcessBuilder(comando)
            .redirectErrorStream(true)
            .apply {
                // Las mismas bibliotecas que usa youtubedl-android al llamar a FFmpeg
                environment()["LD_LIBRARY_PATH"] = listOf("ffmpeg", "python")
                    .joinToString(":") { File(paquetes, "$it/usr/lib").absolutePath }
                environment()["TMPDIR"] = context.cacheDir.absolutePath
            }
            .start()
        val alCancelar = currentCoroutineContext().job.invokeOnCompletion { proceso.destroy() }
        val errores = ArrayDeque<String>()
        try {
            proceso.inputStream.bufferedReader().forEachLine { linea ->
                when {
                    linea.startsWith("out_time_us=") -> {
                        val us = linea.substringAfter('=').toLongOrNull() ?: return@forEachLine
                        if (duracionUs > 0) alProgreso((us.toFloat() / duracionUs).coerceIn(0f, 1f))
                    }
                    LINEA_PROGRESO.matches(linea) -> Unit
                    linea.isNotBlank() -> {
                        errores.addLast(linea)
                        if (errores.size > 10) errores.removeFirst()
                    }
                }
            }
            val codigo = proceso.waitFor()
            ensureActive()
            if (codigo != 0) {
                error(errores.joinToString("\n").ifBlank { "FFmpeg terminó con código $codigo" })
            }
        } finally {
            alCancelar.dispose()
            proceso.destroy()
        }
    }
}
