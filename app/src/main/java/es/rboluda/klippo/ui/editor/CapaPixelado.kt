package es.rboluda.klippo.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import es.rboluda.klippo.edicion.ZonaPixelada

/**
 * Zonas a pixelar dibujadas sobre el vídeo (debe ocupar exactamente el área del vídeo).
 * La vista previa es una cuadrícula de bloques semitransparentes; el pixelado real se aplica
 * al exportar. Si [editable]: arrastrar mueve la zona, las esquinas cambian su tamaño y
 * mantener pulsado la borra.
 */
@Composable
fun CapaPixelado(
    zonas: List<ZonaPixelada>,
    editable: Boolean,
    aspectoVideo: Float,
    onCambiar: (ZonaPixelada) -> Unit,
    onBorrar: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actuales by rememberUpdatedState(zonas)
    val cambiar by rememberUpdatedState(onCambiar)
    val borrar by rememberUpdatedState(onBorrar)
    val radioToque = with(LocalDensity.current) { 28.dp.toPx() }

    val gestos = if (editable) {
        Modifier
            .pointerInput(aspectoVideo) {
                var tocada: Long? = null
                var esquina = -1
                detectDragGestures(
                    onDragStart = { punto ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        tocada = null
                        // De arriba abajo: la última zona añadida se dibuja encima
                        for (z in actuales.asReversed()) {
                            val i = esquinas(z, w, h).indexOfFirst { (it - punto).getDistance() < radioToque }
                            if (i >= 0 || contiene(z, punto, w, h)) {
                                tocada = z.id
                                esquina = i
                                break
                            }
                        }
                    },
                    onDrag = { cambio, arrastre ->
                        val zona = actuales.find { it.id == tocada } ?: return@detectDragGestures
                        cambio.consume()
                        val dx = arrastre.x / size.width
                        val dy = arrastre.y / size.height
                        cambiar(
                            if (esquina >= 0) zona.redimensionar(esquina, dx, dy, aspectoVideo)
                            else zona.mover(dx, dy)
                        )
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { punto ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    actuales.lastOrNull { contiene(it, punto, w, h) }?.let { borrar(it.id) }
                })
            }
    } else {
        Modifier
    }

    Canvas(modifier.fillMaxSize().then(gestos)) {
        for (z in zonas) {
            val origen = Offset(z.zona.x * size.width, z.zona.y * size.height)
            val tamano = Size(z.zona.ancho * size.width, z.zona.alto * size.height)
            dibujarBloques(origen, tamano)
            drawRect(Color.White, origen, tamano, style = Stroke(width = 1.5.dp.toPx()))
            if (editable) {
                for (punto in esquinas(z, size.width, size.height)) {
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = punto)
                }
            }
        }
    }
}

// Vista previa del pixelado: cuadros grises alternos, como un mosaico
private fun DrawScope.dibujarBloques(origen: Offset, tamano: Size) {
    val lado = 12.dp.toPx()
    clipRect(origen.x, origen.y, origen.x + tamano.width, origen.y + tamano.height) {
        drawRect(Color.Black.copy(alpha = 0.35f), origen, tamano)
        var fila = 0
        var y = origen.y
        while (y < origen.y + tamano.height) {
            var x = origen.x + if (fila % 2 == 0) 0f else lado
            while (x < origen.x + tamano.width) {
                drawRect(Color.White.copy(alpha = 0.25f), Offset(x, y), Size(lado, lado))
                x += lado * 2
            }
            y += lado
            fila++
        }
    }
}

private fun contiene(z: ZonaPixelada, punto: Offset, ancho: Float, alto: Float): Boolean =
    punto.x / ancho in z.zona.x..(z.zona.x + z.zona.ancho) &&
        punto.y / alto in z.zona.y..(z.zona.y + z.zona.alto)

// Esquinas en píxeles, en el orden que usa Encuadre.redimensionar
private fun esquinas(z: ZonaPixelada, ancho: Float, alto: Float): List<Offset> {
    val izq = z.zona.x * ancho
    val der = (z.zona.x + z.zona.ancho) * ancho
    val arriba = z.zona.y * alto
    val abajo = (z.zona.y + z.zona.alto) * alto
    return listOf(Offset(izq, arriba), Offset(der, arriba), Offset(izq, abajo), Offset(der, abajo))
}
