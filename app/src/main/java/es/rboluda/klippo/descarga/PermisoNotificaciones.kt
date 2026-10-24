package es.rboluda.klippo.descarga

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit

// El permiso de notificaciones se pide solo una vez, en la primera descarga
// (sea desde Inicio o compartiendo con Klippo desde otra app)
object PermisoNotificaciones {

    private const val PREFERENCIAS = "klippo"
    private const val CLAVE_PEDIDO = "permiso_notificaciones_pedido"
    const val PERMISO = Manifest.permission.POST_NOTIFICATIONS

    fun debePedir(context: Context): Boolean {
        val concedido = ContextCompat.checkSelfPermission(context, PERMISO) == PackageManager.PERMISSION_GRANTED
        return !concedido && !preferencias(context).getBoolean(CLAVE_PEDIDO, false)
    }

    fun marcarPedido(context: Context) {
        preferencias(context).edit { putBoolean(CLAVE_PEDIDO, true) }
    }

    private fun preferencias(context: Context) =
        context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
}
