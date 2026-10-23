package es.rboluda.klippo

import android.app.Application
import es.rboluda.klippo.descarga.MotorDescarga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// Punto de arranque: crea las dependencias compartidas (sin Hilt) y pone en marcha el motor
class KlippoApp : Application() {

    // Ámbito que vive lo mismo que el proceso
    val ambito = CoroutineScope(SupervisorJob())

    lateinit var motor: MotorDescarga
        private set

    override fun onCreate() {
        super.onCreate()
        motor = MotorDescarga(this)
        motor.iniciar(ambito)
    }
}
