package es.rboluda.klippo.edicion

import java.io.Serializable

// Formatos de encuadre; la proporción es ancho/alto en píxeles (null = libre)
enum class Formato(val etiqueta: String, val proporcion: Float?) {
    VERTICAL_9_16("9:16", 9f / 16f),
    CUADRADO("1:1", 1f),
    VERTICAL_4_5("4:5", 4f / 5f),
    LIBRE("Libre", null),
}

/**
 * Zona del vídeo que se conserva. Coordenadas relativas (0..1) al fotograma ya girado,
 * para que no dependan del tamaño de la pantalla. [aspectoVideo] = ancho/alto en píxeles.
 */
data class Encuadre(
    val formato: Formato,
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,
) : Serializable {

    fun mover(dx: Float, dy: Float): Encuadre =
        copy(x = (x + dx).coerceIn(0f, 1f - ancho), y = (y + dy).coerceIn(0f, 1f - alto))

    /**
     * Arrastra una esquina (0 arriba-izq, 1 arriba-der, 2 abajo-izq, 3 abajo-der) dejando fija
     * la opuesta. En los formatos fijos se mantiene la proporción.
     */
    fun redimensionar(
        esquina: Int,
        dx: Float,
        dy: Float,
        aspectoVideo: Float,
        minimo: Float = MINIMO,
    ): Encuadre {
        val izquierda = esquina % 2 == 0
        val arriba = esquina < 2
        val anclaX = if (izquierda) x + ancho else x
        val anclaY = if (arriba) y + alto else y
        val maxAncho = if (izquierda) anclaX else 1f - anclaX
        val maxAlto = if (arriba) anclaY else 1f - anclaY
        var nuevoAncho = if (izquierda) ancho - dx else ancho + dx
        var nuevoAlto = if (arriba) alto - dy else alto + dy

        val proporcion = formato.proporcion
        if (proporcion == null) {
            nuevoAncho = nuevoAncho.coerceIn(minimo, maxAncho)
            nuevoAlto = nuevoAlto.coerceIn(minimo, maxAlto)
        } else {
            // Proporción en coordenadas relativas: (ancho/alto en píxeles) / aspecto del vídeo
            val relativa = proporcion / aspectoVideo
            nuevoAncho = nuevoAncho.coerceIn(minimo, maxAncho)
            nuevoAlto = nuevoAncho / relativa
            if (nuevoAlto > maxAlto) {
                nuevoAlto = maxAlto
                nuevoAncho = nuevoAlto * relativa
            }
        }
        return copy(
            x = if (izquierda) anclaX - nuevoAncho else anclaX,
            y = if (arriba) anclaY - nuevoAlto else anclaY,
            ancho = nuevoAncho,
            alto = nuevoAlto,
        )
    }

    companion object {
        // Lado mínimo del encuadre (10 % del vídeo)
        const val MINIMO = 0.1f

        // El mayor encuadre del formato que cabe en el vídeo, centrado
        fun centrado(formato: Formato, aspectoVideo: Float): Encuadre {
            val proporcion = formato.proporcion
                ?: return Encuadre(formato, 0.1f, 0.1f, 0.8f, 0.8f)
            val relativa = proporcion / aspectoVideo
            var ancho = 1f
            var alto = ancho / relativa
            if (alto > 1f) {
                alto = 1f
                ancho = alto * relativa
            }
            return Encuadre(formato, (1f - ancho) / 2f, (1f - alto) / 2f, ancho, alto)
        }
    }
}
