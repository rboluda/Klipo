package es.rboluda.klippo.ui.inicio

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.rboluda.klippo.R
import es.rboluda.klippo.descarga.EstadoMotor

// Pantalla de inicio: campo de URL y lista de descargas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory)) {
    val estadoMotor by viewModel.estadoMotor.collectAsStateWithLifecycle()
    val actualizando by viewModel.actualizando.collectAsStateWithLifecycle()
    val descargas by viewModel.descargas.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf("") }
    var urlPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    // Tras responder al permiso (sí o no) se lanza la descarga igualmente
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        urlPendiente?.let(viewModel::descargar)
        urlPendiente = null
    }

    val empezarDescarga = {
        if (url.isNotBlank() && estadoMotor is EstadoMotor.Listo) {
            if (viewModel.debePedirPermisoNotificaciones()) {
                viewModel.marcarPermisoPedido()
                urlPendiente = url
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.descargar(url)
            }
            url = ""
        }
    }

    LaunchedEffect(Unit) {
        viewModel.avisos.collect { snackbar.showSnackbar(recursos.getString(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    MenuMotor(
                        estado = estadoMotor,
                        actualizando = actualizando,
                        onActualizar = viewModel::actualizarMotor,
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
            Button(
                onClick = empezarDescarga,
                enabled = url.isNotBlank() && estadoMotor is EstadoMotor.Listo,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.inicio_descargar))
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
                        FilaDescarga(descarga, onCancelar = { viewModel.cancelar(descarga.id) })
                    }
                }
            }
        }
    }
}

// Menú ⋮ con la versión de yt-dlp y la opción de actualizarlo
@Composable
private fun MenuMotor(
    estado: EstadoMotor,
    actualizando: Boolean,
    onActualizar: () -> Unit,
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
    }
}
