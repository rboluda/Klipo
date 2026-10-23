package es.rboluda.klippo.sesion

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                var progreso by remember { mutableIntStateOf(0) }
                var errorCarga by remember { mutableStateOf(false) }

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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(relleno),
                    ) {
                        AndroidView(
                            factory = { context ->
                                crearWebView(
                                    context,
                                    alProgreso = { progreso = it },
                                    alError = { errorCarga = it },
                                ).also { webView = it }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (progreso < 100) {
                            LinearProgressIndicator(
                                progress = { progreso / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (errorCarga) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    stringResource(R.string.sesion_error_carga, plataforma.nombre),
                                    textAlign = TextAlign.Center,
                                )
                                TextButton(onClick = { webView?.reload() }) {
                                    Text(stringResource(R.string.reintentar))
                                }
                            }
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose { webView?.destroy() }
                }
            }
        }
    }

    // La web de login necesita JavaScript y almacenamiento local
    @SuppressLint("SetJavaScriptEnabled")
    private fun crearWebView(
        context: Context,
        alProgreso: (Int) -> Unit,
        alError: (Boolean) -> Unit,
    ): WebView = WebView(context).apply {
        // Sin esto el WebView usa WRAP_CONTENT dentro de Compose y las webs a pantalla
        // completa (100vh) se quedan con altura cero: pantalla gris o en blanco
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progreso: Int) = alProgreso(progreso)
        }
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = alError(false)

            // Solo cuenta el fallo de la página principal (sin red, DNS...), no de recursos sueltos
            override fun onReceivedError(view: WebView, peticion: WebResourceRequest, error: WebResourceError) {
                if (peticion.isForMainFrame) alError(true)
            }
        }
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
