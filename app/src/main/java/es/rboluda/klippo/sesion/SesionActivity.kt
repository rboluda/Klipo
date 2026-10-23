package es.rboluda.klippo.sesion

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import es.rboluda.klippo.R
import es.rboluda.klippo.ui.theme.KlippoTheme

// Abre la web de la plataforma para que el usuario inicie sesión él mismo.
// Klippo nunca ve la contraseña: al pulsar "Listo" solo copia las cookies del WebView.
class SesionActivity : ComponentActivity() {

    private lateinit var plataforma: Plataforma

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plataforma = Plataforma.valueOf(intent.getStringExtra(EXTRA_PLATAFORMA) ?: run { finish(); return })
        enableEdgeToEdge()
        setContent {
            KlippoTheme {
                var webView by remember { mutableStateOf<WebView?>(null) }

                // Atrás navega dentro de la web antes de cerrar la pantalla
                BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.sesion_iniciar, plataforma.nombre)) },
                            actions = {
                                TextButton(onClick = ::terminar) { Text(stringResource(R.string.sesion_listo)) }
                            },
                        )
                    },
                ) { relleno ->
                    AndroidView(
                        factory = { context -> crearWebView(context).also { webView = it } },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(relleno),
                    )
                }

                DisposableEffect(Unit) {
                    onDispose { webView?.destroy() }
                }
            }
        }
    }

    // La web de login necesita JavaScript y almacenamiento local
    @SuppressLint("SetJavaScriptEnabled")
    private fun crearWebView(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Sin la marca "wv" algunas webs no tratan el WebView como navegador de segunda
        settings.userAgentString = settings.userAgentString.replace("; wv", "")
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webViewClient = WebViewClient()
        loadUrl(plataforma.urlLogin)
    }

    private fun terminar() {
        val guardada = Sesiones.guardarDesdeWebView(this, plataforma)
        val texto = getString(
            if (guardada) R.string.sesion_guardada else R.string.sesion_no_detectada,
            plataforma.nombre,
        )
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
        if (guardada) finish()
    }

    companion object {
        private const val EXTRA_PLATAFORMA = "plataforma"

        fun intent(context: Context, plataforma: Plataforma): Intent =
            Intent(context, SesionActivity::class.java).putExtra(EXTRA_PLATAFORMA, plataforma.name)
    }
}
