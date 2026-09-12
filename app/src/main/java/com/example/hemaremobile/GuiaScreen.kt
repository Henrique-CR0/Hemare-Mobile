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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class TopicoGuia(val emoji: String, val titulo: String, val itens: List<String>)
private data class FaseGuia(val titulo: String, val topicos: List<TopicoGuia>)

/** Presentation Layer: conteúdo fixo, adaptado de frontend/src/paginas/Orientacoes.jsx do site Hemare. */
private val fasesGuia = listOf(
    FaseGuia(
        titulo = "Antes de doar",
        topicos = listOf(
            TopicoGuia(
                emoji = "🍽️",
                titulo = "Alimentação e preparo",
                itens = listOf(
                    "Alimente-se bem — nunca vá em jejum. Faça uma refeição leve algumas horas antes.",
                    "Reforce o ferro nos dias anteriores — carnes magras, feijão, lentilha, ovos e folhas verde-escuras.",
                    "Hidrate-se — beba bastante água no dia anterior e antes de doar.",
                    "Descanse — durma bem na última noite."
                )
            ),
            TopicoGuia(
                emoji = "🚫",
                titulo = "Evite no dia",
                itens = listOf(
                    "Alimentos gordurosos, frituras e fast-food.",
                    "Bebida alcoólica nas 12 horas anteriores.",
                    "Excesso de café."
                )
            )
        )
    ),
    FaseGuia(
        titulo = "No dia da doação",
        topicos = listOf(
            TopicoGuia(
                emoji = "⏱️",
                titulo = "Como funciona",
                itens = listOf(
                    "O processo completo (cadastro, triagem, coleta e lanche) leva em média 40 minutos.",
                    "A coleta em si dura só 5 a 15 minutos.",
                    "Leve um documento oficial com foto."
                )
            )
        )
    ),
    FaseGuia(
        titulo = "Depois de doar",
        topicos = listOf(
            TopicoGuia(
                emoji = "✅",
                titulo = "Cuidados imediatos",
                itens = listOf(
                    "Permaneça no hemocentro por 15 minutos e aceite o lanche oferecido.",
                    "Mantenha o curativo por pelo menos 4 horas.",
                    "Hidrate-se bem, especialmente nas primeiras 4 horas.",
                    "Não fume por cerca de 2 horas e evite álcool por 12 horas."
                )
            ),
            TopicoGuia(
                emoji = "🚗",
                titulo = "Dirigir e esforço físico",
                itens = listOf(
                    "Dirigir (carro): aguarde pelo menos 1 hora.",
                    "Esforço físico e academia: evite por 12 horas (idealmente 24h)."
                )
            ),
            TopicoGuia(
                emoji = "👷",
                titulo = "Profissões e esportes",
                itens = listOf(
                    "Motoristas de ônibus/caminhão e operadores de máquinas: aguardar 12 horas.",
                    "Atletas (ciclismo, natação, mergulho, competição): aguardar 24 horas."
                )
            ),
            TopicoGuia(
                emoji = "🚨",
                titulo = "Nos dias seguintes",
                itens = listOf(
                    "Se tiver febre, diarreia ou sintomas de infecção em até 7 a 14 dias, comunique o hemocentro."
                )
            )
        )
    )
)

private const val AVISO_GUIA = "⚠️ Informações orientativas, baseadas no Ministério da Saúde e em hemocentros oficiais. A avaliação final é feita por um profissional no dia da doação."

@Composable
fun GuiaScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CabecalhoVermelho(titulo = "Guia de doação")
        }

        fasesGuia.forEach { fase ->
            item {
                Text(
                    text = fase.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            items(fase.topicos) { topico ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Text(text = topico.emoji, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = topico.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        topico.itens.forEach { texto ->
                            Text(
                                text = "•  $texto",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = AVISO_GUIA,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}
