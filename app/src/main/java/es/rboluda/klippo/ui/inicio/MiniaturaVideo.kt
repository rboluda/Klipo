package es.rboluda.klippo.ui.inicio

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Miniatura de una descarga (o un hueco gris mientras no existe)
@Composable
fun MiniaturaVideo(archivo: File?, modifier: Modifier = Modifier) {
    val imagen by produceState<ImageBitmap?>(null, archivo) {
        value = archivo?.let {
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        imagen?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
