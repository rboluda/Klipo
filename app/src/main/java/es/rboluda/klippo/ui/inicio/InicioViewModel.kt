package es.rboluda.klippo.ui.inicio

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.rboluda.klippo.KlippoApp
import es.rboluda.klippo.R
import es.rboluda.klippo.descarga.EstadoMotor
import es.rboluda.klippo.descarga.MotorDescarga
import es.rboluda.klippo.descarga.ResultadoActualizacion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class InicioViewModel(private val motor: MotorDescarga) : ViewModel() {

    val estadoMotor: StateFlow<EstadoMotor> = motor.estado

    private val _actualizando = MutableStateFlow(false)
    val actualizando: StateFlow<Boolean> = _actualizando.asStateFlow()

    // Mensajes puntuales para el snackbar
    private val _avisos = Channel<Int>(Channel.BUFFERED)
    val avisos = _avisos.receiveAsFlow()

    fun actualizarMotor() {
        if (_actualizando.value) return
        _actualizando.value = true
        viewModelScope.launch {
            @StringRes val aviso = when (motor.actualizar()) {
                ResultadoActualizacion.ACTUALIZADO -> R.string.motor_actualizado
                ResultadoActualizacion.YA_AL_DIA -> R.string.motor_ya_al_dia
                ResultadoActualizacion.ERROR -> R.string.motor_error_actualizar
            }
            _actualizando.value = false
            _avisos.send(aviso)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { InicioViewModel((this[APPLICATION_KEY] as KlippoApp).motor) }
        }
    }
}
