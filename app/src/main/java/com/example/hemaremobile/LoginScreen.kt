package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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

@Composable
fun LoginScreen(
    viewModel: AutenticacaoViewModel,
    onIrParaCadastroDoador: () -> Unit,
    onIrParaCadastroHospital: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

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
            text = "Entrar no Hemare",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Bem-vindo(a) de volta! Acesse sua conta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        CampoAuth(
            valor = email,
            aoAlterar = { email = it; viewModel.limparErro() },
            rotulo = "Email",
            tipoTeclado = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(12.dp))
        CampoAuth(
            valor = senha,
            aoAlterar = { senha = it; viewModel.limparErro() },
            rotulo = "Senha",
            ehSenha = true
        )

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { viewModel.entrar(email, senha) },
            colors = ButtonDefaults.buttonColors(containerColor = HemareVermelho),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }

        if (uiState.erro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.erro,
                style = MaterialTheme.typography.bodyMedium,
                color = HemareVermelho,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onIrParaCadastroDoador) {
            Text("Não tem conta? Cadastre-se")
        }
        TextButton(onClick = onIrParaCadastroHospital) {
            Text("É um hospital ou hemocentro? Cadastre sua instituição")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Contas de teste — doador: doador@hemare.com / doador123 · hospital: hospital@hemare.com / hospital123",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
