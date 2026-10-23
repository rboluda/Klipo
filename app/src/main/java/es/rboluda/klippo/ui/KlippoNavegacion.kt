package es.rboluda.klippo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.rboluda.klippo.ui.exportar.ExportarScreen
import es.rboluda.klippo.ui.inicio.InicioScreen
import es.rboluda.klippo.ui.inicio.InicioViewModel

// Pantallas de la app: Inicio → (Editor, en la Fase 2) → Exportar
object Rutas {
    const val INICIO = "inicio"
    const val ARG_ID = "id"
    const val EXPORTAR = "exportar/{$ARG_ID}"

    fun exportar(id: String) = "exportar/$id"
}

@Composable
fun KlippoNavegacion(inicioViewModel: InicioViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rutas.INICIO) {
        composable(Rutas.INICIO) {
            InicioScreen(
                viewModel = inicioViewModel,
                onAbrir = { id -> nav.navigate(Rutas.exportar(id)) },
            )
        }
        composable(
            Rutas.EXPORTAR,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType }),
        ) {
            ExportarScreen(onVolver = { nav.popBackStack() })
        }
    }
}
