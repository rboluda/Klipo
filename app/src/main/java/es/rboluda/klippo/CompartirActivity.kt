package es.rboluda.klippo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import es.rboluda.klippo.ajustes.Calidad
import es.rboluda.klippo.descarga.DescargaService
import es.rboluda.klippo.descarga.PermisoNotificaciones
import es.rboluda.klippo.ui.comun.DialogoCalidad
import es.rboluda.klippo.ui.theme.KlippoTheme
import es.rboluda.klippo.util.Enlaces
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Klippo light" en el menú Compartir: descarga sin abrir la app. Es transparente, así que
 * sobre la app de origen solo aparece el diálogo de calidad (si está activado en Ajustes);
 * después lanza la descarga en segundo plano y se cierra. El aviso final llega por notificación.
 */
class CompartirActivity : ComponentActivity() {

    private var enlace: String? = null
    private var calidadElegida: Calidad? = null

    // Primera descarga: se pide el permiso de notificaciones y, responda lo que responda, se descarga
    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        lanzar()
    }

    private val app get() = application as KlippoApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlace = Enlaces.extraerUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
        if (enlace == null) {
            Toast.makeText(this, R.string.compartir_sin_enlace, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val ultima = app.ajustes.calidad.first()
            if (!app.ajustes.preguntarCalidad.first()) {
                elegir(ultima)
            } else {
                setContent {
                    KlippoTheme {
                        DialogoCalidad(preseleccionada = ultima, onElegir = ::elegir, onCancelar = ::finish)
                    }
                }
            }
        }
    }

    private fun elegir(calidad: Calidad) {
        calidadElegida = calidad
        if (PermisoNotificaciones.debePedir(this)) {
            PermisoNotificaciones.marcarPedido(this)
            // Se quita el diálogo de calidad mientras Android pregunta por el permiso
            setContent { }
            pedirPermiso.launch(PermisoNotificaciones.PERMISO)
        } else {
            lanzar()
        }
    }

    private fun lanzar() {
        val url = enlace
        val calidad = calidadElegida
        if (url != null && calidad != null) {
            // En el ámbito de la app: la actividad se cierra enseguida
            app.ambito.launch { app.ajustes.guardarCalidad(calidad) }
            DescargaService.descargar(this, url, calidad)
            Toast.makeText(this, R.string.compartir_descargando, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
