package es.rboluda.klippo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.rboluda.klippo.ui.inicio.InicioScreen
import es.rboluda.klippo.ui.theme.KlippoTheme

// Única Activity de la app; la navegación entre pantallas se hace en Compose
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KlippoTheme {
                InicioScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as KlippoApp).limpiarYCargar()
    }
}
