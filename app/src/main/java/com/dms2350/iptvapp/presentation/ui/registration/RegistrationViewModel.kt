package com.dms2350.iptvapp.presentation.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dms2350.iptvapp.data.local.UserPreferences
import com.dms2350.iptvapp.data.service.DeviceHeartbeatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val heartbeatService: DeviceHeartbeatService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun onCedulaChanged(cedula: String) {
        // Solo permitir números
        val filteredCedula = cedula.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(cedula = filteredCedula, cedulaError = null)
    }

    fun onRegister() {
        val name = _uiState.value.name.trim()
        val cedula = _uiState.value.cedula.trim()

        // Validaciones
        var hasError = false

        if (name.isEmpty()) {
            _uiState.value = _uiState.value.copy(nameError = "El nombre es requerido")
            hasError = true
        } else if (name.length < 3) {
            _uiState.value = _uiState.value.copy(nameError = "El nombre debe tener al menos 3 caracteres")
            hasError = true
        }

        if (cedula.isEmpty()) {
            _uiState.value = _uiState.value.copy(cedulaError = "La cédula es requerida")
            hasError = true
        } else if (cedula.length < 5) {
            _uiState.value = _uiState.value.copy(cedulaError = "La cédula debe tener al menos 5 dígitos")
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Guardar información del usuario
                userPreferences.saveUserInfo(name, cedula)

                Timber.i("REGISTRO: Usuario registrado - Nombre: $name, Cédula: $cedula")

                // Iniciar heartbeat AHORA que tenemos los datos del usuario
                Timber.i("HEARTBEAT: Iniciando servicio con datos del usuario")
                heartbeatService.startHeartbeat()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrationCompleted = true
                )
            } catch (e: Exception) {
                Timber.e(e, "REGISTRO: Error al guardar información del usuario")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            userPreferences.skipRegistration()
            Timber.i("REGISTRO: Usuario omitió el registro")

            // Iniciar heartbeat aunque el usuario omitió el registro (se enviará con "N/A")
            Timber.i("HEARTBEAT: Iniciando servicio con datos N/A (usuario omitió registro)")
            heartbeatService.startHeartbeat()

            _uiState.value = _uiState.value.copy(registrationCompleted = true)
        }
    }
}

data class RegistrationUiState(
    val name: String = "",
    val cedula: String = "",
    val nameError: String? = null,
    val cedulaError: String? = null,
    val isLoading: Boolean = false,
    val registrationCompleted: Boolean = false
)

