package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hemaremobile.ui.theme.HemareVerde
import com.example.hemaremobile.ui.theme.HemareVermelho

private val CorAmarelaHospital = Color(0xFFE0B000)
private val CorEmergencia = Color(0xFF7C4DFF)

private fun corUrgencia(urgencia: String): Color = when (urgencia) {
    "estavel" -> HemareVerde
    "alerta" -> CorAmarelaHospital
    "critico" -> HemareVermelho
    "emergencia" -> CorEmergencia
    else -> HemareVerde
}

@Composable
fun PainelHospitalScreen(
    nomeHospital: String,
    onSair: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PainelHospitalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CabecalhoVermelho(titulo = "Painel do Hospital")
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nomeHospital,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onSair) {
                    Text("Sair")
                }
            }
        }

        item {
            Text(
                text = "Gerencie seu estoque, publique necessidades e encontre doadores compatíveis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            CartaoEstoque(estoque = uiState.estoque, onDefinirNivel = viewModel::definirEstoque)
        }

        item {
            CartaoPublicarNecessidade(
                tipoSelecionado = uiState.tipoSelecionado,
                urgenciaSelecionada = uiState.urgenciaSelecionada,
                mensagem = uiState.mensagem,
                onSelecionarTipo = viewModel::selecionarTipo,
                onSelecionarUrgencia = viewModel::selecionarUrgencia,
                onPublicar = viewModel::publicar
            )
        }

        item {
            CartaoNecessidades(
                necessidades = uiState.necessidades,
                onVerMatch = viewModel::verMatch
            )
        }

        uiState.necessidadeEmMatch?.let { necessidade ->
            item {
                CartaoMatch(
                    necessidade = necessidade,
                    tiposCompativeis = viewModel.tiposCompativeis(necessidade.tipoSanguineo),
                    doadores = viewModel.doadoresCompativeis(necessidade.tipoSanguineo),
                    confirmados = uiState.confirmados,
                    onConfirmar = viewModel::confirmarDoacao,
                    onFechar = viewModel::fecharMatch
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoEstoque(
    estoque: Map<String, String>,
    onDefinirNivel: (String, String) -> Unit
) {
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
                text = "🌡️ Termômetro de estoque",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Toque no nível de cada tipo sanguíneo para atualizar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            TIPOS_SANGUINEOS.forEachIndexed { indice, tipo ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = tipo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = corUrgencia(estoque[tipo] ?: "estavel")
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NIVEIS_ESTOQUE.forEach { (valor, rotulo) ->
                        ChipNivel(
                            texto = rotulo,
                            selecionado = estoque[tipo] == valor,
                            onClick = { onDefinirNivel(tipo, valor) }
                        )
                    }
                }
                if (indice < TIPOS_SANGUINEOS.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ChipNivel(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val corFundo = if (selecionado) HemareVermelho else MaterialTheme.colorScheme.surfaceVariant
    val corTexto = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = corFundo
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = corTexto,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
        )
    }
}

@Composable
private fun CartaoPublicarNecessidade(
    tipoSelecionado: String,
    urgenciaSelecionada: String,
    mensagem: String,
    onSelecionarTipo: (String) -> Unit,
    onSelecionarUrgencia: (String) -> Unit,
    onPublicar: () -> Unit
) {
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
                text = "Publicar necessidade",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            SeletorDropdown(
                rotulo = "Tipo sanguíneo necessário",
                valor = tipoSelecionado,
                opcoes = TIPOS_SANGUINEOS.map { it to it },
                onSelecionar = onSelecionarTipo
            )
            Spacer(modifier = Modifier.height(12.dp))
            SeletorDropdown(
                rotulo = "Urgência",
                valor = urgenciaSelecionada,
                opcoes = URGENCIAS_DETALHADAS,
                onSelecionar = onSelecionarUrgencia
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onPublicar,
                colors = ButtonDefaults.buttonColors(containerColor = HemareVermelho),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Publicar")
            }
            if (mensagem.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mensagem.startsWith("✅")) HemareVerde else HemareVermelho
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorDropdown(
    rotulo: String,
    valor: String,
    opcoes: List<Pair<String, String>>,
    onSelecionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val rotuloAtual = opcoes.find { it.first == valor }?.second ?: ""

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = rotuloAtual,
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HemareVermelho,
                cursorColor = HemareVermelho
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opcoes.forEach { (valorOpcao, rotuloOpcao) ->
                DropdownMenuItem(
                    text = { Text(rotuloOpcao) },
                    onClick = {
                        onSelecionar(valorOpcao)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CartaoNecessidades(
    necessidades: List<Necessidade>,
    onVerMatch: (Necessidade) -> Unit
) {
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
                text = "Minhas necessidades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (necessidades.isEmpty()) {
                Text(
                    text = "Nenhuma necessidade publicada ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                necessidades.forEach { necessidade ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = necessidade.tipoSanguineo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = corUrgencia(necessidade.urgencia)
                            )
                            Text(
                                text = "  " + (NIVEIS_ESTOQUE.find { it.first == necessidade.urgencia }?.second ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { onVerMatch(necessidade) }) {
                            Text("Ver doadores")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoMatch(
    necessidade: Necessidade,
    tiposCompativeis: List<String>,
    doadores: List<DoadorCompativel>,
    confirmados: Set<Int>,
    onConfirmar: (Int) -> Unit,
    onFechar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Doadores compatíveis com ${necessidade.tipoSanguineo}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onFechar) {
                    Text("Fechar")
                }
            }
            Text(
                text = "Tipos que podem doar: ${tiposCompativeis.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (doadores.isEmpty()) {
                Text(
                    text = "Nenhum doador compatível cadastrado ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                doadores.forEach { doador ->
                    Spacer(modifier = Modifier.height(12.dp))
                    LinhaDoador(
                        doador = doador,
                        confirmado = doador.id in confirmados,
                        onConfirmar = { onConfirmar(doador.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDoador(doador: DoadorCompativel, confirmado: Boolean, onConfirmar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HemareVermelho.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = doador.tipoSanguineo,
                            color = HemareVermelho,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = doador.nome,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = doador.cidade,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (doador.identificado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = doador.telefone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (confirmado) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = HemareVerde)
                        Text(
                            text = " Confirmado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HemareVerde,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onConfirmar,
                        colors = ButtonDefaults.buttonColors(containerColor = HemareVerde),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar doação")
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Contato protegido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
