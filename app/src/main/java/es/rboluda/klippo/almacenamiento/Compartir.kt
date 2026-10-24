package es.rboluda.klippo.almacenamiento

import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import es.rboluda.klippo.R
import java.io.File

// Comparte un vídeo (o audio) de la caché con el share sheet del sistema o con WhatsApp.
// Los intents se exponen para usarlos también desde las acciones de la notificación.
object Compartir {

    // WhatsApp normal y Business (declarados en <queries> del manifest para poder detectarlos)
    private val PAQUETES_WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun video(context: Context, video: File, titulo: String?) {
        context.startActivity(intentElegir(context, video, titulo))
    }

    // Envía el archivo directamente a WhatsApp (se elige el chat dentro de WhatsApp)
    fun aWhatsApp(context: Context, video: File, titulo: String?) {
        intentWhatsApp(context, video, titulo)?.let(context::startActivity)
    }

    // Paquete de WhatsApp instalado, o null si no hay ninguno
    fun whatsapp(context: Context): String? = PAQUETES_WHATSAPP.firstOrNull { paquete ->
        runCatching { context.packageManager.getPackageInfo(paquete, 0) }.isSuccess
    }

    // Share sheet del sistema; en Android 14+ incluye "Guardar en galería" como acción propia
    fun intentElegir(context: Context, video: File, titulo: String?): Intent {
        val selector = Intent.createChooser(envio(context, video, titulo), null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            selector.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(accionGuardar(context, video)))
        }
        return selector
    }

    fun intentWhatsApp(context: Context, video: File, titulo: String?): Intent? =
        whatsapp(context)?.let { envio(context, video, titulo).setPackage(it) }

    // Broadcast que guarda el archivo en la galería sin abrir nada
    fun pendienteGuardar(context: Context, video: File): PendingIntent = PendingIntent.getBroadcast(
        context,
        video.absolutePath.hashCode(),
        Intent(context, GuardarEnGaleriaReceiver::class.java)
            .putExtra(GuardarEnGaleriaReceiver.EXTRA_RUTA, video.absolutePath),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // Tipo MIME según la extensión: vídeo MP4 o audio M4A
    fun tipo(archivo: File): String = if (archivo.extension == "m4a") "audio/mp4" else "video/mp4"

    private fun envio(context: Context, video: File, titulo: String?): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", video)
        return Intent(Intent.ACTION_SEND).apply {
            type = tipo(video)
            putExtra(Intent.EXTRA_STREAM, uri)
            titulo?.let { putExtra(Intent.EXTRA_TITLE, it) }
            // ClipData hace que el permiso de lectura llegue también a la vista previa del chooser
            clipData = ClipData.newRawUri(titulo, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun accionGuardar(context: Context, video: File): ChooserAction =
        ChooserAction.Builder(
            Icon.createWithResource(context, R.drawable.ic_guardar_galeria),
            context.getString(R.string.galeria_guardar),
            pendienteGuardar(context, video),
        ).build()
}
