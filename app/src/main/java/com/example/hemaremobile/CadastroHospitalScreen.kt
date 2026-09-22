package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.ui.theme.HemareVermelho

private val ESTADOS = listOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
    "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
)

private fun mascaraCnpj(valor: String): String {
    val digitos = valor.filter { it.isDigit() }.take(14)
    val sb = StringBuilder()
    digitos.forEachIndexed { indice, digito ->
        when (indice) {
            2, 5 -> sb.append('.')
            8 -> sb.append('/')
            12 -> sb.append('-')
        }
        sb.append(digito)
    }
    return sb.toString()
}

private fun cnpjValido(valor: String): Boolean {
    val cnpj = valor.filter { it.isDigit() }
    if (cnpj.length != 14 || cnpj.all { it == cnpj[0] }) return false

    fun calcularDigito(base: Int): Int {
        var soma = 0
        var pos = base - 7
        for (i in base downTo 1) {
            soma += (cnpj[base - i] - '0') * pos
            pos--
            if (pos < 2) pos = 9
        }
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }

    return calcularDigito(12) == (cnpj[12] - '0') && calcularDigito(13) == (cnpj[13] - '0')
}

private fun mascaraCep(valor: String): String {
    val digitos = valor.filter { it.isDigit() }.take(8)
    return if (digitos.length > 5) "${digitos.substring(0, 5)}-${digitos.substring(5)}" else digitos
}

@Composable
fun CadastroHospitalScreen(
    viewModel: AutenticacaoViewModel,
    onVoltarParaLogin: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var cnpj by remember { mutableStateOf("") }
    var cnes by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var cep by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    fun cadastrar() {
        when {
            nome.isBlank() || cnpj.isBlank() || cnes.isBlank() || email.isBlank() || senha.isBlank() ||
                cep.isBlank() || endereco.isBlank() || numero.isBlank() || bairro.isBlank() ||
                cidade.isBlank() || estado.isBlank() -> {
                mensagem = "❌ Preencha todos os campos obrigatórios."
            }
            !cnpjValido(cnpj) -> {
                mensagem = "❌ CNPJ inválido."
            }
            senha.length < 8 -> {
                mensagem = "❌ A senha deve ter pelo menos 8 caracteres."
            }
            else -> {
                mensagem = ""
                viewModel.cadastrarHospital(nome, email, senha)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "🏥", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cadastrar hospital",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cadastre sua instituição e encontre doadores.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        item { CampoAuth(valor = nome, aoAlterar = { nome = it }, rotulo = "Nome do hospital") }
        item {
            CampoAuth(
                valor = cnpj,
                aoAlterar = { cnpj = mascaraCnpj(it) },
                rotulo = "CNPJ",
                tipoTeclado = KeyboardType.Number
            )
        }
        item {
            CampoAuth(
                valor = cnes,
                aoAlterar = { cnes = it.filter { c -> c.isDigit() }.take(7) },
                rotulo = "CNES",
                tipoTeclado = KeyboardType.Number
            )
        }
        item {
            CampoAuth(
                valor = email,
                aoAlterar = { email = it },
                rotulo = "Email institucional",
                tipoTeclado = KeyboardType.Email
            )
        }
        item {
            CampoAuth(
                valor = senha,
                aoAlterar = { senha = it },
                rotulo = "Senha (mín. 8 caracteres)",
                ehSenha = true
            )
        }
        item {
            CampoAuth(
                valor = cep,
                aoAlterar = { cep = mascaraCep(it) },
                rotulo = "CEP",
                tipoTeclado = KeyboardType.Number
            )
        }
        item { CampoAuth(valor = endereco, aoAlterar = { endereco = it }, rotulo = "Endereço (rua)") }
        item { CampoAuth(valor = numero, aoAlterar = { numero = it }, rotulo = "Número", tipoTeclado = KeyboardType.Number) }
        item { CampoAuth(valor = bairro, aoAlterar = { bairro = it }, rotulo = "Bairro") }
        item { CampoAuth(valor = complemento, aoAlterar = { complemento = it }, rotulo = "Complemento (opcional)") }
        item { CampoAuth(valor = cidade, aoAlterar = { cidade = it }, rotulo = "Cidade") }
        item { CampoEstado(valor = estado, aoSelecionar = { estado = it }) }

        item {
            Button(
                onClick = { cadastrar() },
                colors = ButtonDefaults.buttonColors(containerColor = HemareVermelho),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cadastrar hospital")
            }
        }

        if (mensagem.isNotEmpty()) {
            item {
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HemareVermelho,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onVoltarParaLogin) {
                    Text("Voltar para o login")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoEstado(valor: String, aoSelecionar: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Estado (UF)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HemareVermelho,
                cursorColor = HemareVermelho
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            ESTADOS.forEach { uf ->
                DropdownMenuItem(
                    text = { Text(uf) },
                    onClick = {
                        aoSelecionar(uf)
                        expandido = false
                    }
                )
            }
        }
    }
}
