package es.rboluda.klippo.ui.inicio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.rboluda.klippo.KlippoApp
import es.rboluda.klippo.R
import es.rboluda.klippo.ajustes.Calidad
import es.rboluda.klippo.descarga.Descarga
import es.rboluda.klippo.descarga.DescargaService
import es.rboluda.klippo.descarga.EstadoMotor
import es.rboluda.klippo.descarga.MotorDescarga
import es.rboluda.klippo.descarga.ResultadoActualizacion
import es.rboluda.klippo.sesion.Plataforma
import es.rboluda.klippo.sesion.Sesiones
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class InicioViewModel(private val app: KlippoApp) : ViewModel() {

    private val motor: MotorDescarga = app.motor

    val estadoMotor: StateFlow<EstadoMotor> = motor.estado
    val descargas: StateFlow<List<Descarga>> = app.descargas.descargas

    // La calidad se pregunta en cada vídeo; la última elegida queda marcada la próxima vez
    fun descargar(url: String, calidad: Calidad) {
        ultimaUrlPegada = url.trim()
        viewModelScope.launch { app.ajustes.guardarCalidad(calidad) }
        DescargaService.descargar(app, url.trim(), calidad)
    }

    val ultimaCalidad: StateFlow<Calidad> = app.ajustes.calidad
        .stateIn(viewModelScope, SharingStarted.Eagerly, Calidad.MEJOR)

    // Enlace recibido desde Compartir de otra app; la pantalla lo consume y lo descarga
    private val _urlCompartida = MutableStateFlow<String?>(null)
    val urlCompartida: StateFlow<String?> = _urlCompartida.asStateFlow()

    fun recibirCompartido(url: String) {
        ultimaUrlPegada = url
        _urlCompartida.value = url
    }

    fun compartidoConsumido() {
        _urlCompartida.value = null
    }

    // Evita volver a pegar del portapapeles un enlace ya usado
    private var ultimaUrlPegada: String? = null

    fun debeAutorrellenar(url: String): Boolean {
        if (url == ultimaUrlPegada || descargas.value.any { it.url == url }) return false
        ultimaUrlPegada = url
        return true
    }

    fun cancelar(id: String) {
        DescargaService.cancelar(app, id)
    }

    // El permiso de notificaciones se pide solo una vez, en la primera descarga
    private val preferencias = app.getSharedPreferences("klippo", Context.MODE_PRIVATE)

    fun debePedirPermisoNotificaciones(): Boolean {
        val concedido = ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return !concedido && !preferencias.getBoolean(CLAVE_PERMISO_PEDIDO, false)
    }

    fun marcarPermisoPedido() {
        preferencias.edit { putBoolean(CLAVE_PERMISO_PEDIDO, true) }
    }

    private val _actualizando = MutableStateFlow(false)
    val actualizando: StateFlow<Boolean> = _actualizando.asStateFlow()

    // Mensajes puntuales para el snackbar
    private val _avisos = Channel<String>(Channel.BUFFERED)
    val avisos = _avisos.receiveAsFlow()

    // Plataformas con sesión iniciada (se refresca al volver a la pantalla)
    private val _sesiones = MutableStateFlow<Set<Plataforma>>(emptySet())
    val sesiones: StateFlow<Set<Plataforma>> = _sesiones.asStateFlow()

    fun refrescarSesiones() {
        _sesiones.value = Plataforma.entries.filter { Sesiones.tieneSesion(app, it) }.toSet()
    }

    fun cerrarSesion(plataforma: Plataforma) {
        Sesiones.cerrar(app, plataforma)
        refrescarSesiones()
        _avisos.trySend(app.getString(R.string.sesion_cerrada, plataforma.nombre))
    }

    // Días desde la publicación de la versión de yt-dlp (se calcula en local, sin red)
    val diasMotor: StateFlow<Int?> = motor.estado
        .map { (it as? EstadoMotor.Listo)?.version?.let(::diasDesdeVersion) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Si se indica [reintentar], tras actualizar se relanza esa descarga fallida
    fun actualizarMotor(reintentar: Descarga? = null) {
        if (_actualizando.value) return
        _actualizando.value = true
        viewModelScope.launch {
            val resultado = motor.actualizar()
            @StringRes val aviso = when (resultado) {
                ResultadoActualizacion.ACTUALIZADO -> R.string.motor_actualizado
                ResultadoActualizacion.YA_AL_DIA -> R.string.motor_ya_al_dia
                ResultadoActualizacion.ERROR -> R.string.motor_error_actualizar
            }
            _actualizando.value = false
            if (reintentar != null && resultado != ResultadoActualizacion.ERROR) {
                app.descargas.eliminar(reintentar.id)
                withContext(Dispatchers.IO) { reintentar.carpeta.deleteRecursively() }
                descargar(reintentar.url, reintentar.calidad)
            }
            _avisos.send(app.getString(aviso))
        }
    }

    companion object {
        private const val CLAVE_PERMISO_PEDIDO = "permiso_notificaciones_pedido"
        const val DIAS_MOTOR_ANTIGUO = 30

        // Las versiones de yt-dlp son fechas: 2026.10.20 (o 2026.10.20.123456 en nightly)
        private val FECHA_VERSION = Regex("""^(\d{4})\.(\d{1,2})\.(\d{1,2})""")

        private fun diasDesdeVersion(version: String): Int? {
            val (anio, mes, dia) = FECHA_VERSION.find(version)?.destructured ?: return null
            return try {
                val fecha = LocalDate.of(anio.toInt(), mes.toInt(), dia.toInt())
                ChronoUnit.DAYS.between(fecha, LocalDate.now()).toInt()
            } catch (e: Exception) {
                null
            }
        }

        val Factory = viewModelFactory {
            initializer { InicioViewModel(this[APPLICATION_KEY] as KlippoApp) }
        }
    }
}
