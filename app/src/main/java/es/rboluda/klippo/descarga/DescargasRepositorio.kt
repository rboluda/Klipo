package es.rboluda.klippo.descarga

import es.rboluda.klippo.almacenamiento.MetaDescarga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

// Lista de descargas compartida entre el servicio y la UI.
// Las completadas se reconstruyen desde disco; las demás solo viven en memoria.
class DescargasRepositorio {

    private val _descargas = MutableStateFlow<List<Descarga>>(emptyList())
    val descargas: StateFlow<List<Descarga>> = _descargas.asStateFlow()

    // La lista va de la más reciente a la más antigua
    fun agregar(descarga: Descarga) {
        _descargas.update { (listOf(descarga) + it).sortedByDescending { d -> d.creado } }
    }

    fun actualizar(id: String, cambio: (Descarga) -> Descarga) {
        _descargas.update { lista -> lista.map { if (it.id == id) cambio(it) else it } }
    }

    fun eliminar(id: String) {
        _descargas.update { lista -> lista.filterNot { it.id == id } }
    }

    fun buscar(id: String): Descarga? = _descargas.value.find { it.id == id }

    fun idsActivos(): Set<String> =
        _descargas.value.filter { it.estado == EstadoDescarga.Descargando }.map { it.id }.toSet()

    // Sustituye las completadas por lo que hay realmente en [raiz] (p. ej. tras la limpieza)
    fun sincronizar(raiz: File) {
        val enDisco = raiz.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { MetaDescarga.leerCompletada(it) }
            .orEmpty()
        _descargas.update { actual ->
            val noCompletadas = actual.filter { it.estado != EstadoDescarga.Completada }
            val ids = noCompletadas.map { it.id }.toSet()
            (noCompletadas + enDisco.filterNot { it.id in ids }).sortedByDescending { it.creado }
        }
    }
}
