package es.rboluda.klippo.ui.comun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import es.rboluda.klippo.R
import es.rboluda.klippo.ui.theme.NaranjaKlippo
import es.rboluda.klippo.ui.theme.RojoKlippo

// El icono de la app dentro de la interfaz: degradado de la marca y las tijeras amarillas
@Composable
fun LogoKlippo(tamano: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.3f))
            .background(Brush.linearGradient(listOf(NaranjaKlippo, RojoKlippo))),
        contentAlignment = Alignment.Center,
    ) {
        // La capa del icono adaptativo tiene margen (zona segura): se amplía para llenar el hueco
        Image(
            painter = painterResource(R.drawable.ic_launcher_primer_plano),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.45f),
        )
    }
}
