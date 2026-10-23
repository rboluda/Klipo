package es.rboluda.klippo.ui.editor

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import es.rboluda.klippo.edicion.Formato
import es.rboluda.klippo.edicion.Recorte
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

// Herramientas de la barra inferior. La Fase 3 añadirá emojis y pixelado.
private enum class Herramienta(@param:StringRes val nombre: Int, @param:DrawableRes val icono: Int) {
    RECORTAR(R.string.editor_recortar, R.drawable.ic_recortar),
    ENCUADRE(R.string.editor_encuadre, R.drawable.ic_encuadre),
}

// Formatos de encuadre como chips; "Original" quita el encuadre
@Composable
private fun PanelEncuadre(seleccionado: Formato?, onElegir: (Formato?) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = seleccionado == null,
                onClick = { onElegir(null) },
                label = { Text(stringResource(R.string.encuadre_original)) },
            )
            for (formato in Formato.entries) {
                FilterChip(
                    selected = seleccionado == formato,
                    onClick = { onElegir(formato) },
                    label = {
                        Text(
                            if (formato == Formato.LIBRE) stringResource(R.string.encuadre_libre)
                            else formato.etiqueta
                        )
                    },
                )
            }
        }
    }
}

// Editor: vídeo arriba, panel de la herramienta activa y barra de herramientas abajo.
// "Siguiente" lleva a Exportar con los parámetros elegidos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onVolver: () -> Unit,
    onSiguiente: (id: String) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    val descarga by viewModel.descarga.collectAsStateWithLifecycle()
    val medidas by viewModel.medidas.collectAsStateWithLifecycle()
    val duracionMs = medidas?.duracionMs ?: 0L
    val aspecto = medidas?.aspecto ?: 0f
    val recorte by viewModel.recorte.collectAsStateWithLifecycle()
    val encuadre by viewModel.encuadre.collectAsStateWithLifecycle()
    var herramienta by rememberSaveable { mutableStateOf<Herramienta?>(null) }
    val archivo = descarga?.archivo?.takeIf { it.exists() }
    val reproductor = archivo?.let { rememberReproductor(it) }

    // Vista previa del recorte: la reproducción se queda dentro del tramo elegido, en bucle
    LaunchedEffect(reproductor, recorte) {
        val tramo = recorte ?: return@LaunchedEffect
        val player = reproductor ?: return@LaunchedEffect
        while (true) {
            // En pausa no se toca: al arrastrar el final se muestra ese fotograma
            val posicion = player.currentPosition
            if (player.isPlaying && (posicion >= tramo.finMs || posicion < tramo.inicioMs - MARGEN_MS)) {
                player.seekTo(tramo.inicioMs)
            }
            delay(50)
        }
    }

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
                        onClick = {
                            reproductor?.pause()
                            descarga?.let { onSiguiente(it.id) }
                        },
                        enabled = archivo != null,
                    ) {
                        Text(stringResource(R.string.siguiente))
                    }
                },
            )
        },
        bottomBar = {
            Column {
                val tramo = recorte
                if (herramienta == Herramienta.RECORTAR && tramo != null && duracionMs > 0) {
                    PanelRecorte(
                        recorte = tramo,
                        duracionMs = duracionMs,
                        onCambiar = { inicio, fin, movido ->
                            viewModel.cambiarRecorte(inicio, fin)
                            // Mientras se arrastra se ve el fotograma del extremo que se mueve
                            reproductor?.pause()
                            reproductor?.seekTo(movido)
                        },
                        onSoltar = { reproductor?.seekTo(tramo.inicioMs) },
                    )
                }
                if (herramienta == Herramienta.ENCUADRE) {
                    PanelEncuadre(
                        seleccionado = encuadre?.formato,
                        onElegir = viewModel::elegirFormato,
                    )
                }
                NavigationBar {
                    for (opcion in Herramienta.entries) {
                        NavigationBarItem(
                            selected = herramienta == opcion,
                            onClick = { herramienta = if (herramienta == opcion) null else opcion },
                            // El encuadre necesita conocer las medidas del vídeo
                            enabled = archivo != null && (opcion != Herramienta.ENCUADRE || aspecto > 0f),
                            icon = { Icon(painterResource(opcion.icono), null) },
                            label = { Text(stringResource(opcion.nombre)) },
                        )
                    }
                }
            }
        },
    ) { relleno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (reproductor != null) {
                // La caja tiene la proporción del vídeo: así la capa de encuadre coincide
                // exactamente con la imagen (sin las bandas negras del reproductor)
                Box(if (aspecto > 0f) Modifier.aspectRatio(aspecto) else Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { PlayerView(it).apply { player = reproductor } },
                        modifier = Modifier.fillMaxSize(),
                    )
                    encuadre?.let {
                        CapaEncuadre(
                            encuadre = it,
                            editable = herramienta == Herramienta.ENCUADRE,
                            aspectoVideo = aspecto,
                            onCambiar = viewModel::cambiarEncuadre,
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.exportar_no_disponible), color = Color.White)
            }
        }
    }
}

// Margen para no saltar al inicio por redondeos al buscar justo en el borde del tramo
private const val MARGEN_MS = 250L

// Doble barra para elegir inicio y fin, con los tiempos y la duración resultante
@Composable
private fun PanelRecorte(
    recorte: Recorte,
    duracionMs: Long,
    onCambiar: (inicioMs: Long, finMs: Long, movidoMs: Long) -> Unit,
    onSoltar: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(
                    R.string.recorte_resumen,
                    formatearTiempo(recorte.inicioMs),
                    formatearTiempo(recorte.finMs),
                    formatearTiempo(recorte.duracionMs),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            RangeSlider(
                value = recorte.inicioMs.toFloat()..recorte.finMs.toFloat(),
                onValueChange = { rango ->
                    val inicio = rango.start.toLong()
                    val fin = rango.endInclusive.toLong()
                    val movido = if (inicio != recorte.inicioMs) inicio else fin
                    onCambiar(inicio, fin, movido)
                },
                onValueChangeFinished = onSoltar,
                valueRange = 0f..duracionMs.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ExoPlayer ligado al ciclo de vida de la pantalla: se pausa al salir y se libera al cerrar
@Composable
private fun rememberReproductor(archivo: File): ExoPlayer {
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
    return reproductor
}

// "1:05.3" (minutos, segundos y décimas)
private fun formatearTiempo(ms: Long): String {
    val decimas = ms / 100
    return String.format(Locale.ROOT, "%d:%02d.%d", decimas / 600, (decimas / 10) % 60, decimas % 10)
}
