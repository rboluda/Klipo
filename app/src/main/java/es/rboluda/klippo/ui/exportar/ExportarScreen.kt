package es.rboluda.klippo.ui.exportar

import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.rboluda.klippo.R
import es.rboluda.klippo.almacenamiento.Compartir
import es.rboluda.klippo.edicion.ParametrosEdicion

// Pantalla Exportar: vista previa del vídeo, compartir y guardar en la galería
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarScreen(
    parametros: ParametrosEdicion,
    onVolver: () -> Unit,
    viewModel: ExportarViewModel = viewModel(factory = ExportarViewModel.factory(parametros)),
) {
    val descarga = viewModel.descarga
    val estado by viewModel.estadoExportacion.collectAsStateWithLifecycle()
    val info by viewModel.info.collectAsStateWithLifecycle()
    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    // En Android 14+ el guardado va dentro del share sheet (ChooserAction)
    val guardarEnShareSheet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    LaunchedEffect(Unit) {
        viewModel.avisos.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exportar_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(painterResource(R.drawable.ic_atras), stringResource(R.string.volver))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { relleno ->
        val actual = descarga
        if (actual == null || estado == EstadoExportacion.NoDisponible) {
            // La caché se limpia cada hora: el vídeo puede haber desaparecido
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.exportar_no_disponible))
            }
            return@Scaffold
        }
        val archivo = (estado as? EstadoExportacion.Lista)?.archivo

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Miniatura(info)
            Text(
                text = actual.titulo ?: actual.url,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            info?.let {
                Text(
                    text = textoInfo(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (val e = estado) {
                is EstadoExportacion.Generando -> {
                    Text(
                        text = stringResource(R.string.exportar_generando, (e.progreso * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { e.progreso }, modifier = Modifier.fillMaxWidth())
                }
                is EstadoExportacion.Error -> {
                    Text(
                        text = stringResource(R.string.exportar_error, e.mensaje),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = viewModel::generar, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.reintentar))
                    }
                }
                else -> Unit
            }
            Button(
                onClick = { archivo?.let { Compartir.video(context, it, actual.titulo) } },
                enabled = archivo != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.compartir))
            }
            if (guardarEnShareSheet) {
                Text(
                    text = stringResource(R.string.exportar_pista_galeria),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(
                    onClick = viewModel::guardarEnGaleria,
                    enabled = archivo != null && !guardando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.galeria_guardar))
                }
            }
        }
    }
}

@Composable
private fun Miniatura(info: InfoVideo?) {
    // Proporción real del vídeo (vertical u horizontal); 16:9 mientras se calcula
    val proporcion = info?.takeIf { it.ancho > 0 && it.alto > 0 }
        ?.let { it.ancho.toFloat() / it.alto } ?: (16f / 9f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .aspectRatio(proporcion, matchHeightConstraintsFirst = proporcion < 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val imagen = info?.miniatura
        if (imagen != null) {
            Image(
                bitmap = imagen.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (info == null) {
            CircularProgressIndicator()
        }
    }
}

// "0:42 · 1080×1920 · 12,3 MB"
@Composable
private fun textoInfo(info: InfoVideo): String {
    val context = LocalContext.current
    val segundos = info.duracionMs / 1000
    val duracion = "%d:%02d".format(segundos / 60, segundos % 60)
    val tamano = Formatter.formatShortFileSize(context, info.bytes)
    return listOfNotNull(
        duracion,
        "${info.ancho}×${info.alto}".takeIf { info.ancho > 0 },
        tamano,
    ).joinToString(" · ")
}
