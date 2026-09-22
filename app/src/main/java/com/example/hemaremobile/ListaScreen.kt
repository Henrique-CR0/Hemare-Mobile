package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.navigation.RotasLista
import com.example.hemaremobile.ui.theme.HemareRosaClaro
import com.example.hemaremobile.ui.theme.HemareVermelho

private data class ItemLista(
    val rota: String,
    val icone: ImageVector,
    val titulo: String,
    val descricao: String
)

private val itensLista = listOf(
    ItemLista(RotasLista.POSSO_DOAR, Icons.Filled.HealthAndSafety, "Posso doar?", "Faça a triagem rápida"),
    ItemLista(RotasLista.ONDE_DOAR, Icons.Filled.LocationOn, "Onde doar", "Hemocentros perto de você"),
    ItemLista(RotasLista.GUIA, Icons.Filled.Checklist, "Guia de doação", "Antes, durante e depois"),
    ItemLista(RotasLista.MITOS, Icons.Filled.Lightbulb, "Mitos e verdades", "Tire suas dúvidas")
)

@Composable
fun ListaScreen(onItemClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CabecalhoVermelho(titulo = "Aprenda sobre doação")
        }

        items(itensLista) { item ->
            Card(
                onClick = { onItemClick(item.rota) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                LinhaItemLista(item)
            }
        }
    }
}

@Composable
private fun LinhaItemLista(item: ItemLista) {
    Row(
        modifier = Modifier
            .padding(14.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HemareRosaClaro,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = item.icone,
                    contentDescription = null,
                    tint = HemareVermelho,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
