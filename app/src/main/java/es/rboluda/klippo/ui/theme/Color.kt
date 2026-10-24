package es.rboluda.klippo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Colores de la marca, sacados del icono: degradado naranja → rojo y tijeras amarillas
val NaranjaKlippo = Color(0xFFFC9703)
val RojoKlippo = Color(0xFFFB2716)
val AmarilloKlippo = Color(0xFFFCF905)

// Esquema claro: naranja tostado como principal (legible sobre blanco), neutros cálidos
val EsquemaClaro = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF8A4F36),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE5D9),
    onSecondaryContainer = Color(0xFF341000),
    tertiary = Color(0xFF6B5E00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF9E64B),
    onTertiaryContainer = Color(0xFF201C00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231914),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231914),
    surfaceVariant = Color(0xFFF5DED4),
    onSurfaceVariant = Color(0xFF53433C),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD8C2B9),
    surfaceDim = Color(0xFFE8D6CF),
    surfaceBright = Color(0xFFFFF8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFCEAE3),
    surfaceContainerHigh = Color(0xFFF6E4DD),
    surfaceContainerHighest = Color(0xFFF0DFD8),
    inverseSurface = Color(0xFF392E29),
    inverseOnSurface = Color(0xFFFFEDE6),
    inversePrimary = Color(0xFFFFB596),
)

// Esquema oscuro: el naranja se aclara para contrastar sobre fondos casi negros
val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5C1A00),
    primaryContainer = Color(0xFF9A3412),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFFFB59A),
    onSecondary = Color(0xFF532210),
    secondaryContainer = Color(0xFF6E3823),
    onSecondaryContainer = Color(0xFFFFDBCE),
    tertiary = Color(0xFFDCCA2E),
    onTertiary = Color(0xFF373100),
    tertiaryContainer = Color(0xFF504700),
    onTertiaryContainer = Color(0xFFF9E64B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A110D),
    onBackground = Color(0xFFF1DFD8),
    surface = Color(0xFF1A110D),
    onSurface = Color(0xFFF1DFD8),
    surfaceVariant = Color(0xFF53433C),
    onSurfaceVariant = Color(0xFFD8C2B9),
    outline = Color(0xFFA08D85),
    outlineVariant = Color(0xFF53433C),
    surfaceDim = Color(0xFF1A110D),
    surfaceBright = Color(0xFF423732),
    surfaceContainerLowest = Color(0xFF140C09),
    surfaceContainerLow = Color(0xFF231914),
    surfaceContainer = Color(0xFF271D18),
    surfaceContainerHigh = Color(0xFF322722),
    surfaceContainerHighest = Color(0xFF3D322D),
    inverseSurface = Color(0xFFF1DFD8),
    inverseOnSurface = Color(0xFF392E29),
    inversePrimary = Color(0xFFA63A0F),
)
