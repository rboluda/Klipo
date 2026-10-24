package es.rboluda.klippo.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.rboluda.klippo.BuildConfig
import es.rboluda.klippo.R
import es.rboluda.klippo.ui.comun.LogoKlippo

private const val GITHUB_AUTOR = "https://github.com/rboluda"
private const val GITHUB_PROYECTO = "https://github.com/rboluda/Klippo"

// Componentes libres en los que se apoya Klippo, con su licencia
private data class Componente(val nombre: String, val licencia: String, val url: String)

private val COMPONENTES = listOf(
    Componente("yt-dlp", "Unlicense", "https://github.com/yt-dlp/yt-dlp"),
    Componente("FFmpeg", "GPL", "https://ffmpeg.org"),
    Componente("youtubedl-android", "GPL-3.0", "https://github.com/yausername/youtubedl-android"),
    Componente("Jetpack Compose · Media3", "Apache 2.0", "https://developer.android.com/jetpack"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcercaDeScreen(onVolver: () -> Unit) {
    val enlaces = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.acerca_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(painterResource(R.drawable.ic_atras), stringResource(R.string.volver))
                    }
                },
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LogoKlippo(tamano = 96.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.acerca_descripcion),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            // Autor y proyecto
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.acerca_autor),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { enlaces.openUri(GITHUB_AUTOR) }) {
                            Text(stringResource(R.string.acerca_mi_github))
                        }
                        OutlinedButton(onClick = { enlaces.openUri(GITHUB_PROYECTO) }) {
                            Text(stringResource(R.string.acerca_codigo))
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.acerca_principios),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Créditos del software libre
            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.acerca_software_libre),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                for (componente in COMPONENTES) {
                    TextButton(
                        onClick = { enlaces.openUri(componente.url) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(componente.nombre, modifier = Modifier.weight(1f))
                        Text(
                            componente.licencia,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
