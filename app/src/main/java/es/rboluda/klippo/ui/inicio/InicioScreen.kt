package es.rboluda.klippo.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.rboluda.klippo.R
import es.rboluda.klippo.descarga.EstadoMotor

// Pantalla de inicio: campo de URL y lista de descargas.
// La descarga llega en el paso 3; de momento solo el menú del motor funciona.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory)) {
    val estadoMotor by viewModel.estadoMotor.collectAsStateWithLifecycle()
    val actualizando by viewModel.actualizando.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.avisos.collect { snackbar.showSnackbar(context.getString(it)) }
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
            )
            Button(
                onClick = { /* Paso 3: lanzar la descarga */ },
                enabled = url.isNotBlank() && estadoMotor is EstadoMotor.Listo,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.inicio_descargar))
            }

            // Lista de descargas (vacía hasta el paso 4)
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
