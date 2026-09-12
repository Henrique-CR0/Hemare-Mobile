package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConfiguracaoScreen(viewModel: ConfiguracaoViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Configuração",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            SecaoConfig(titulo = "Preferências") {
                ItemSwitch(
                    titulo = "Tema escuro",
                    descricao = "Ativa o modo escuro em todo o app.",
                    checked = state.temaEscuro,
                    onCheckedChange = viewModel::alternarTemaEscuro
                )
                ItemSwitch(
                    titulo = "Notificações",
                    descricao = "Avisos sobre campanhas e urgências de doação.",
                    checked = state.notificacoesAtivas,
                    onCheckedChange = viewModel::alternarNotificacoes
                )
            }
        }

        item {
            SecaoConfig(titulo = "Sobre o app") {
                ItemTexto("Versão", "0.1.0 (presentation layer)")
                ItemTexto(
                    "Missão do Hemare",
                    "Conectar doadores de sangue a hemocentros e hospitais, incentivando a doação voluntária e regular."
                )
                ItemTexto(
                    "Fontes oficiais",
                    "Ministério da Saúde, Fundação Hemope e Fundação Pró-Sangue."
                )
                ItemTexto("Autor", "Henrique Carneiro — github.com/Henrique-CR0")
            }
        }

        item {
            SecaoConfig(titulo = "Outros") {
                ItemTexto("Política de privacidade", "Em breve")
                ItemTexto("Termos de uso", "Em breve")
                ItemTexto("Fale conosco", "contato@hemare.app")
            }
        }
    }
}

@Composable
private fun SecaoConfig(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(4.dp), content = conteudo)
        }
    }
}

@Composable
private fun ItemSwitch(
    titulo: String,
    descricao: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ItemTexto(titulo: String, valor: String) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
