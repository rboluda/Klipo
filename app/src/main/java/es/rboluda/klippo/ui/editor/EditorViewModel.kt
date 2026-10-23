package es.rboluda.klippo.ui.editor

import android.media.MediaMetadataRetriever
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.rboluda.klippo.KlippoApp
import es.rboluda.klippo.descarga.Descarga
import es.rboluda.klippo.edicion.Recorte
import es.rboluda.klippo.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Estado del editor. La UI solo recoge parámetros: el vídeo no se toca hasta exportar.
// En la Fase 3 se añadirán encuadre, emojis y pixelado.
class EditorViewModel(app: KlippoApp, private val estado: SavedStateHandle) : ViewModel() {

    private val id: String = checkNotNull(estado[Rutas.ARG_ID])

    val descarga: StateFlow<Descarga?> = app.descargas.descargas
        .map { lista -> lista.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, app.descargas.buscar(id))

    private val _duracionMs = MutableStateFlow(0L)
    val duracionMs: StateFlow<Long> = _duracionMs.asStateFlow()

    // Se guarda en SavedStateHandle para no perderlo al girar la pantalla
    val recorte: StateFlow<Recorte?> = estado.getStateFlow<Recorte?>(CLAVE_RECORTE, null)

    init {
        app.descargas.buscar(id)?.archivo?.let { archivo ->
            viewModelScope.launch {
                val duracion = withContext(Dispatchers.IO) {
                    val lector = MediaMetadataRetriever()
                    try {
                        lector.setDataSource(archivo.absolutePath)
                        lector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    } catch (e: Exception) {
                        0L
                    } finally {
                        lector.release()
                    }
                }
                _duracionMs.value = duracion
                if (recorte.value == null && duracion > 0) estado[CLAVE_RECORTE] = Recorte(0, duracion)
            }
        }
    }

    // Mantiene un mínimo de medio segundo entre inicio y fin
    fun cambiarRecorte(inicioMs: Long, finMs: Long) {
        val duracion = _duracionMs.value
        if (duracion <= 0) return
        val inicio = inicioMs.coerceIn(0, duracion)
        val fin = finMs.coerceIn(0, duracion)
        if (fin - inicio < DURACION_MINIMA_MS) return
        estado[CLAVE_RECORTE] = Recorte(inicio, fin)
    }

    // Recorte que hay que aplicar al exportar; null si se deja el vídeo entero
    fun recorteEfectivo(): Recorte? {
        val r = recorte.value ?: return null
        return r.takeIf { it.inicioMs > 0 || it.finMs < _duracionMs.value }
    }

    companion object {
        private const val CLAVE_RECORTE = "recorte"
        const val DURACION_MINIMA_MS = 500L

        val Factory = viewModelFactory {
            initializer { EditorViewModel(this[APPLICATION_KEY] as KlippoApp, createSavedStateHandle()) }
        }
    }
}
