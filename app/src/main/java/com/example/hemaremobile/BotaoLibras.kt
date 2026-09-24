package com.example.hemaremobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.ui.theme.HemareVermelho

/**
 * Botão flutuante de Libras, igual em posição/função ao avatar VLibras do site
 * (ativado em Configuração). A tradução em si ainda não está embutida no app —
 * por enquanto o botão explica isso, reservando o espaço para a integração futura.
 */
@Composable
fun BotaoLibrasFlutuante(modifier: Modifier = Modifier) {
    var mostrarInfo by remember { mutableStateOf(false) }

    Surface(
        onClick = { mostrarInfo = true },
        shape = CircleShape,
        color = HemareVermelho,
        shadowElevation = 6.dp,
        modifier = modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Accessibility,
                contentDescription = "Libras",
                tint = Color.White
            )
        }
    }

    if (mostrarInfo) {
        AlertDialog(
            onDismissRequest = { mostrarInfo = false },
            confirmButton = {
                TextButton(onClick = { mostrarInfo = false }) {
                    Text("Entendi")
                }
            },
            title = { Text("Tradução em Libras") },
            text = {
                Text(
                    "No site do Hemare, este botão abre o avatar VLibras, que traduz o " +
                        "conteúdo da tela para Língua Brasileira de Sinais. Essa tradução " +
                        "ainda não está embutida no app — por enquanto, a opção reserva o " +
                        "espaço para quando essa integração for feita."
                )
            }
        )
    }
}
