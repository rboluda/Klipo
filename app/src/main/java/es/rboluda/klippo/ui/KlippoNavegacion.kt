package es.rboluda.klippo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.rboluda.klippo.ui.editor.EditorScreen
import es.rboluda.klippo.ui.editor.EditorViewModel
import es.rboluda.klippo.ui.exportar.ExportarScreen
import es.rboluda.klippo.ui.inicio.InicioScreen
import es.rboluda.klippo.ui.inicio.InicioViewModel

// Pantallas de la app (solo tres): Inicio → Editor → Exportar
object Rutas {
    const val INICIO = "inicio"
    const val ARG_ID = "id"
    const val EDITOR = "editor/{$ARG_ID}"
    const val EXPORTAR = "exportar/{$ARG_ID}"

    fun editor(id: String) = "editor/$id"
    fun exportar(id: String) = "exportar/$id"
}

private val argumentoId = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType })

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
        composable(Rutas.EDITOR, arguments = argumentoId) {
            EditorScreen(
                onVolver = { nav.popBackStack() },
                onSiguiente = { id -> nav.navigate(Rutas.exportar(id)) },
            )
        }
        composable(Rutas.EXPORTAR, arguments = argumentoId) { entrada ->
            // Los parámetros de edición viven en el ViewModel del editor (que sigue debajo en
            // la pila y los guarda en SavedStateHandle): así no hay que pasarlos por la ruta
            val entradaEditor = remember(entrada) { nav.getBackStackEntry(Rutas.EDITOR) }
            val editor: EditorViewModel = viewModel(entradaEditor, factory = EditorViewModel.Factory)
            ExportarScreen(
                parametros = remember(entrada) { editor.parametros() },
                onVolver = { nav.popBackStack() },
            )
        }
    }
}
