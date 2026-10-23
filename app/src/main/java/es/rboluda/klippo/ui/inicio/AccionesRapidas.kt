package es.rboluda.klippo.ui.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import es.rboluda.klippo.R
import es.rboluda.klippo.almacenamiento.Compartir
import es.rboluda.klippo.descarga.Descarga

/**
 * Accesos directos de una descarga completada, sin pasar por el editor:
 * enviar el vídeo original por WhatsApp, compartirlo con cualquier app o abrir el editor.
 */
@Composable
fun AccionesRapidas(descarga: Descarga, onEditar: () -> Unit, modifier: Modifier = Modifier) {
    val video = descarga.archivo ?: return
    val context = LocalContext.current
    // Icono de la app de WhatsApp instalada (null si no está): no se incluye ningún logotipo
    val iconoWhatsApp: ImageBitmap? = remember {
        Compartir.whatsapp(context)?.let { paquete ->
            runCatching {
                context.packageManager.getApplicationIcon(paquete).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }

    Row(modifier) {
        if (iconoWhatsApp != null) {
            IconButton(onClick = { Compartir.aWhatsApp(context, video, descarga.titulo) }) {
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
        IconButton(onClick = onEditar) {
            Icon(painterResource(R.drawable.ic_editar), stringResource(R.string.accion_editar))
        }
    }
}
