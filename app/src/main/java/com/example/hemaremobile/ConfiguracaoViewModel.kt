package com.example.hemaremobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConfiguracaoUiState(
    val temaEscuro: Boolean = true,
    val notificacoesAtivas: Boolean = true
)

/**
 * Presentation Layer: preferências guardadas só em memória (StateFlow),
 * sem persistência real — isso é responsabilidade da Data layer futura.
 * O tema escuro começa ativado (visual padrão do Hemare).
 */
class ConfiguracaoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracaoUiState())
    val uiState: StateFlow<ConfiguracaoUiState> = _uiState.asStateFlow()

    fun alternarTemaEscuro(ativo: Boolean) {
        _uiState.update { it.copy(temaEscuro = ativo) }
    }

    fun alternarNotificacoes(ativo: Boolean) {
        _uiState.update { it.copy(notificacoesAtivas = ativo) }
    }
}
