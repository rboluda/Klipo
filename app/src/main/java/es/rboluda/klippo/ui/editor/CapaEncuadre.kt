package es.rboluda.klippo.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import es.rboluda.klippo.edicion.Encuadre

// Modos de arrastre: una esquina (0..3), mover el marco o nada
private const val MOVER = -1
private const val NADA = -2

/**
 * Marco de encuadre dibujado sobre el vídeo (debe ocupar exactamente el área del vídeo).
 * Fuera del marco se oscurece; si [editable], se mueve arrastrando el interior y se
 * redimensiona arrastrando las esquinas.
 */
@Composable
fun CapaEncuadre(
    encuadre: Encuadre,
    editable: Boolean,
    aspectoVideo: Float,
    onCambiar: (Encuadre) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actual by rememberUpdatedState(encuadre)
    val cambiar by rememberUpdatedState(onCambiar)
    val radioToque = with(LocalDensity.current) { 32.dp.toPx() }

    val gestos = if (editable) {
        Modifier.pointerInput(aspectoVideo) {
            var modo = NADA
            detectDragGestures(
                onDragStart = { punto ->
                    val e = actual
                    val esquina = esquinas(e, size.width.toFloat(), size.height.toFloat())
                        .indexOfFirst { (it - punto).getDistance() < radioToque }
                    val dentro = punto.x / size.width in e.x..(e.x + e.ancho) &&
                        punto.y / size.height in e.y..(e.y + e.alto)
                    modo = when {
                        esquina >= 0 -> esquina
                        dentro -> MOVER
                        else -> NADA
                    }
                },
                onDrag = { cambio, arrastre ->
                    if (modo != NADA) {
                        cambio.consume()
                        val dx = arrastre.x / size.width
                        val dy = arrastre.y / size.height
                        cambiar(
                            if (modo == MOVER) actual.mover(dx, dy)
                            else actual.redimensionar(modo, dx, dy, aspectoVideo)
                        )
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Canvas(modifier.fillMaxSize().then(gestos)) {
        val izq = encuadre.x * size.width
        val arriba = encuadre.y * size.height
        val der = (encuadre.x + encuadre.ancho) * size.width
        val abajo = (encuadre.y + encuadre.alto) * size.height
        val sombra = Color.Black.copy(alpha = 0.6f)

        // Lo que quedará fuera del vídeo exportado
        drawRect(sombra, Offset.Zero, Size(size.width, arriba))
        drawRect(sombra, Offset(0f, abajo), Size(size.width, size.height - abajo))
        drawRect(sombra, Offset(0f, arriba), Size(izq, abajo - arriba))
        drawRect(sombra, Offset(der, arriba), Size(size.width - der, abajo - arriba))

        drawRect(
            Color.White,
            Offset(izq, arriba),
            Size(der - izq, abajo - arriba),
            style = Stroke(width = 2.dp.toPx()),
        )
        if (editable) {
            for (punto in esquinas(encuadre, size.width, size.height)) {
                drawCircle(Color.White, radius = 8.dp.toPx(), center = punto)
            }
        }
    }
}

// Esquinas en píxeles, en el orden que usa Encuadre.redimensionar
private fun esquinas(e: Encuadre, ancho: Float, alto: Float): List<Offset> {
    val izq = e.x * ancho
    val der = (e.x + e.ancho) * ancho
    val arriba = e.y * alto
    val abajo = (e.y + e.alto) * alto
    return listOf(Offset(izq, arriba), Offset(der, arriba), Offset(izq, abajo), Offset(der, abajo))
}
