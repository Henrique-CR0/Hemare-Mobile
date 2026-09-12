package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.ui.theme.HemareVermelho
import com.example.hemaremobile.ui.theme.HemareRosaClaro

@Composable
fun InicioScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HemareVermelho)
            .padding(24.dp)
    ) {
        Text(
            text = "Hemare",
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

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EstatisticaCard(
                emoji = "🩸",
                valor = "4 vidas",
                legenda = "por doação",
                modifier = Modifier.weight(1f)
            )
            EstatisticaCard(
                emoji = "⏱️",
                valor = "10 min",
                legenda = "de doação",
                modifier = Modifier.weight(1f)
            )
            EstatisticaCard(
                emoji = "❤️",
                valor = "1,6%",
                legenda = "da população doa",
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            IlustracaoGotas()
        }
    }
}

@Composable
fun EstatisticaCard(
    emoji: String,
    valor: String,
    legenda: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = valor,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = legenda,
            color = HemareRosaClaro,
            fontSize = 10.sp
        )
    }
}

@Composable
fun IlustracaoGotas(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        GotaDecorativa(tamanho = 160.dp, alpha = 0.10f)
        GotaDecorativa(
            tamanho = 90.dp,
            alpha = 0.18f,
            modifier = Modifier.offset(x = (-60).dp, y = 40.dp)
        )
        GotaDecorativa(
            tamanho = 60.dp,
            alpha = 0.22f,
            modifier = Modifier.offset(x = 55.dp, y = (-50).dp)
        )
    }
}

@Composable
fun GotaDecorativa(
    tamanho: Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .rotate(45f)
            .clip(
                RoundedCornerShape(
                    topStart = tamanho / 2,
                    topEnd = tamanho / 2,
                    bottomEnd = tamanho / 2,
                    bottomStart = 0.dp
                )
            )
            .background(Color.White.copy(alpha = alpha))
    )
}

@Preview(showBackground = true)
@Composable
fun InicioScreenPreview() {
    InicioScreen()
}
