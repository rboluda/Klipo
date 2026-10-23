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
import es.rboluda.klippo.edicion.Encuadre
import es.rboluda.klippo.edicion.Formato
import es.rboluda.klippo.edicion.ParametrosEdicion
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

// Medidas del vídeo ya girado (como se ve en pantalla)
data class MedidasVideo(val duracionMs: Long, val ancho: Int, val alto: Int) {
    val aspecto: Float get() = if (ancho > 0 && alto > 0) ancho.toFloat() / alto else 0f
}

// Estado del editor. La UI solo recoge parámetros: el vídeo no se toca hasta exportar.
// Todo se guarda en SavedStateHandle para no perderlo al girar la pantalla.
class EditorViewModel(app: KlippoApp, private val estado: SavedStateHandle) : ViewModel() {

    private val id: String = checkNotNull(estado[Rutas.ARG_ID])

    val descarga: StateFlow<Descarga?> = app.descargas.descargas
        .map { lista -> lista.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, app.descargas.buscar(id))

    private val _medidas = MutableStateFlow<MedidasVideo?>(null)
    val medidas: StateFlow<MedidasVideo?> = _medidas.asStateFlow()

    val recorte: StateFlow<Recorte?> = estado.getStateFlow<Recorte?>(CLAVE_RECORTE, null)
    val encuadre: StateFlow<Encuadre?> = estado.getStateFlow<Encuadre?>(CLAVE_ENCUADRE, null)

    init {
        app.descargas.buscar(id)?.archivo?.let { archivo ->
            viewModelScope.launch {
                val medidas = withContext(Dispatchers.IO) { leerMedidas(archivo.absolutePath) }
                _medidas.value = medidas
                if (recorte.value == null && medidas.duracionMs > 0) {
                    estado[CLAVE_RECORTE] = Recorte(0, medidas.duracionMs)
                }
            }
        }
    }

    // Mantiene un mínimo de medio segundo entre inicio y fin
    fun cambiarRecorte(inicioMs: Long, finMs: Long) {
        val duracion = _medidas.value?.duracionMs ?: return
        val inicio = inicioMs.coerceIn(0, duracion)
        val fin = finMs.coerceIn(0, duracion)
        if (fin - inicio < DURACION_MINIMA_MS) return
        estado[CLAVE_RECORTE] = Recorte(inicio, fin)
    }

    // null = vídeo completo (sin encuadre)
    fun elegirFormato(formato: Formato?) {
        val aspecto = _medidas.value?.aspecto?.takeIf { it > 0 } ?: return
        estado[CLAVE_ENCUADRE] = formato?.let { Encuadre.centrado(it, aspecto) }
    }

    fun cambiarEncuadre(encuadre: Encuadre) {
        estado[CLAVE_ENCUADRE] = encuadre
    }

    // Lo que se aplicará al exportar (sin recorte si se deja el vídeo entero)
    fun parametros(): ParametrosEdicion {
        val duracion = _medidas.value?.duracionMs ?: 0L
        val recorteEfectivo = recorte.value?.takeIf { it.inicioMs > 0 || it.finMs < duracion }
        return ParametrosEdicion(recorte = recorteEfectivo, encuadre = encuadre.value)
    }

    private fun leerMedidas(ruta: String): MedidasVideo {
        val lector = MediaMetadataRetriever()
        return try {
            lector.setDataSource(ruta)
            fun entero(clave: Int) = lector.extractMetadata(clave)?.toLongOrNull() ?: 0L
            var ancho = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            var alto = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            val rotacion = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
            if (rotacion == 90 || rotacion == 270) ancho = alto.also { alto = ancho }
            MedidasVideo(entero(MediaMetadataRetriever.METADATA_KEY_DURATION), ancho, alto)
        } catch (e: Exception) {
            MedidasVideo(0, 0, 0)
        } finally {
            lector.release()
        }
    }

    companion object {
        private const val CLAVE_RECORTE = "recorte"
        private const val CLAVE_ENCUADRE = "encuadre"
        const val DURACION_MINIMA_MS = 500L

        val Factory = viewModelFactory {
            initializer { EditorViewModel(this[APPLICATION_KEY] as KlippoApp, createSavedStateHandle()) }
        }
    }
}
