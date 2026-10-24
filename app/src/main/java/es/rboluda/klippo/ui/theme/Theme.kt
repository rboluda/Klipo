package es.rboluda.klippo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.rboluda.klippo.KlippoApp
import es.rboluda.klippo.ajustes.Tema

// Esquinas generosas: tarjetas y botones redondeados, coherentes con el icono
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Fuente del sistema; solo se refuerzan los títulos
private val Tipografia = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

// Tema de la app. Lee de Ajustes el modo (sistema/claro/oscuro) y si se usan los colores
// de Klippo o los del fondo de pantalla (Material You).
@Composable
fun KlippoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val ajustes = (context.applicationContext as KlippoApp).ajustes
    val tema by ajustes.tema.collectAsState(initial = Tema.SISTEMA)
    val coloresSistema by ajustes.coloresSistema.collectAsState(initial = false)

    val oscuro = when (tema) {
        Tema.SISTEMA -> isSystemInDarkTheme()
        Tema.CLARO -> false
        Tema.OSCURO -> true
    }
    val colores = when {
        coloresSistema && oscuro -> dynamicDarkColorScheme(context)
        coloresSistema -> dynamicLightColorScheme(context)
        oscuro -> EsquemaOscuro
        else -> EsquemaClaro
    }
    MaterialTheme(colorScheme = colores, shapes = Formas, typography = Tipografia, content = content)
}
