package es.rboluda.klippo.ui.comun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.rboluda.klippo.R
import es.rboluda.klippo.ajustes.Calidad

// Diálogo que pregunta la calidad de cada vídeo; un toque en una opción lanza la descarga.
// La última usada aparece marcada para elegirla rápido. Se usa en Inicio y al compartir.
@Composable
fun DialogoCalidad(
    preseleccionada: Calidad,
    onElegir: (Calidad) -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.calidad_titulo)) },
        text = {
            Column {
                for (opcion in Calidad.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onElegir(opcion) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = opcion == preseleccionada, onClick = { onElegir(opcion) })
                        Text(stringResource(opcion.etiqueta))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        },
    )
}
