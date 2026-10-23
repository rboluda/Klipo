package es.rboluda.klippo.ui.editor

import android.icu.text.BreakIterator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.rboluda.klippo.R
import es.rboluda.klippo.edicion.EmojiSuperpuesto
import kotlin.math.roundToInt

/**
 * Emojis sobre el vídeo (debe ocupar exactamente el área del vídeo). El tamaño de letra es
 * tamano × alto de la capa, igual que al exportar. Si [editable]: arrastrar mueve el emoji
 * tocado, pellizcar cambia su tamaño y mantener pulsado lo borra.
 */
@Composable
fun CapaEmojis(
    emojis: List<EmojiSuperpuesto>,
    editable: Boolean,
    onMover: (id: Long, dx: Float, dy: Float) -> Unit,
    onEscalar: (id: Long, factor: Float) -> Unit,
    onBorrar: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actuales by rememberUpdatedState(emojis)
    val mover by rememberUpdatedState(onMover)
    val escalar by rememberUpdatedState(onEscalar)
    val borrar by rememberUpdatedState(onBorrar)
    val densidad = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val ancho = constraints.maxWidth.toFloat()
        val alto = constraints.maxHeight.toFloat()

        val gestos = if (editable) {
            Modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val inicio = awaitFirstDown(requireUnconsumed = false)
                        val tocado = emojiEn(actuales, inicio.position, ancho, alto) ?: return@awaitEachGesture
                        // Mientras haya dedos: un dedo mueve, dos dedos (pellizco) escalan
                        while (true) {
                            val evento = awaitPointerEvent()
                            if (evento.changes.none { it.pressed }) break
                            val desplazamiento = evento.calculatePan()
                            val zoom = evento.calculateZoom()
                            if (desplazamiento != Offset.Zero) {
                                mover(tocado.id, desplazamiento.x / ancho, desplazamiento.y / alto)
                            }
                            if (zoom != 1f) escalar(tocado.id, zoom)
                            if (desplazamiento != Offset.Zero || zoom != 1f) {
                                evento.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { punto ->
                        emojiEn(actuales, punto, ancho, alto)?.let { borrar(it.id) }
                    })
                }
        } else {
            Modifier
        }

        Box(Modifier.fillMaxSize().then(gestos)) {
            for (emoji in emojis) {
                Text(
                    text = emoji.texto,
                    // toSp con la densidad: el tamaño en píxeles es exactamente tamano × alto
                    fontSize = with(densidad) { (emoji.tamano * alto).toSp() },
                    lineHeight = with(densidad) { (emoji.tamano * alto).toSp() },
                    modifier = Modifier.layout { medible, _ ->
                        val colocable = medible.measure(Constraints())
                        layout(colocable.width, colocable.height) {
                            colocable.place(
                                (emoji.x * ancho - colocable.width / 2f).roundToInt(),
                                (emoji.y * alto - colocable.height / 2f).roundToInt(),
                            )
                        }
                    },
                )
            }
        }
    }
}

// El emoji bajo el dedo (el de más arriba si se solapan)
private fun emojiEn(emojis: List<EmojiSuperpuesto>, punto: Offset, ancho: Float, alto: Float): EmojiSuperpuesto? =
    emojis.lastOrNull { e ->
        val radio = e.tamano * alto * 0.7f
        (Offset(e.x * ancho, e.y * alto) - punto).getDistance() < radio
    }

// Los más usados a mano; cualquier otro, con el teclado de emojis del móvil
private val FRECUENTES = listOf(
    "😂", "😍", "🔥", "❤️", "👍", "🙌", "😎", "🎉", "💯", "😱", "🤣", "👀", "✨", "🙈", "💀", "🥳",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoEmoji(onElegir: (String) -> Unit, onCancelar: () -> Unit) {
    var otro by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.emoji_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (emoji in FRECUENTES) {
                        Text(
                            text = emoji,
                            fontSize = 30.sp,
                            modifier = Modifier
                                .clickable { onElegir(emoji) }
                                .padding(6.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = otro,
                    onValueChange = { otro = it },
                    label = { Text(stringResource(R.string.emoji_otro)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { primerEmoji(otro)?.let(onElegir) },
                enabled = otro.isNotBlank(),
            ) {
                Text(stringResource(R.string.emoji_anadir))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        },
    )
}

// Primer carácter visible del texto (un emoji puede ocupar varios caracteres: 👨‍👩‍👧, 👍🏽…)
private fun primerEmoji(texto: String): String? {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return null
    val iterador = BreakIterator.getCharacterInstance().apply { setText(limpio) }
    val fin = iterador.next()
    return if (fin == BreakIterator.DONE) limpio else limpio.substring(0, fin)
}
