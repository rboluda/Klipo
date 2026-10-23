package es.rboluda.klippo.actualizacion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import es.rboluda.klippo.R

// Recibe el resultado de la sesión de PackageInstaller
class InstalacionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            // El sistema necesita que el usuario confirme: se abre su pantalla de instalación
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            // Si sale bien, Android cierra y sustituye la app: no hay nada que hacer
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> Unit
            else -> {
                val detalle = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                Toast.makeText(
                    context,
                    context.getString(R.string.actualizacion_error_instalar, detalle),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}
