package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Hemocentro(
    val nome: String,
    val cidade: String,
    val estado: String,
    val endereco: String,
    val telefone: String
)

/**
 * Presentation Layer: lista fixa (mock) de hemocentros reais do Brasil.
 * A busca por proximidade/GPS real fica para quando a Domain/Data layer existir.
 */
private val hemocentros = listOf(
    Hemocentro("Fundação Hemope", "Recife", "PE", "Endereço de exemplo", "Central de atendimento"),
    Hemocentro("Fundação Pró-Sangue", "São Paulo", "SP", "Endereço de exemplo", "Central de atendimento"),
    Hemocentro("Hemominas", "Belo Horizonte", "MG", "Endereço de exemplo", "Central de atendimento"),
    Hemocentro("Hemoce", "Fortaleza", "CE", "Endereço de exemplo", "Central de atendimento"),
    Hemocentro("Hemorio", "Rio de Janeiro", "RJ", "Endereço de exemplo", "Central de atendimento")
)

@Composable
fun OndeDoarScreen() {
    var busca by remember { mutableStateOf("") }
    val filtrados = remember(busca) {
        if (busca.isBlank()) {
            hemocentros
        } else {
            hemocentros.filter {
                it.cidade.contains(busca, ignoreCase = true) ||
                    it.estado.contains(busca, ignoreCase = true) ||
                    it.nome.contains(busca, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CabecalhoVermelho(titulo = "Onde doar")
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "📍 Lista de exemplo — em breve conectada à localização real do doador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    placeholder = { Text("Buscar por cidade ou estado") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(filtrados) { hemocentro ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = hemocentro.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${hemocentro.cidade} - ${hemocentro.estado}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    LinhaInfo(icone = Icons.Filled.LocationOn, texto = hemocentro.endereco)
                    LinhaInfo(icone = Icons.Filled.Phone, texto = hemocentro.telefone)
                }
            }
        }

        if (filtrados.isEmpty()) {
            item {
                Text(
                    text = "Nenhum local encontrado para essa busca.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun LinhaInfo(icone: ImageVector, texto: String) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
