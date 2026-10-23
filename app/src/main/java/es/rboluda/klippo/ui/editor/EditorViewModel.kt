package es.rboluda.klippo.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.rboluda.klippo.KlippoApp
import es.rboluda.klippo.descarga.Descarga
import es.rboluda.klippo.ui.Rutas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Estado del editor. En la Fase 2 guardará el recorte; en la 3, encuadre, emojis y pixelado.
// La UI solo recoge parámetros: el vídeo no se toca hasta exportar.
class EditorViewModel(app: KlippoApp, estado: SavedStateHandle) : ViewModel() {

    private val id: String = checkNotNull(estado[Rutas.ARG_ID])

    val descarga: StateFlow<Descarga?> = app.descargas.descargas
        .map { lista -> lista.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, app.descargas.buscar(id))

    companion object {
        val Factory = viewModelFactory {
            initializer { EditorViewModel(this[APPLICATION_KEY] as KlippoApp, createSavedStateHandle()) }
        }
    }
}
