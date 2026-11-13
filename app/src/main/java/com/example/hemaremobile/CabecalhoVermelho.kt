package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.ui.theme.HemareVermelho

/** Ação de "voltar" da tela atual. É null nas telas das abas (que não têm para onde voltar). A navegação preenche. */
val LocalVoltar = compositionLocalOf<(() -> Unit)?> { null }

/** Cabeçalho vermelho fixo no topo das telas, com o botão Voltar quando a tela foi aberta por cima de outra. */
@Composable
fun CabecalhoVermelho(titulo: String) {
    val voltar = LocalVoltar.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HemareVermelho)
            .statusBarsPadding()
            .padding(horizontal = if (voltar != null) 8.dp else 24.dp, vertical = if (voltar != null) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (voltar != null) {
            IconButton(onClick = voltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
        }
        Text(
            text = titulo,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = if (voltar != null) 4.dp else 0.dp)
        )
    }
}
