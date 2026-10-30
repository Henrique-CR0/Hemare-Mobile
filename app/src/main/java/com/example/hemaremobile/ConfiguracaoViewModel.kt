package com.example.hemaremobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConfiguracaoUiState(
    val temaEscuro: Boolean = true,
    val notificacoesAtivas: Boolean = true,
    val altoContraste: Boolean = false,
    val librasAtivo: Boolean = false,
    /** Tamanho do texto: 0,9 a 1,3 vezes o tamanho do sistema (como o A− / A+ do site). */
    val escalaFonte: Float = 1f
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

    fun alternarAltoContraste(ativo: Boolean) {
        _uiState.update { it.copy(altoContraste = ativo) }
    }

    fun alternarLibras(ativo: Boolean) {
        _uiState.update { it.copy(librasAtivo = ativo) }
    }

    fun mudarFonte(passo: Int) {
        _uiState.update {
            val nova = Math.round((it.escalaFonte + passo * 0.1f) * 10f) / 10f
            it.copy(escalaFonte = nova.coerceIn(0.9f, 1.3f))
        }
    }

    fun restaurarFonte() {
        _uiState.update { it.copy(escalaFonte = 1f) }
    }
}
