package es.rboluda.klippo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import es.rboluda.klippo.ui.KlippoNavegacion
import es.rboluda.klippo.ui.inicio.InicioViewModel
import es.rboluda.klippo.ui.theme.KlippoTheme
import es.rboluda.klippo.util.Enlaces

// Única Activity de la app; la navegación entre pantallas se hace en Compose
class MainActivity : ComponentActivity() {

    private val inicioViewModel: InicioViewModel by viewModels { InicioViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo la primera vez: al rotar no hay que volver a descargar el mismo enlace
        if (savedInstanceState == null) procesarIntent(intent)
        setContent {
            KlippoTheme {
                KlippoNavegacion(inicioViewModel)
            }
        }
    }

    // Con launchMode singleTop, compartir o tocar una notificación con la app abierta llega aquí
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        procesarIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        (application as KlippoApp).limpiarYCargar()
    }

    private fun procesarIntent(intent: Intent?) {
        // Notificación de descarga completada: abrir ese vídeo en el editor
        intent?.getStringExtra(EXTRA_ABRIR)?.let {
            inicioViewModel.abrirDescarga(it)
            return
        }
        // Compartir → "Klippo" desde otra app (ACTION_SEND text/plain)
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val enlace = Enlaces.extraerUrl(intent.getStringExtra(Intent.EXTRA_TEXT)) ?: return
        inicioViewModel.recibirCompartido(enlace)
    }

    companion object {
        // Id de la descarga que hay que abrir en el editor al entrar
        const val EXTRA_ABRIR = "es.rboluda.klippo.ABRIR"
    }
}
