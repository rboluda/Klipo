package es.rboluda.klippo.ui.inicio

import android.Manifest
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.rboluda.klippo.BuildConfig
import es.rboluda.klippo.R
import es.rboluda.klippo.ajustes.Calidad
import es.rboluda.klippo.util.Enlaces
import es.rboluda.klippo.descarga.EstadoMotor
import es.rboluda.klippo.sesion.Plataforma
import es.rboluda.klippo.sesion.SesionActivity

// Pantalla de inicio: campo de URL y lista de descargas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory)) {
    val estadoMotor by viewModel.estadoMotor.collectAsStateWithLifecycle()
    val actualizando by viewModel.actualizando.collectAsStateWithLifecycle()
    val descargas by viewModel.descargas.collectAsStateWithLifecycle()
    val diasMotor by viewModel.diasMotor.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf("") }
    var urlPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val sesiones by viewModel.sesiones.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val iniciarSesion = { p: Plataforma -> context.startActivity(SesionActivity.intent(context, p)) }

    // Al volver de la pantalla de login, la sesión puede haber cambiado
    LifecycleResumeEffect(Unit) {
        viewModel.refrescarSesiones()
        onPauseOrDispose { }
    }

    val calidad by viewModel.calidad.collectAsStateWithLifecycle()
    val urlCompartida by viewModel.urlCompartida.collectAsStateWithLifecycle()

    // Tras responder al permiso (sí o no) se lanza la descarga igualmente
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        urlPendiente?.let(viewModel::descargar)
        urlPendiente = null
    }

    // El permiso de notificaciones se pide en la primera descarga, venga de donde venga
    val lanzarDescarga = { enlace: String ->
        if (viewModel.debePedirPermisoNotificaciones()) {
            viewModel.marcarPermisoPedido()
            urlPendiente = enlace
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.descargar(enlace)
        }
    }

    val empezarDescarga = {
        if (url.isNotBlank() && estadoMotor is EstadoMotor.Listo) {
            lanzarDescarga(url)
            url = ""
        }
    }

    // Enlace compartido desde otra app: se descarga directamente (el servicio espera
    // a que el motor esté listo si aún está arrancando)
    LaunchedEffect(urlCompartida) {
        urlCompartida?.let {
            lanzarDescarga(it)
            viewModel.compartidoConsumido()
        }
    }

    // Autorrelleno desde el portapapeles. Android solo deja leerlo con la ventana
    // enfocada, así que se hace al ganar el foco (al abrir o volver a la app)
    val ventanaEnfocada = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(ventanaEnfocada) {
        if (!ventanaEnfocada || url.isNotBlank()) return@LaunchedEffect
        val portapapeles = context.getSystemService(ClipboardManager::class.java)
        val texto = portapapeles.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(context)?.toString()
        val enlace = Enlaces.extraerUrl(texto)
        if (enlace != null && Enlaces.esSoportada(enlace) && viewModel.debeAutorrellenar(enlace)) {
            url = enlace
        }
    }

    LaunchedEffect(Unit) {
        viewModel.avisos.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        // Versión a la vista para saber qué compilación está instalada
                        Text(
                            text = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    MenuOpciones(
                        estado = estadoMotor,
                        actualizando = actualizando,
                        sesiones = sesiones,
                        onActualizar = viewModel::actualizarMotor,
                        onIniciarSesion = iniciarSesion,
                        onCerrarSesion = viewModel::cerrarSesion,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Barra fina mientras el motor arranca o se actualiza
            if (estadoMotor is EstadoMotor.Iniciando || actualizando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                val dias = diasMotor
                if (dias != null && dias > InicioViewModel.DIAS_MOTOR_ANTIGUO) {
                    AvisoMotorAntiguo(dias, onActualizar = { viewModel.actualizarMotor() })
                }
            }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.inicio_url_etiqueta)) },
                supportingText = { Text(stringResource(R.string.inicio_url_ayuda)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { empezarDescarga() }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectorCalidad(calidad, onElegir = viewModel::elegirCalidad)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = empezarDescarga,
                    enabled = url.isNotBlank() && estadoMotor is EstadoMotor.Listo,
                ) {
                    Text(stringResource(R.string.inicio_descargar))
                }
            }

            if (descargas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.inicio_lista_vacia),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(descargas, key = { it.id }) { descarga ->
                        FilaDescarga(
                            descarga = descarga,
                            actualizandoMotor = actualizando,
                            onCancelar = { viewModel.cancelar(descarga.id) },
                            onActualizarYReintentar = { viewModel.actualizarMotor(reintentar = descarga) },
                            onIniciarSesion = iniciarSesion,
                        )
                    }
                }
            }
        }
    }
}

// Botón con la calidad actual que despliega las opciones
@Composable
private fun SelectorCalidad(calidad: Calidad, onElegir: (Calidad) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { abierto = true }) {
            Text(stringResource(calidad.etiqueta) + " ▾")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            for (opcion in Calidad.entries) {
                DropdownMenuItem(
                    text = { Text(stringResource(opcion.etiqueta)) },
                    onClick = {
                        abierto = false
                        onElegir(opcion)
                    },
                )
            }
        }
    }
}

// Línea discreta cuando yt-dlp tiene más de un mes: las descargas empiezan a fallar
@Composable
private fun AvisoMotorAntiguo(dias: Int, onActualizar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.motor_antiguo, dias, dias),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onActualizar) { Text(stringResource(R.string.actualizar)) }
    }
}

// Menú ⋮: versión de yt-dlp, actualizarlo y sesiones de Instagram/X
@Composable
private fun MenuOpciones(
    estado: EstadoMotor,
    actualizando: Boolean,
    sesiones: Set<Plataforma>,
    onActualizar: () -> Unit,
    onIniciarSesion: (Plataforma) -> Unit,
    onCerrarSesion: (Plataforma) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }

    IconButton(onClick = { abierto = true }) {
        Icon(
            painter = painterResource(R.drawable.ic_mas_opciones),
            contentDescription = stringResource(R.string.menu_opciones),
        )
    }
    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
        val textoVersion = when (estado) {
            EstadoMotor.Iniciando -> stringResource(R.string.motor_iniciando)
            is EstadoMotor.Listo -> stringResource(
                R.string.motor_version,
                estado.version ?: stringResource(R.string.motor_version_desconocida),
            )
            is EstadoMotor.Error -> stringResource(R.string.motor_error)
        }
        DropdownMenuItem(text = { Text(textoVersion) }, onClick = {}, enabled = false)
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (actualizando) R.string.motor_actualizando else R.string.motor_actualizar
                    )
                )
            },
            onClick = {
                abierto = false
                onActualizar()
            },
            enabled = estado is EstadoMotor.Listo && !actualizando,
        )
        HorizontalDivider()
        for (plataforma in Plataforma.entries) {
            val conSesion = plataforma in sesiones
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (conSesion) R.string.sesion_cerrar else R.string.sesion_iniciar,
                            plataforma.nombre,
                        )
                    )
                },
                onClick = {
                    abierto = false
                    if (conSesion) onCerrarSesion(plataforma) else onIniciarSesion(plataforma)
                },
            )
        }
    }
}
