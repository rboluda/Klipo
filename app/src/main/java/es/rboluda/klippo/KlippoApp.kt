package es.rboluda.klippo

import android.app.Application
import es.rboluda.klippo.ajustes.AjustesRepositorio
import es.rboluda.klippo.almacenamiento.LimpiezaCache
import es.rboluda.klippo.descarga.DescargaService
import es.rboluda.klippo.descarga.DescargasRepositorio
import es.rboluda.klippo.descarga.MotorDescarga
import es.rboluda.klippo.descarga.Notificaciones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

// Punto de arranque: crea las dependencias compartidas (sin Hilt) y pone en marcha el motor
class KlippoApp : Application() {

    // Ámbito que vive lo mismo que el proceso
    val ambito = CoroutineScope(SupervisorJob())

    lateinit var motor: MotorDescarga
        private set

    val descargas = DescargasRepositorio()

    val ajustes by lazy { AjustesRepositorio(this) }

    val carpetaDescargas: File get() = File(cacheDir, DescargaService.CARPETA_DESCARGAS)

    override fun onCreate() {
        super.onCreate()
        Notificaciones.crearCanales(this)
        motor = MotorDescarga(this)
        motor.iniciar(ambito)
    }

    // Se llama cada vez que la app pasa a primer plano: borra lo antiguo y recarga la lista
    fun limpiarYCargar() {
        ambito.launch(Dispatchers.IO) {
            LimpiezaCache.limpiar(cacheDir, carpetaDescargas, descargas.idsActivos())
            descargas.sincronizar(carpetaDescargas)
        }
    }
}
