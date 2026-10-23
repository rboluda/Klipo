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

// Comparte un vídeo de la caché con el share sheet del sistema
object Compartir {

    // WhatsApp normal y Business (declarados en <queries> del manifest para poder detectarlos)
    private val PAQUETES_WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun video(context: Context, video: File, titulo: String?) {
        val selector = Intent.createChooser(envio(context, video, titulo), null)
        // Android 14+: "Guardar en galería" aparece como acción propia dentro del share sheet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            selector.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(accionGuardar(context, video)))
        }
        context.startActivity(selector)
    }

    // Paquete de WhatsApp instalado, o null si no hay ninguno
    fun whatsapp(context: Context): String? = PAQUETES_WHATSAPP.firstOrNull { paquete ->
        runCatching { context.packageManager.getPackageInfo(paquete, 0) }.isSuccess
    }

    // Envía el vídeo directamente a WhatsApp (se elige el chat dentro de WhatsApp)
    fun aWhatsApp(context: Context, video: File, titulo: String?) {
        val paquete = whatsapp(context) ?: return
        context.startActivity(envio(context, video, titulo).setPackage(paquete))
    }

    private fun envio(context: Context, video: File, titulo: String?): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", video)
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            titulo?.let { putExtra(Intent.EXTRA_TITLE, it) }
            // ClipData hace que el permiso de lectura llegue también a la vista previa del chooser
            clipData = ClipData.newRawUri(titulo, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun accionGuardar(context: Context, video: File): ChooserAction {
        val intent = Intent(context, GuardarEnGaleriaReceiver::class.java)
            .putExtra(GuardarEnGaleriaReceiver.EXTRA_RUTA, video.absolutePath)
        val pendiente = PendingIntent.getBroadcast(
            context,
            video.absolutePath.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return ChooserAction.Builder(
            Icon.createWithResource(context, R.drawable.ic_guardar_galeria),
            context.getString(R.string.galeria_guardar),
            pendiente,
        ).build()
    }
}
