package com.example.hemaremobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val TIPOS_SANGUINEOS = listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")

val NIVEIS_ESTOQUE = listOf(
    "estavel" to "🟢 Estável",
    "alerta" to "🟡 Alerta",
    "critico" to "🔴 Crítico",
    "emergencia" to "⚫ Emergência"
)

val URGENCIAS_DETALHADAS = listOf(
    "estavel" to "🟢 Estável — reposição de rotina",
    "alerta" to "🟡 Alerta — estoque baixo",
    "critico" to "🔴 Crítico — poucos dias de estoque",
    "emergencia" to "⚫ Emergência — situação extrema"
)

data class Necessidade(val id: Int, val tipoSanguineo: String, val urgencia: String)

data class DoadorCompativel(
    val id: Int,
    val nome: String,
    val tipoSanguineo: String,
    val cidade: String,
    val telefone: String,
    val identificado: Boolean
)

/** Quem pode doar para cada tipo receptor (regra real de compatibilidade ABO/Rh). */
private val COMPATIBILIDADE = mapOf(
    "O+" to listOf("O+", "O-"),
    "O-" to listOf("O-"),
    "A+" to listOf("A+", "A-", "O+", "O-"),
    "A-" to listOf("A-", "O-"),
    "B+" to listOf("B+", "B-", "O+", "O-"),
    "B-" to listOf("B-", "O-"),
    "AB+" to listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"),
    "AB-" to listOf("A-", "B-", "O-", "AB-")
)

/** Presentation Layer: diretório de doadores fixo (mock), só para simular o match. */
private val DOADORES_MOCK = listOf(
    DoadorCompativel(1, "Ana Beatriz Souza", "O-", "São Paulo, SP", "(11) 91234-5678", identificado = true),
    DoadorCompativel(2, "Carlos Eduardo Lima", "O+", "São Paulo, SP", "", identificado = false),
    DoadorCompativel(3, "Fernanda Costa", "A+", "Campinas, SP", "(19) 99876-5432", identificado = true),
    DoadorCompativel(4, "João Pedro Alves", "A-", "São Paulo, SP", "", identificado = false),
    DoadorCompativel(5, "Mariana Oliveira", "B+", "Guarulhos, SP", "(11) 98765-4321", identificado = true),
    DoadorCompativel(6, "Rafael Santos", "B-", "São Paulo, SP", "", identificado = false),
    DoadorCompativel(7, "Beatriz Fernandes", "AB+", "São Paulo, SP", "(11) 97654-3210", identificado = true),
    DoadorCompativel(8, "Lucas Martins", "AB-", "Osasco, SP", "", identificado = false)
)

data class PainelHospitalUiState(
    val estoque: Map<String, String> = emptyMap(),
    val necessidades: List<Necessidade> = emptyList(),
    val tipoSelecionado: String = "",
    val urgenciaSelecionada: String = "alerta",
    val mensagem: String = "",
    val necessidadeEmMatch: Necessidade? = null,
    val confirmados: Set<Int> = emptySet()
)

/**
 * Presentation Layer: estoque, necessidades e match guardados só em memória (mock),
 * sem chamadas reais ao backend — isso é responsabilidade da Data layer futura.
 */
class PainelHospitalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PainelHospitalUiState())
    val uiState: StateFlow<PainelHospitalUiState> = _uiState.asStateFlow()

    private var proximoId = 1

    fun definirEstoque(tipo: String, nivel: String) {
        _uiState.update { it.copy(estoque = it.estoque + (tipo to nivel)) }
    }

    fun selecionarTipo(tipo: String) {
        _uiState.update { it.copy(tipoSelecionado = tipo) }
    }

    fun selecionarUrgencia(urgencia: String) {
        _uiState.update { it.copy(urgenciaSelecionada = urgencia) }
    }

    fun publicar() {
        val estado = _uiState.value
        if (estado.tipoSelecionado.isEmpty()) {
            _uiState.update { it.copy(mensagem = "❌ Escolha o tipo sanguíneo.") }
            return
        }
        val nova = Necessidade(proximoId++, estado.tipoSelecionado, estado.urgenciaSelecionada)
        _uiState.update {
            it.copy(
                necessidades = it.necessidades + nova,
                tipoSelecionado = "",
                mensagem = "✅ Necessidade publicada com sucesso."
            )
        }
    }

    fun verMatch(necessidade: Necessidade) {
        _uiState.update { it.copy(necessidadeEmMatch = necessidade) }
    }

    fun fecharMatch() {
        _uiState.update { it.copy(necessidadeEmMatch = null) }
    }

    fun confirmarDoacao(doadorId: Int) {
        _uiState.update { it.copy(confirmados = it.confirmados + doadorId) }
    }

    fun tiposCompativeis(tipoReceptor: String): List<String> = COMPATIBILIDADE[tipoReceptor] ?: emptyList()

    fun doadoresCompativeis(tipoReceptor: String): List<DoadorCompativel> {
        val tipos = tiposCompativeis(tipoReceptor)
        return DOADORES_MOCK.filter { it.tipoSanguineo in tipos }
    }
}
