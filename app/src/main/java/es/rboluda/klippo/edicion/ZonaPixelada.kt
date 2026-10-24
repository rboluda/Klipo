package es.rboluda.klippo.edicion

import java.io.Serializable
import kotlin.math.roundToInt

/**
 * Zona fija del vídeo que se pixela durante todo el vídeo (caras, matrículas…).
 * Reutiliza la geometría del encuadre libre: coordenadas relativas (0..1) al fotograma
 * completo ya girado.
 */
data class ZonaPixelada(val id: Long, val zona: Encuadre) : Serializable {

    fun mover(dx: Float, dy: Float) = copy(zona = zona.mover(dx, dy))

    fun redimensionar(esquina: Int, dx: Float, dy: Float, aspectoVideo: Float) =
        copy(zona = zona.redimensionar(esquina, dx, dy, aspectoVideo, minimo = MINIMO))

    companion object {
        // Más pequeña que el encuadre: una matrícula puede ocupar poco del vídeo
        const val MINIMO = 0.04f

        // Zona nueva: un cuadrado centrado de un tercio del lado corto
        fun nueva(aspectoVideo: Float): ZonaPixelada {
            val alto = if (aspectoVideo >= 1f) 0.33f else 0.33f * aspectoVideo
            val ancho = alto / aspectoVideo
            return ZonaPixelada(
                id = System.nanoTime(),
                zona = Encuadre(Formato.LIBRE, (1f - ancho) / 2f, (1f - alto) / 2f, ancho, alto),
            )
        }
    }
}

// Zona ya pasada a píxeles pares del vídeo, lista para el filtro de FFmpeg
data class ZonaEnPixeles(val x: Int, val y: Int, val ancho: Int, val alto: Int, val bloque: Int) {
    companion object {
        fun de(zona: Encuadre, medidas: MedidasVideo): ZonaEnPixeles {
            fun par(valor: Float) = (valor / 2f).roundToInt() * 2
            fun parAbajo(valor: Int) = valor / 2 * 2
            val x = par(zona.x * medidas.ancho).coerceIn(0, parAbajo(medidas.ancho - 2))
            val y = par(zona.y * medidas.alto).coerceIn(0, parAbajo(medidas.alto - 2))
            // Por el redondeo, la zona no puede salirse del fotograma (crop fallaría)
            val ancho = par(zona.ancho * medidas.ancho).coerceIn(2, parAbajo(medidas.ancho - x))
            val alto = par(zona.alto * medidas.alto).coerceIn(2, parAbajo(medidas.alto - y))
            // Unos 10 bloques en el lado corto de la zona: se ve pixelado sin dejar adivinar
            val bloque = (minOf(ancho, alto) / 10).coerceAtLeast(4)
            return ZonaEnPixeles(x, y, ancho, alto, bloque)
        }
    }
}
