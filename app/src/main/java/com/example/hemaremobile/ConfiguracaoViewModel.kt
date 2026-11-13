package com.example.hemaremobile

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConfiguracaoUiState(
    val temaEscuro: Boolean = true,
    val altoContraste: Boolean = false,
    val librasAtivo: Boolean = false,
    /** Tamanho do texto: 0,9 a 1,3 vezes o tamanho do sistema (como o A− / A+ do site). */
    val escalaFonte: Float = 1f
)

/**
 * Preferências de aparência e acessibilidade. Ficam guardadas no aparelho (SharedPreferences): quem escolheu
 * tema claro, alto contraste ou texto maior não precisa escolher de novo a cada vez que abre o app.
 * O tema escuro é o padrão (visual do Hemare).
 */
class ConfiguracaoViewModel(aplicacao: Application) : AndroidViewModel(aplicacao) {

    private val prefs: SharedPreferences = aplicacao.getSharedPreferences("configuracao", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ConfiguracaoUiState(
            temaEscuro = prefs.getBoolean(TEMA_ESCURO, true),
            altoContraste = prefs.getBoolean(ALTO_CONTRASTE, false),
            librasAtivo = prefs.getBoolean(LIBRAS, false),
            escalaFonte = prefs.getFloat(ESCALA_FONTE, 1f).coerceIn(0.9f, 1.3f)
        )
    )
    val uiState: StateFlow<ConfiguracaoUiState> = _uiState.asStateFlow()

    private fun mudar(grava: SharedPreferences.Editor.() -> Unit, novo: (ConfiguracaoUiState) -> ConfiguracaoUiState) {
        _uiState.update(novo)
        prefs.edit().apply(grava).apply()
    }

    fun alternarTemaEscuro(ativo: Boolean) = mudar({ putBoolean(TEMA_ESCURO, ativo) }) { it.copy(temaEscuro = ativo) }

    fun alternarAltoContraste(ativo: Boolean) = mudar({ putBoolean(ALTO_CONTRASTE, ativo) }) { it.copy(altoContraste = ativo) }

    fun alternarLibras(ativo: Boolean) = mudar({ putBoolean(LIBRAS, ativo) }) { it.copy(librasAtivo = ativo) }

    fun mudarFonte(passo: Int) {
        val nova = (Math.round((_uiState.value.escalaFonte + passo * 0.1f) * 10f) / 10f).coerceIn(0.9f, 1.3f)
        mudar({ putFloat(ESCALA_FONTE, nova) }) { it.copy(escalaFonte = nova) }
    }

    fun restaurarFonte() = mudar({ putFloat(ESCALA_FONTE, 1f) }) { it.copy(escalaFonte = 1f) }

    private companion object {
        const val TEMA_ESCURO = "tema_escuro"
        const val ALTO_CONTRASTE = "alto_contraste"
        const val LIBRAS = "libras"
        const val ESCALA_FONTE = "escala_fonte"
    }
}
