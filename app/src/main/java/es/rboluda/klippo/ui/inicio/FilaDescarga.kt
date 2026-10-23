package es.rboluda.klippo.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.rboluda.klippo.R
import es.rboluda.klippo.descarga.Descarga
import es.rboluda.klippo.descarga.EstadoDescarga

// Tarjeta de una descarga: título (o URL), estado y barra de progreso
@Composable
fun FilaDescarga(descarga: Descarga, onCancelar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = descarga.titulo ?: descarga.url,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (descarga.estado == EstadoDescarga.Descargando) {
                if (descarga.progreso < 0f) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { descarga.progreso },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = textoEstado(descarga),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (descarga.estado is EstadoDescarga.Fallida) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                )
                if (descarga.estado == EstadoDescarga.Descargando) {
                    TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
                }
            }
        }
    }
}

@Composable
private fun textoEstado(descarga: Descarga): String = when (val estado = descarga.estado) {
    EstadoDescarga.Descargando -> {
        val porcentaje = (descarga.progreso * 100).toInt()
        when {
            descarga.progreso < 0f -> stringResource(R.string.descarga_analizando)
            descarga.etaSegundos > 0 -> stringResource(
                R.string.descarga_progreso_eta,
                porcentaje,
                formatearDuracion(descarga.etaSegundos),
            )
            else -> stringResource(R.string.descarga_progreso, porcentaje)
        }
    }
    EstadoDescarga.Completada -> stringResource(R.string.descarga_completada)
    EstadoDescarga.Cancelada -> stringResource(R.string.descarga_cancelada)
    is EstadoDescarga.Fallida -> stringResource(R.string.descarga_fallida, estado.mensaje)
}

private fun formatearDuracion(segundos: Long): String =
    "%d:%02d".format(segundos / 60, segundos % 60)
