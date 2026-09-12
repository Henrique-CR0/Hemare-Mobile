package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.navigation.RotasLista
import com.example.hemaremobile.ui.theme.HemareRosaClaro
import com.example.hemaremobile.ui.theme.HemareVermelho

private data class AtalhoInicio(val emoji: String, val titulo: String, val descricao: String, val rota: String)

private val atalhos = listOf(
    AtalhoInicio("🗺️", "Onde doar", "Hemocentros e hospitais perto de você.", RotasLista.ONDE_DOAR),
    AtalhoInicio("📖", "Guia da doação", "Antes, durante e depois: como se preparar.", RotasLista.GUIA),
    AtalhoInicio("💬", "Mitos e verdades", "Dúvidas comuns, esclarecidas com fontes oficiais.", RotasLista.MITOS)
)

@Composable
fun InicioScreen(onAtalhoClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HemareVermelho)
                    .padding(24.dp)
            ) {
                Text(
                    text = "🩸 Hemare",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Sua doação pode salvar até 4 vidas",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Uma única doação pode salvar até quatro vidas. Doe sangue, doe esperança.",
                    color = HemareRosaClaro,
                    fontSize = 16.sp
                )
            }
        }

        items(atalhos) { atalho ->
            Card(
                onClick = { onAtalhoClick(atalho.rota) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(text = atalho.emoji, style = MaterialTheme.typography.titleLarge)
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = atalho.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = atalho.descricao,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioScreenPreview() {
    InicioScreen(onAtalhoClick = {})
}
