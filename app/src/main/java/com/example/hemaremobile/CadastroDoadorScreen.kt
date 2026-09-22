package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.ui.theme.HemareVerde
import com.example.hemaremobile.ui.theme.HemareVermelho

private val PADRAO_EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun forcaSenha(senha: String): Int {
    var forca = 0
    if (senha.length >= 8) forca++
    if (senha.any { it.isUpperCase() } && senha.any { it.isLowerCase() }) forca++
    if (senha.any { it.isDigit() } && senha.any { !it.isLetterOrDigit() }) forca++
    return forca
}

private val ROTULOS_FORCA = listOf("", "Fraca", "Média", "Forte")
private val CORES_FORCA = listOf(Color.Transparent, HemareVermelho, Color(0xFFE0B000), HemareVerde)

@Composable
fun CadastroDoadorScreen(
    viewModel: AutenticacaoViewModel,
    onVoltarParaLogin: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var avisoNome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailTocado by remember { mutableStateOf(false) }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val emailValido = email.isNotEmpty() && PADRAO_EMAIL.matches(email)
    val mostrarEmailErro = emailTocado && email.isNotEmpty() && !emailValido
    val forca = forcaSenha(senha)

    fun digitarNome(valor: String) {
        avisoNome = if (valor.any { it.isDigit() }) "O nome não pode conter números." else ""
        nome = valor.filterNot { it.isDigit() }
    }

    fun cadastrar() {
        when {
            nome.isBlank() || email.isBlank() || senha.isBlank() -> {
                mensagem = "❌ Preencha todos os campos."
            }
            !emailValido -> {
                emailTocado = true
                mensagem = "❌ Digite um email válido (ex: nome@email.com)."
            }
            senha.length < 8 -> {
                mensagem = "❌ A senha deve ter pelo menos 8 caracteres."
            }
            else -> {
                mensagem = ""
                viewModel.cadastrarDoador(nome, email, senha)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🩸", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Criar sua conta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Junte-se ao Hemare e ajude a salvar vidas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        CampoAuth(valor = nome, aoAlterar = ::digitarNome, rotulo = "Nome completo")
        if (avisoNome.isNotEmpty()) {
            AvisoCampo(avisoNome)
        }

        Spacer(modifier = Modifier.height(12.dp))
        CampoAuth(
            valor = email,
            aoAlterar = { email = it },
            rotulo = "Email",
            tipoTeclado = KeyboardType.Email
        )
        if (mostrarEmailErro) {
            AvisoCampo("Email inválido — verifique o \"@\" e o ponto.")
        }

        Spacer(modifier = Modifier.height(12.dp))
        CampoAuth(
            valor = senha,
            aoAlterar = { senha = it },
            rotulo = "Senha (mín. 8 caracteres)",
            ehSenha = true
        )
        if (senha.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            BarraForcaSenha(forca = forca)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { cadastrar() },
            colors = ButtonDefaults.buttonColors(containerColor = HemareVermelho),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cadastrar")
        }

        if (mensagem.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyMedium,
                color = HemareVermelho,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onVoltarParaLogin) {
            Text("Já tem conta? Entrar")
        }
    }
}

@Composable
private fun AvisoCampo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = HemareVermelho,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

@Composable
private fun BarraForcaSenha(forca: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { indice ->
            val ativo = indice < forca
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (ativo) CORES_FORCA[forca] else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Text(
            text = ROTULOS_FORCA[forca],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
