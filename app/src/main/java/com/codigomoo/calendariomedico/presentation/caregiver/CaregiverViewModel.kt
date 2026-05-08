package com.codigomoo.calendariomedico.presentation.caregiver

import androidx.lifecycle.ViewModel
import com.codigomoo.calendariomedico.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class PinMode { SET, CONFIRM, ENTER, CHANGE_VERIFY }

data class PinUiState(
    val mode: PinMode = PinMode.ENTER,
    val title: String = "Modo Cuidador",
    val subtitle: String = "Ingresa tu PIN para continuar",
    val dotCount: Int = 0,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CaregiverViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    private var buffer = ""
    private var firstPin = ""

    fun init(inputMode: String) {
        buffer = ""
        firstPin = ""
        val mode = when {
            inputMode == "change" && pinManager.hasPin() -> PinMode.CHANGE_VERIFY
            inputMode == "setup" || inputMode == "set" || !pinManager.hasPin() -> PinMode.SET
            else -> PinMode.ENTER
        }
        applyMode(mode)
    }

    fun onDigit(digit: Int) {
        if (buffer.length >= 4) return
        if (_state.value.error != null) {
            buffer = ""
            _state.update { it.copy(dotCount = 0, error = null) }
        }
        buffer += digit.toString()
        _state.update { it.copy(dotCount = buffer.length) }
        if (buffer.length == 4) processPin()
    }

    fun onDelete() {
        if (buffer.isEmpty()) return
        buffer = buffer.dropLast(1)
        _state.update { it.copy(dotCount = buffer.length, error = null) }
    }

    private fun processPin() {
        when (_state.value.mode) {
            PinMode.CHANGE_VERIFY -> {
                if (pinManager.verifyPin(buffer)) {
                    buffer = ""
                    applyMode(PinMode.SET)
                } else {
                    buffer = ""
                    _state.update { it.copy(dotCount = 0, error = "PIN incorrecto") }
                }
            }
            PinMode.SET -> {
                firstPin = buffer
                buffer = ""
                applyMode(PinMode.CONFIRM)
            }
            PinMode.CONFIRM -> {
                if (buffer == firstPin) {
                    pinManager.setPin(buffer)
                    buffer = ""
                    firstPin = ""
                    _state.update { it.copy(dotCount = 0, isSuccess = true) }
                } else {
                    buffer = ""
                    firstPin = ""
                    applyMode(PinMode.SET)
                    _state.update { it.copy(error = "Los PINs no coinciden") }
                }
            }
            PinMode.ENTER -> {
                if (pinManager.verifyPin(buffer)) {
                    buffer = ""
                    _state.update { it.copy(dotCount = 0, isSuccess = true) }
                } else {
                    buffer = ""
                    _state.update { it.copy(dotCount = 0, error = "PIN incorrecto") }
                }
            }
        }
    }

    private fun applyMode(mode: PinMode) {
        val (title, subtitle) = when (mode) {
            PinMode.CHANGE_VERIFY -> "Cambiar PIN" to "Ingresa tu PIN actual"
            PinMode.SET -> "Crear PIN" to "Ingresa un PIN de 4 dígitos"
            PinMode.CONFIRM -> "Confirmar PIN" to "Repite el PIN para confirmar"
            PinMode.ENTER -> "Modo Cuidador" to "Ingresa tu PIN para continuar"
        }
        _state.update {
            it.copy(mode = mode, title = title, subtitle = subtitle, dotCount = 0, error = null)
        }
    }
}
