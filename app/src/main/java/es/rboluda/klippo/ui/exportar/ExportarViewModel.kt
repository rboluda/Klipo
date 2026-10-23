package es.rboluda.klippo.ui.exportar

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.rboluda.klippo.KlippoApp
import es.rboluda.klippo.R
import es.rboluda.klippo.almacenamiento.GuardarEnGaleria
import es.rboluda.klippo.descarga.Descarga
import es.rboluda.klippo.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Datos del vídeo leídos del propio archivo (sin red)
data class InfoVideo(
    val duracionMs: Long,
    val ancho: Int,
    val alto: Int,
    val bytes: Long,
    val miniatura: Bitmap?,
)

class ExportarViewModel(private val app: KlippoApp, estado: SavedStateHandle) : ViewModel() {

    private val id: String = checkNotNull(estado[Rutas.ARG_ID])

    val descarga: StateFlow<Descarga?> = app.descargas.descargas
        .map { lista -> lista.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, app.descargas.buscar(id))

    private val _info = MutableStateFlow<InfoVideo?>(null)
    val info: StateFlow<InfoVideo?> = _info.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    private val _avisos = Channel<String>(Channel.BUFFERED)
    val avisos = _avisos.receiveAsFlow()

    init {
        app.descargas.buscar(id)?.archivo?.let { archivo ->
            viewModelScope.launch { _info.value = leerInfo(archivo) }
        }
    }

    // Solo Android 13: en 14+ se guarda desde la acción del share sheet
    fun guardarEnGaleria() {
        val archivo = descarga.value?.archivo ?: return
        if (_guardando.value) return
        _guardando.value = true
        viewModelScope.launch {
            val texto = try {
                GuardarEnGaleria.guardar(app, archivo)
                app.getString(R.string.galeria_guardado)
            } catch (e: Exception) {
                app.getString(R.string.galeria_error)
            }
            _guardando.value = false
            _avisos.send(texto)
        }
    }

    private suspend fun leerInfo(archivo: File): InfoVideo? = withContext(Dispatchers.IO) {
        val lector = MediaMetadataRetriever()
        try {
            lector.setDataSource(archivo.absolutePath)
            fun entero(clave: Int) = lector.extractMetadata(clave)?.toLongOrNull() ?: 0L
            var ancho = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            var alto = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            // Los vídeos grabados en vertical a veces vienen girados por metadatos
            val rotacion = entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
            if (rotacion == 90 || rotacion == 270) ancho = alto.also { alto = ancho }
            InfoVideo(
                duracionMs = entero(MediaMetadataRetriever.METADATA_KEY_DURATION),
                ancho = ancho,
                alto = alto,
                bytes = archivo.length(),
                miniatura = lector.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC),
            )
        } catch (e: Exception) {
            null
        } finally {
            lector.release()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ExportarViewModel(this[APPLICATION_KEY] as KlippoApp, createSavedStateHandle()) }
        }
    }
}
