package es.rboluda.klippo.descarga

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import es.rboluda.klippo.MainActivity
import es.rboluda.klippo.R

// Canales y notificaciones de las descargas
object Notificaciones {

    private const val CANAL_PROGRESO = "progreso"
    private const val CANAL_TERMINADAS = "terminadas"
    const val ID_PROGRESO = 1

    fun crearCanales(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannels(
            listOf(
                // Importancia baja: la barra de progreso no debe sonar ni vibrar
                NotificationChannel(
                    CANAL_PROGRESO,
                    context.getString(R.string.canal_progreso),
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                    CANAL_TERMINADAS,
                    context.getString(R.string.canal_terminadas),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        )
    }

    fun progreso(context: Context, descarga: Descarga, activas: Int): Notification {
        val titulo = if (activas > 1) {
            context.resources.getQuantityString(R.plurals.notif_descargando_varias, activas, activas)
        } else {
            context.getString(R.string.notif_descargando)
        }
        val cancelar = PendingIntent.getService(
            context,
            descarga.id.hashCode(),
            DescargaService.intentCancelar(context, descarga.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val porcentaje = (descarga.progreso * 100).toInt()
        return Notification.Builder(context, CANAL_PROGRESO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(descarga.titulo ?: descarga.url)
            .setSubText(if (descarga.progreso >= 0f) "$porcentaje %" else null)
            .setProgress(100, porcentaje.coerceAtLeast(0), descarga.progreso < 0f)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(abrirApp(context))
            .addAction(
                Notification.Action.Builder(null, context.getString(R.string.cancelar), cancelar).build()
            )
            .build()
    }

    // Aviso final (completada o con error), útil cuando se comparte desde otra app
    fun mostrarTerminada(context: Context, descarga: Descarga) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        if (!gestor.areNotificationsEnabled()) return
        val (titulo, texto) = when (val estado = descarga.estado) {
            EstadoDescarga.Completada ->
                context.getString(R.string.notif_completada) to (descarga.titulo ?: descarga.url)
            is EstadoDescarga.Fallida ->
                context.getString(R.string.notif_fallida) to estado.mensaje
            else -> return
        }
        val notificacion = Notification.Builder(context, CANAL_TERMINADAS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setAutoCancel(true)
            .setContentIntent(abrirApp(context))
            .build()
        gestor.notify(descarga.id.hashCode(), notificacion)
    }

    private fun abrirApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
