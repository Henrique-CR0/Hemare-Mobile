package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class Veredito { MITO, VERDADE, DEPENDE }

private data class Mito(val pergunta: String, val veredito: Veredito, val texto: String)

/** Presentation Layer: conteúdo fixo, adaptado de frontend/src/paginas/Mitos.jsx do site Hemare. */
private val mitos = listOf(
    Mito("Doar sangue engorda ou emagrece.", Veredito.MITO, "Não faz nem uma coisa nem outra. O líquido é reposto pelo corpo em cerca de 24 horas."),
    Mito("Mulher menstruada não pode doar.", Veredito.MITO, "Pode, sim! A perda menstrual já é prevista pelo corpo. Quem usa anticoncepcional ou DIU também está liberada."),
    Mito("Quem fez tatuagem recente pode doar normalmente.", Veredito.MITO, "Quem fez tatuagem, maquiagem definitiva ou micropigmentação há menos de 12 meses precisa aguardar esse período antes de doar."),
    Mito("Doar sangue vicia.", Veredito.MITO, "Não existe nenhuma dependência ligada ao ato de doar."),
    Mito("Preciso estar em jejum para doar.", Veredito.MITO, "É o contrário! Você deve estar alimentado. Doar em jejum aumenta o risco de passar mal."),
    Mito("Doar enfraquece o organismo ou deixa o sangue \"mais fraco\".", Veredito.MITO, "O corpo repõe o volume rapidamente — a coleta é menos de 10% do seu sangue. Não há enfraquecimento."),
    Mito("Quem toma remédio nunca pode doar.", Veredito.MITO, "Depende do remédio. Muitos não impedem (como anticoncepcional). Antibióticos e anti-inflamatórios pedem um tempo de espera. Informe sempre na triagem."),
    Mito("Quem tem pressão alta ou diabetes não pode doar.", Veredito.DEPENDE, "Se a hipertensão ou o diabetes estiverem controlados e sem complicações, geralmente é possível doar. A avaliação é feita na triagem."),
    Mito("Já tive dengue, nunca mais posso doar.", Veredito.MITO, "Falso. Após a recuperação e um período de espera, você volta a poder doar."),
    Mito("Quem já teve hepatite depois dos 11 anos não pode doar.", Veredito.VERDADE, "Segundo a legislação, quem teve hepatite viral após os 11 anos de idade fica impedido de doar."),
    Mito("Gripe ou febre não atrapalham a doação.", Veredito.MITO, "Quem está com gripe, resfriado ou febre deve aguardar a recuperação antes de doar."),
    Mito("Uma doação ajuda várias pessoas.", Veredito.VERDADE, "O sangue é separado em componentes — uma doação pode salvar até 4 vidas."),
    Mito("Todo tipo sanguíneo é bem-vindo.", Veredito.VERDADE, "Todos são importantes! O O– é o doador universal e o mais requisitado em emergências, mas os tipos mais comuns (O+ e A+) também são muito usados."),
    Mito("Pessoas com tatuagem antiga (mais de 1 ano) não podem doar.", Veredito.MITO, "Podem. O impedimento é só para tatuagens recentes (menos de 12 meses), não importa a quantidade."),
    Mito("Idosos não podem doar sangue.", Veredito.MITO, "É possível doar até os 69 anos, desde que a primeira doação tenha sido feita antes dos 60."),
    Mito("Posso doar quantas vezes eu quiser no ano.", Veredito.MITO, "Há um intervalo: homens a cada 60 dias (até 4x/ano) e mulheres a cada 90 dias (até 3x/ano), para o corpo repor o ferro.")
)

private const val AVISO_MITOS = "⚠️ Conteúdo baseado em informações do Ministério da Saúde e de hemocentros oficiais (Hemominas, Hemoce, Pró-Sangue). Em caso de dúvida sobre seu caso, consulte o hemocentro antes de doar."

private fun selo(veredito: Veredito): Pair<String, Color> = when (veredito) {
    Veredito.VERDADE -> "✅ Verdade" to Color(0xFF35C47A)
    Veredito.DEPENDE -> "⚠️ Depende" to Color(0xFFE0B000)
    Veredito.MITO -> "❌ Mito" to Color(0xFFC8102E)
}

@Composable
fun MitosScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CabecalhoVermelho(titulo = "Mitos e verdades")
        }

        item {
            Text(
                text = "Muita gente deixa de doar por causa de informações erradas. Veja o que é mito e o que é verdade.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        items(mitos) { item ->
            val (textoSelo, corSelo) = selo(item.veredito)
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
                        text = "“${item.pergunta}”",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = corSelo.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = textoSelo,
                            style = MaterialTheme.typography.labelLarge,
                            color = corSelo,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = item.texto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = AVISO_MITOS,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}
