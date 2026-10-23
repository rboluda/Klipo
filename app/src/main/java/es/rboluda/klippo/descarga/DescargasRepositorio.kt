package es.rboluda.klippo.descarga

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Lista de descargas compartida entre el servicio y la UI (solo en memoria por ahora)
class DescargasRepositorio {

    private val _descargas = MutableStateFlow<List<Descarga>>(emptyList())
    val descargas: StateFlow<List<Descarga>> = _descargas.asStateFlow()

    fun agregar(descarga: Descarga) {
        _descargas.update { listOf(descarga) + it }
    }

    fun actualizar(id: String, cambio: (Descarga) -> Descarga) {
        _descargas.update { lista -> lista.map { if (it.id == id) cambio(it) else it } }
    }

    fun buscar(id: String): Descarga? = _descargas.value.find { it.id == id }
}
