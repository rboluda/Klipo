package es.rboluda.klippo.ui.inicio

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import es.rboluda.klippo.R
import es.rboluda.klippo.almacenamiento.Compartir
import es.rboluda.klippo.almacenamiento.GuardarEnGaleria
import es.rboluda.klippo.descarga.Descarga
import kotlinx.coroutines.launch

/**
 * Accesos directos de una descarga completada, sin pasar por el editor: enviar el vídeo
 * original por WhatsApp, compartirlo con cualquier app, guardarlo en la galería o editarlo.
 */
@Composable
fun AccionesRapidas(descarga: Descarga, onEditar: () -> Unit, modifier: Modifier = Modifier) {
    val video = descarga.archivo ?: return
    val context = LocalContext.current
    val recursos = LocalResources.current
    val ambito = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    // Icono de la app de WhatsApp instalada (null si no está): no se incluye ningún logotipo
    val iconoWhatsApp: ImageBitmap? = remember {
        Compartir.whatsapp(context)?.let { paquete ->
            runCatching {
                context.packageManager.getApplicationIcon(paquete).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }

    // DIAGNÓSTICO TEMPORAL: informe de lo que pasa al pulsar WhatsApp, para copiarlo y enviarlo
    var informe by remember { mutableStateOf<String?>(null) }
    informe?.let { texto ->
        AlertDialog(
            onDismissRequest = { informe = null },
            title = { Text("Diagnóstico de WhatsApp") },
            text = {
                SelectionContainer {
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("Diagnóstico Klippo", texto))
                    informe = null
                }) { Text("Copiar") }
            },
            dismissButton = { TextButton(onClick = { informe = null }) { Text("Cerrar") } },
        )
    }

    Row(modifier) {
        if (iconoWhatsApp != null) {
            IconButton(onClick = { informe = Compartir.diagnosticoWhatsApp(context, video, descarga.titulo) }) {
                Image(
                    bitmap = iconoWhatsApp,
                    contentDescription = stringResource(R.string.accion_whatsapp),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        IconButton(onClick = { Compartir.video(context, video, descarga.titulo) }) {
            Icon(painterResource(R.drawable.ic_compartir), stringResource(R.string.compartir))
        }
        IconButton(
            onClick = {
                guardando = true
                ambito.launch {
                    val texto = try {
                        GuardarEnGaleria.guardar(context, video)
                        recursos.getString(R.string.galeria_guardado)
                    } catch (e: Exception) {
                        recursos.getString(R.string.galeria_error)
                    }
                    guardando = false
                    Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !guardando,
        ) {
            if (guardando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_guardar_galeria), stringResource(R.string.galeria_guardar))
            }
        }
        // Las herramientas del editor son para vídeo
        if (!descarga.esAudio) {
            IconButton(onClick = onEditar) {
                Icon(painterResource(R.drawable.ic_editar), stringResource(R.string.accion_editar))
            }
        }
    }
}
