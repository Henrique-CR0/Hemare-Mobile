package com.example.hemaremobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PerguntaSimNao(val campo: String, val texto: String)

/** Presentation Layer: campos e perguntas adaptados de frontend/src/paginas/Triagem.jsx do site Hemare. */
val perguntasSituacoesRecentes = listOf(
    PerguntaSimNao("tatuagemRecente", "Fez tatuagem ou micropigmentação nos últimos 12 meses?"),
    PerguntaSimNao("gripeResfriado", "Está com gripe ou resfriado (ou teve há poucos dias)?"),
    PerguntaSimNao("bebidaAlcoolica", "Ingeriu bebida alcoólica nas últimas 12 horas?"),
    PerguntaSimNao("gravidezOuPosParto", "Está grávida ou teve parto recentemente?")
)

val perguntasSaude = listOf(
    PerguntaSimNao("temHIV", "Você tem HIV/AIDS?"),
    PerguntaSimNao("temHepatiteB", "Você tem Hepatite B?"),
    PerguntaSimNao("temHepatiteC", "Você tem Hepatite C?"),
    PerguntaSimNao("temHTLV", "Você tem HTLV?"),
    PerguntaSimNao("temChagas", "Você tem Doença de Chagas?"),
    PerguntaSimNao("hepatiteAposOnzeAnos", "Teve hepatite após os 11 anos de idade?"),
    PerguntaSimNao("usaDrogasInjetaveis", "Faz uso de drogas injetáveis?")
)

val perguntasAtencao = listOf(
    PerguntaSimNao("temDiabetes", "Você tem diabetes?"),
    PerguntaSimNao("temHipertensao", "Você tem hipertensão (pressão alta)?"),
    PerguntaSimNao("usaMedicacaoContinua", "Você usa algum medicamento controlado ou de uso contínuo?")
)

private const val IDADE_MIN = 16
private const val IDADE_MAX = 69
private const val PESO_MIN = 50

enum class NivelResultado { VERDE, AMARELO, VERMELHO }

data class ResultadoTriagem(val nivel: NivelResultado, val titulo: String, val motivos: List<String>)

data class PossoDoarUiState(
    val idadeTexto: String = "",
    val pesoTexto: String = "",
    val respostas: Map<String, Boolean> = emptyMap(),
    val resultado: ResultadoTriagem? = null,
    val erro: String = ""
) {
    val avisoIdade: String
        get() {
            val idade = idadeTexto.toIntOrNull() ?: return ""
            return if (idade > 0 && (idade < IDADE_MIN || idade > IDADE_MAX))
                "Idade para doação: $IDADE_MIN a $IDADE_MAX anos."
            else ""
        }

    val avisoPeso: String
        get() {
            val peso = pesoTexto.toIntOrNull() ?: return ""
            return if (peso > 0 && peso < PESO_MIN) "Peso mínimo para doação: $PESO_MIN kg." else ""
        }
}

/**
 * Presentation Layer: triagem com regras fixas (mock), adaptada de
 * frontend/src/regras/triagem.js do site Hemare. Não substitui a triagem clínica.
 */
class PossoDoarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PossoDoarUiState())
    val uiState: StateFlow<PossoDoarUiState> = _uiState.asStateFlow()

    fun alterarIdade(texto: String) {
        val valor = texto.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceAtMost(120)
        _uiState.update { it.copy(idadeTexto = valor?.toString() ?: "") }
    }

    fun alterarPeso(texto: String) {
        val valor = texto.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceAtMost(300)
        _uiState.update { it.copy(pesoTexto = valor?.toString() ?: "") }
    }

    fun responder(campo: String, resposta: Boolean) {
        _uiState.update { it.copy(respostas = it.respostas + (campo to resposta)) }
    }

    fun verResultado() {
        val estado = _uiState.value
        val idade = estado.idadeTexto.toIntOrNull()
        val peso = estado.pesoTexto.toIntOrNull()
        if (idade == null || peso == null) {
            _uiState.update {
                it.copy(resultado = null, erro = "⚠️ Preencha pelo menos sua idade e seu peso para ver o resultado.")
            }
            return
        }
        _uiState.update { it.copy(erro = "", resultado = avaliarTriagem(idade, peso, it.respostas)) }
    }

    fun refazer() {
        _uiState.value = PossoDoarUiState()
    }
}

private fun avaliarTriagem(idade: Int, peso: Int, r: Map<String, Boolean>): ResultadoTriagem {
    val impedimentos = mutableListOf<String>()
    val atencoes = mutableListOf<String>()

    if (r["temHIV"] == true) impedimentos += "Você marcou HIV/AIDS."
    if (r["temHepatiteB"] == true || r["temHepatiteC"] == true) impedimentos += "Você marcou Hepatite B ou C."
    if (r["temHTLV"] == true) impedimentos += "Você marcou HTLV."
    if (r["temChagas"] == true) impedimentos += "Você marcou Doença de Chagas."
    if (r["usaDrogasInjetaveis"] == true) impedimentos += "Você marcou uso de drogas injetáveis."
    if (r["hepatiteAposOnzeAnos"] == true) impedimentos += "Você marcou hepatite após os 11 anos de idade."

    if (peso > 0 && peso < PESO_MIN) atencoes += "Seu peso está abaixo de 50 kg, que é o mínimo para doar."
    if (idade > 0 && (idade < IDADE_MIN || idade > IDADE_MAX)) atencoes += "A idade para doar é de 16 a 69 anos."

    if (r["tatuagemRecente"] == true) atencoes += "Tatuagem/micropigmentação nos últimos 12 meses (1 ano) pede um tempo de espera."
    if (r["gripeResfriado"] == true) atencoes += "Gripe ou resfriado recente pede aguardar alguns dias."
    if (r["bebidaAlcoolica"] == true) atencoes += "Bebida alcoólica nas últimas 12 horas impede a doação hoje."
    if (r["gravidezOuPosParto"] == true) atencoes += "Gravidez ou pós-parto recente pede um período de espera."

    if (r["temDiabetes"] == true) atencoes += "Diabetes: se controlada, geralmente não impede — confirme na triagem."
    if (r["temHipertensao"] == true) atencoes += "Hipertensão: se controlada, geralmente não impede — confirme na triagem."
    if (r["usaMedicacaoContinua"] == true) {
        atencoes += "Você usa medicação contínua/controlada. Muitos remédios não impedem a doação, mas alguns pedem um tempo de espera. NUNCA pare um remédio por conta própria para doar — entre em contato com o hemocentro onde vai doar para confirmar."
    }

    return when {
        impedimentos.isNotEmpty() -> ResultadoTriagem(NivelResultado.VERMELHO, "Há um ponto importante a verificar", impedimentos)
        atencoes.isNotEmpty() -> ResultadoTriagem(NivelResultado.AMARELO, "Atenção: confirme alguns pontos no hemocentro", atencoes)
        else -> ResultadoTriagem(NivelResultado.VERDE, "Tudo indica que você pode doar!", emptyList())
    }
}
