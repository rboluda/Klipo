package es.rboluda.klippo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import es.rboluda.klippo.ui.inicio.InicioScreen
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
        if (savedInstanceState == null) procesarCompartido(intent)
        setContent {
            KlippoTheme {
                InicioScreen(inicioViewModel)
            }
        }
    }

    // Con launchMode singleTop, compartir con la app ya abierta llega por aquí
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        procesarCompartido(intent)
    }

    override fun onStart() {
        super.onStart()
        (application as KlippoApp).limpiarYCargar()
    }

    // Enlace recibido desde el menú Compartir de otra app (ACTION_SEND text/plain)
    private fun procesarCompartido(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val enlace = Enlaces.extraerUrl(intent.getStringExtra(Intent.EXTRA_TEXT)) ?: return
        inicioViewModel.recibirCompartido(enlace)
    }
}
