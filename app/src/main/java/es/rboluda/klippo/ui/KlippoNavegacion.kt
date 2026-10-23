package es.rboluda.klippo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.rboluda.klippo.edicion.Recorte
import es.rboluda.klippo.ui.editor.EditorScreen
import es.rboluda.klippo.ui.exportar.ExportarScreen
import es.rboluda.klippo.ui.inicio.InicioScreen
import es.rboluda.klippo.ui.inicio.InicioViewModel

// Pantallas de la app (solo tres): Inicio → Editor → Exportar
object Rutas {
    const val INICIO = "inicio"
    const val ARG_ID = "id"
    // Parámetros de edición que el editor pasa a Exportar (-1 = sin recorte)
    const val ARG_INICIO = "inicio"
    const val ARG_FIN = "fin"
    const val EDITOR = "editor/{$ARG_ID}"
    const val EXPORTAR = "exportar/{$ARG_ID}?$ARG_INICIO={$ARG_INICIO}&$ARG_FIN={$ARG_FIN}"

    fun editor(id: String) = "editor/$id"
    fun exportar(id: String, recorte: Recorte?) =
        "exportar/$id?$ARG_INICIO=${recorte?.inicioMs ?: -1}&$ARG_FIN=${recorte?.finMs ?: -1}"
}

@Composable
fun KlippoNavegacion(inicioViewModel: InicioViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rutas.INICIO) {
        composable(Rutas.INICIO) {
            InicioScreen(
                viewModel = inicioViewModel,
                onAbrir = { id -> nav.navigate(Rutas.editor(id)) },
            )
        }
        composable(
            Rutas.EDITOR,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType }),
        ) {
            EditorScreen(
                onVolver = { nav.popBackStack() },
                onSiguiente = { id, recorte -> nav.navigate(Rutas.exportar(id, recorte)) },
            )
        }
        composable(
            Rutas.EXPORTAR,
            arguments = listOf(
                navArgument(Rutas.ARG_ID) { type = NavType.StringType },
                navArgument(Rutas.ARG_INICIO) { type = NavType.LongType; defaultValue = -1L },
                navArgument(Rutas.ARG_FIN) { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            ExportarScreen(onVolver = { nav.popBackStack() })
        }
    }
}
