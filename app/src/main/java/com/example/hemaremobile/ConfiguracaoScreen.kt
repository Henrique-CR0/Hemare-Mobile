package com.example.hemaremobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.data.Estado
import com.example.hemaremobile.data.TipoConta
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.theme.HemareVermelhoAcao

@Composable
fun ConfiguracaoScreen(
    viewModel: ConfiguracaoViewModel,
    estado: Estado,
    onSair: () -> Unit,
    onEntrar: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val usuario = estado.usuario

    TelaLista("Configuração") {
        bloco {
            Rotulo("CONTA")
            Cartao {
                if (usuario == null) {
                    Titulo("Você não está logado")
                    Dica("Entre para agendar, ver seu histórico e cuidar do seu perfil.")
                    Espaco(8)
                    Botao("Entrar ou criar conta", onEntrar)
                } else {
                    Text(usuario.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Dica(usuario.email)
                    Dica(
                        when (usuario.tipo) {
                            TipoConta.DOADOR -> "Conta de doador"
                            TipoConta.HOSPITAL -> "Conta de hospital"
                            TipoConta.ADMIN -> "Conta de administrador"
                        }
                    )
                    Espaco(8)
                    BotaoVazado("Sair", onSair)
                }
            }
            Espaco(16)

            Rotulo("PREFERÊNCIAS")
            ItemSwitch(Icons.Filled.DarkMode, "Tema escuro", state.temaEscuro, viewModel::alternarTemaEscuro)
            Espaco(8)
            ItemSwitch(Icons.Filled.Notifications, "Notificações", state.notificacoesAtivas, viewModel::alternarNotificacoes)
            Espaco(8)
            ItemSwitch(Icons.Filled.Contrast, "Alto contraste", state.altoContraste, viewModel::alternarAltoContraste)
            Espaco(8)
            ItemSwitch(Icons.Filled.Accessibility, "Libras", state.librasAtivo, viewModel::alternarLibras)
            Espaco(16)

            Rotulo("TAMANHO DO TEXTO")
            Cartao {
                Text("${Math.round(state.escalaFonte * 100)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Espaco(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("A−", false, state.escalaFonte > 0.91f) { viewModel.mudarFonte(-1) }
                    Chip("Normal", state.escalaFonte == 1f) { viewModel.restaurarFonte() }
                    Chip("A+", false, state.escalaFonte < 1.29f) { viewModel.mudarFonte(1) }
                }
            }
            Espaco(16)

            Rotulo("SOBRE O APP")
            Cartao {
                LinhaValor("Versão", "2.0.0")
                Espaco(8)
                Text("Desenvolvido por", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Espaco(6)
                Desenvolvedor("Henrique Carneiro Ribeiro", "carneiro.ribeiro@novaroma.edu.br")
                Espaco(6)
                Desenvolvedor("Lucas Pereira Vietiez", "lucas.veietez@novaroma.edu.br")
            }
        }
    }
}

@Composable
private fun ItemSwitch(icone: ImageVector, titulo: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinhaValor(titulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Desenvolvedor(nome: String, email: String) {
    androidx.compose.foundation.layout.Column {
        Text(nome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Text(email, fontSize = 13.sp, color = HemareVermelhoAcao, modifier = Modifier.padding(top = 1.dp))
    }
}
