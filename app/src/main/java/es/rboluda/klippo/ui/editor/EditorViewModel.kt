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
import es.rboluda.klippo.edicion.EmojiSuperpuesto
import es.rboluda.klippo.edicion.Encuadre
import es.rboluda.klippo.edicion.EstiloTexto
import es.rboluda.klippo.edicion.Formato
import es.rboluda.klippo.edicion.MedidasVideo
import es.rboluda.klippo.edicion.ParametrosEdicion
import es.rboluda.klippo.edicion.Recorte
import es.rboluda.klippo.edicion.ZonaPixelada
import es.rboluda.klippo.ui.Rutas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    // ArrayList: SavedStateHandle necesita un tipo Serializable concreto
    val emojis: StateFlow<ArrayList<EmojiSuperpuesto>> =
        estado.getStateFlow(CLAVE_EMOJIS, ArrayList())
    val pixelados: StateFlow<ArrayList<ZonaPixelada>> =
        estado.getStateFlow(CLAVE_PIXELADOS, ArrayList())

    init {
        viewModelScope.launch {
            // Al abrir desde la notificación con la app recién arrancada, la lista aún se está
            // cargando desde la caché: se espera a que aparezca el vídeo
            val archivo = descarga.mapNotNull { it?.archivo }.first()
            val medidas = withContext(Dispatchers.IO) { MedidasVideo.leer(archivo) }
            _medidas.value = medidas
            if (recorte.value == null && medidas.duracionMs > 0) {
                estado[CLAVE_RECORTE] = Recorte(0, medidas.duracionMs)
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

    // Los emojis nuevos aparecen en el centro; se colocan después con gestos
    fun agregarEmoji(texto: String) {
        guardarEmojis(emojis.value + EmojiSuperpuesto(id = System.nanoTime(), texto = texto))
    }

    // Los textos van en la misma lista que los emojis (mismos gestos y misma exportación)
    fun agregarTexto(texto: String, estilo: EstiloTexto) {
        guardarEmojis(
            emojis.value + EmojiSuperpuesto(
                id = System.nanoTime(),
                texto = texto,
                tamano = EmojiSuperpuesto.TAMANO_INICIAL_TEXTO,
                estilo = estilo,
            )
        )
    }

    val silenciar: StateFlow<Boolean> = estado.getStateFlow(CLAVE_SILENCIAR, false)

    fun alternarSilencio() {
        estado[CLAVE_SILENCIAR] = !silenciar.value
    }

    fun moverEmoji(id: Long, dx: Float, dy: Float) = cambiarEmoji(id) { it.mover(dx, dy) }

    fun escalarEmoji(id: Long, factor: Float) = cambiarEmoji(id) { it.escalar(factor) }

    fun borrarEmoji(id: Long) {
        guardarEmojis(emojis.value.filterNot { it.id == id })
    }

    private fun cambiarEmoji(id: Long, cambio: (EmojiSuperpuesto) -> EmojiSuperpuesto) {
        guardarEmojis(emojis.value.map { if (it.id == id) cambio(it) else it })
    }

    private fun guardarEmojis(lista: List<EmojiSuperpuesto>) {
        estado[CLAVE_EMOJIS] = ArrayList(lista)
    }

    // Las zonas nuevas aparecen en el centro; se colocan después con gestos
    fun agregarPixelado() {
        val aspecto = _medidas.value?.aspecto?.takeIf { it > 0 } ?: return
        guardarPixelados(pixelados.value + ZonaPixelada.nueva(aspecto))
    }

    fun cambiarPixelado(zona: ZonaPixelada) {
        guardarPixelados(pixelados.value.map { if (it.id == zona.id) zona else it })
    }

    fun borrarPixelado(id: Long) {
        guardarPixelados(pixelados.value.filterNot { it.id == id })
    }

    private fun guardarPixelados(lista: List<ZonaPixelada>) {
        estado[CLAVE_PIXELADOS] = ArrayList(lista)
    }

    // Lo que se aplicará al exportar (sin recorte si se deja el vídeo entero)
    fun parametros(): ParametrosEdicion {
        val duracion = _medidas.value?.duracionMs ?: 0L
        val recorteEfectivo = recorte.value?.takeIf { it.inicioMs > 0 || it.finMs < duracion }
        return ParametrosEdicion(
            recorte = recorteEfectivo,
            encuadre = encuadre.value,
            emojis = emojis.value.toList(),
            pixelados = pixelados.value.toList(),
            silenciar = silenciar.value,
        )
    }

    companion object {
        private const val CLAVE_RECORTE = "recorte"
        private const val CLAVE_ENCUADRE = "encuadre"
        private const val CLAVE_EMOJIS = "emojis"
        private const val CLAVE_PIXELADOS = "pixelados"
        private const val CLAVE_SILENCIAR = "silenciar"
        const val DURACION_MINIMA_MS = 500L

        val Factory = viewModelFactory {
            initializer { EditorViewModel(this[APPLICATION_KEY] as KlippoApp, createSavedStateHandle()) }
        }
    }
}
