package es.rboluda.klippo.ajustes

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.rboluda.klippo.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Calidad máxima de descarga; null = la mejor disponible
enum class Calidad(val alturaMax: Int?, @param:StringRes val etiqueta: Int) {
    MEJOR(null, R.string.calidad_mejor),
    P1080(1080, R.string.calidad_1080),
    P720(720, R.string.calidad_720),
    P480(480, R.string.calidad_480),
}

private val Context.almacen by preferencesDataStore(name = "ajustes")

// Últimos ajustes usados: se aplican también a los enlaces recibidos con Compartir
class AjustesRepositorio(private val context: Context) {

    private val claveCalidad = stringPreferencesKey("calidad")

    val calidad: Flow<Calidad> = context.almacen.data.map { prefs ->
        prefs[claveCalidad]?.let { nombre -> Calidad.entries.find { it.name == nombre } } ?: Calidad.MEJOR
    }

    suspend fun calidadActual(): Calidad = calidad.first()

    suspend fun guardarCalidad(calidad: Calidad) {
        context.almacen.edit { it[claveCalidad] = calidad.name }
    }
}
