package es.rboluda.klippo.ui.editor

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import es.rboluda.klippo.R
import java.io.File

// Herramientas de la barra inferior. Se irán activando en cada paso de las fases 2 y 3.
private enum class Herramienta(@param:StringRes val nombre: Int, @param:DrawableRes val icono: Int) {
    RECORTAR(R.string.editor_recortar, R.drawable.ic_recortar),
}

// Editor: vídeo arriba, herramientas abajo; "Siguiente" lleva a Exportar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onVolver: () -> Unit,
    onSiguiente: (id: String) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    val descarga by viewModel.descarga.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(painterResource(R.drawable.ic_atras), stringResource(R.string.volver))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { descarga?.let { onSiguiente(it.id) } },
                        enabled = descarga?.archivo?.exists() == true,
                    ) {
                        Text(stringResource(R.string.siguiente))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                for (herramienta in Herramienta.entries) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        // El recorte se activa en el paso 2.2
                        enabled = false,
                        icon = { Icon(painterResource(herramienta.icono), null) },
                        label = { Text(stringResource(herramienta.nombre)) },
                    )
                }
            }
        },
    ) { relleno ->
        val archivo = descarga?.archivo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (archivo != null && archivo.exists()) {
                Reproductor(archivo)
            } else {
                Text(stringResource(R.string.exportar_no_disponible), color = Color.White)
            }
        }
    }
}

// ExoPlayer con los controles estándar de Media3; se pausa al salir de la app
@Composable
private fun Reproductor(archivo: File) {
    val context = LocalContext.current
    val reproductor = remember(archivo) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(archivo.toURI().toString()))
            prepare()
        }
    }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(reproductor, ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) reproductor.pause()
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            reproductor.release()
        }
    }
    Column(Modifier.fillMaxWidth()) {
        AndroidView(
            factory = { PlayerView(it).apply { player = reproductor } },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
