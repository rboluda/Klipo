package es.rboluda.klippo.edicion

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.Serializable
import kotlin.math.ceil
import kotlin.math.roundToInt

// Estilos del texto superpuesto (los emojis no llevan estilo)
enum class EstiloTexto {
    // Letras blancas con borde negro, legibles sobre cualquier fondo
    CONTORNO,

    // Letras blancas sobre una franja oscura redondeada
    FONDO;

    companion object {
        // Proporciones respecto al tamaño de letra; las comparten la vista previa y el PNG
        const val GROSOR_CONTORNO = 0.14f
        const val MARGEN_FONDO = 0.3f
        const val RADIO_FONDO = 0.25f
        const val OPACIDAD_FONDO = 0.6f
    }
}

/**
 * Emoji o texto colocado sobre el vídeo. [x] e [y] son el centro y [tamano] el tamaño de
 * letra, todo relativo (0..1) al fotograma completo ya girado. [estilo] null = emoji.
 * Se muestra durante todo el vídeo.
 */
data class EmojiSuperpuesto(
    val id: Long,
    val texto: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val tamano: Float = TAMANO_INICIAL,
    val estilo: EstiloTexto? = null,
) : Serializable {

    val esTexto: Boolean get() = estilo != null

    fun mover(dx: Float, dy: Float): EmojiSuperpuesto =
        copy(x = (x + dx).coerceIn(0f, 1f), y = (y + dy).coerceIn(0f, 1f))

    fun escalar(factor: Float): EmojiSuperpuesto =
        copy(tamano = (tamano * factor).coerceIn(TAMANO_MINIMO, TAMANO_MAXIMO))

    companion object {
        const val TAMANO_INICIAL = 0.15f
        // El texto empieza más pequeño que un emoji: suele ser más largo
        const val TAMANO_INICIAL_TEXTO = 0.07f
        private const val TAMANO_MINIMO = 0.03f
        private const val TAMANO_MAXIMO = 0.8f
    }
}

// Un emoji o texto ya dibujado en PNG y su posición en píxeles (esquina superior izquierda)
data class EmojiRenderizado(val png: File, val x: Int, val y: Int)

object RenderEmoji {

    /**
     * Dibuja el emoji o el texto con las fuentes del móvil, al mismo tamaño relativo que en la
     * vista previa (tamaño de letra = tamano × alto del vídeo).
     */
    fun renderizar(elemento: EmojiSuperpuesto, medidas: MedidasVideo, destino: File): EmojiRenderizado {
        val imagen = elemento.estilo?.let { dibujarTexto(elemento.texto, elemento.tamano * medidas.alto, it) }
            ?: dibujarEmoji(elemento.texto, elemento.tamano * medidas.alto)
        destino.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val ancho = imagen.width
        val alto = imagen.height
        imagen.recycle()
        return EmojiRenderizado(
            png = destino,
            x = (elemento.x * medidas.ancho - ancho / 2f).roundToInt(),
            y = (elemento.y * medidas.alto - alto / 2f).roundToInt(),
        )
    }

    // Emoji recortado a su silueta
    private fun dibujarEmoji(texto: String, tamanoLetra: Float): Bitmap {
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = tamanoLetra }
        val limites = Rect()
        pincel.getTextBounds(texto, 0, texto.length, limites)
        val imagen = Bitmap.createBitmap(
            limites.width().coerceAtLeast(1), limites.height().coerceAtLeast(1), Bitmap.Config.ARGB_8888,
        )
        Canvas(imagen).drawText(texto, -limites.left.toFloat(), -limites.top.toFloat(), pincel)
        return imagen
    }

    // Texto en negrita con contorno o con franja de fondo
    private fun dibujarTexto(texto: String, tamanoLetra: Float, estilo: EstiloTexto): Bitmap {
        val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamanoLetra
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
        }
        val metricas = relleno.fontMetrics
        val anchoTexto = relleno.measureText(texto)
        val altoTexto = metricas.descent - metricas.ascent
        val margen = when (estilo) {
            EstiloTexto.CONTORNO -> tamanoLetra * EstiloTexto.GROSOR_CONTORNO
            EstiloTexto.FONDO -> tamanoLetra * EstiloTexto.MARGEN_FONDO
        }
        val ancho = ceil(anchoTexto + margen * 2).toInt().coerceAtLeast(1)
        val alto = ceil(altoTexto + margen * 2).toInt().coerceAtLeast(1)
        val imagen = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(imagen)
        val baseX = margen
        val baseY = margen - metricas.ascent
        when (estilo) {
            EstiloTexto.CONTORNO -> {
                val borde = Paint(relleno).apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = tamanoLetra * EstiloTexto.GROSOR_CONTORNO
                    strokeJoin = Paint.Join.ROUND
                }
                lienzo.drawText(texto, baseX, baseY, borde)
            }
            EstiloTexto.FONDO -> {
                val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    alpha = (255 * EstiloTexto.OPACIDAD_FONDO).roundToInt()
                }
                val radio = tamanoLetra * EstiloTexto.RADIO_FONDO
                lienzo.drawRoundRect(RectF(0f, 0f, ancho.toFloat(), alto.toFloat()), radio, radio, fondo)
            }
        }
        lienzo.drawText(texto, baseX, baseY, relleno)
        return imagen
    }
}
