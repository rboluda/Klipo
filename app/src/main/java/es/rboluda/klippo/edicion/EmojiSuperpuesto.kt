package es.rboluda.klippo.edicion

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.Serializable
import kotlin.math.roundToInt

/**
 * Emoji colocado sobre el vídeo. [x] e [y] son el centro y [tamano] la altura del emoji,
 * todo relativo (0..1) al fotograma completo ya girado. Se muestra durante todo el vídeo.
 */
data class EmojiSuperpuesto(
    val id: Long,
    val texto: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val tamano: Float = TAMANO_INICIAL,
) : Serializable {

    fun mover(dx: Float, dy: Float): EmojiSuperpuesto =
        copy(x = (x + dx).coerceIn(0f, 1f), y = (y + dy).coerceIn(0f, 1f))

    fun escalar(factor: Float): EmojiSuperpuesto =
        copy(tamano = (tamano * factor).coerceIn(TAMANO_MINIMO, TAMANO_MAXIMO))

    companion object {
        const val TAMANO_INICIAL = 0.15f
        private const val TAMANO_MINIMO = 0.04f
        private const val TAMANO_MAXIMO = 0.8f
    }
}

// Un emoji ya dibujado en PNG y su posición en píxeles (esquina superior izquierda)
data class EmojiRenderizado(val png: File, val x: Int, val y: Int)

object RenderEmoji {

    /**
     * Dibuja el emoji con la fuente de emojis del móvil, al mismo tamaño relativo que en la
     * vista previa (tamaño de letra = tamano × alto del vídeo), recortado a su silueta.
     */
    fun renderizar(emoji: EmojiSuperpuesto, medidas: MedidasVideo, destino: File): EmojiRenderizado {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = emoji.tamano * medidas.alto }
        val limites = Rect()
        pincel.getTextBounds(emoji.texto, 0, emoji.texto.length, limites)
        val ancho = limites.width().coerceAtLeast(1)
        val alto = limites.height().coerceAtLeast(1)
        val imagen = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(imagen).drawText(emoji.texto, -limites.left.toFloat(), -limites.top.toFloat(), pincel)
        destino.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        imagen.recycle()
        return EmojiRenderizado(
            png = destino,
            x = (emoji.x * medidas.ancho - ancho / 2f).roundToInt(),
            y = (emoji.y * medidas.alto - alto / 2f).roundToInt(),
        )
    }
}
