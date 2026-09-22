package com.example.hemaremobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TipoConta { DOADOR, HOSPITAL }

data class ContaUsuario(
    val nome: String,
    val email: String,
    val senha: String,
    val tipo: TipoConta
)

data class AutenticacaoUiState(
    val contaLogada: ContaUsuario? = null,
    val erro: String = ""
)

/**
 * Presentation Layer: sessão e contas guardadas só em memória (mock), sem chamadas
 * reais ao backend nem persistência — isso é responsabilidade da Data layer futura.
 * Já vem com 2 contas de exemplo para testar o login sem precisar cadastrar antes.
 */
class AutenticacaoViewModel : ViewModel() {

    private val contasCadastradas = mutableListOf(
        ContaUsuario("Maria Doadora", "doador@hemare.com", "doador123", TipoConta.DOADOR),
        ContaUsuario("Hospital São Lucas", "hospital@hemare.com", "hospital123", TipoConta.HOSPITAL)
    )

    private val _uiState = MutableStateFlow(AutenticacaoUiState())
    val uiState: StateFlow<AutenticacaoUiState> = _uiState.asStateFlow()

    fun entrar(email: String, senha: String) {
        if (email.isBlank() || senha.isBlank()) {
            _uiState.update { it.copy(erro = "❌ Preencha email e senha.") }
            return
        }
        val conta = contasCadastradas.find {
            it.email.equals(email.trim(), ignoreCase = true) && it.senha == senha
        }
        if (conta == null) {
            _uiState.update { it.copy(erro = "❌ Email ou senha inválidos.") }
        } else {
            _uiState.value = AutenticacaoUiState(contaLogada = conta)
        }
    }

    fun cadastrarDoador(nome: String, email: String, senha: String) {
        val conta = ContaUsuario(nome, email.trim(), senha, TipoConta.DOADOR)
        contasCadastradas.removeAll { it.email.equals(conta.email, ignoreCase = true) }
        contasCadastradas.add(conta)
        _uiState.value = AutenticacaoUiState(contaLogada = conta)
    }

    fun cadastrarHospital(nome: String, email: String, senha: String) {
        val conta = ContaUsuario(nome, email.trim(), senha, TipoConta.HOSPITAL)
        contasCadastradas.removeAll { it.email.equals(conta.email, ignoreCase = true) }
        contasCadastradas.add(conta)
        _uiState.value = AutenticacaoUiState(contaLogada = conta)
    }

    fun sair() {
        _uiState.value = AutenticacaoUiState()
    }

    fun limparErro() {
        if (_uiState.value.erro.isNotEmpty()) {
            _uiState.update { it.copy(erro = "") }
        }
    }
}
