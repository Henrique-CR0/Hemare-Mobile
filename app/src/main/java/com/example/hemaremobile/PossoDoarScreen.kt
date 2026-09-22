package com.example.hemaremobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hemaremobile.ui.theme.HemareVerde
import com.example.hemaremobile.ui.theme.HemareVermelho

private val CorAmarela = Color(0xFFE0B000)

@Composable
fun PossoDoarScreen(viewModel: PossoDoarViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CabecalhoVermelho(titulo = "Posso doar?")
        }

        item {
            Text(
                text = "Preencha suas informações para receber uma orientação sobre a doação. Isso não substitui a triagem clínica feita no hemocentro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            GrupoSobreVoce(uiState = uiState, viewModel = viewModel)
        }

        item {
            GrupoPerguntas(
                titulo = "Situações recentes",
                perguntas = perguntasSituacoesRecentes,
                respostas = uiState.respostas,
                onResponder = viewModel::responder
            )
        }

        item {
            GrupoPerguntas(
                titulo = "Saúde",
                perguntas = perguntasSaude,
                respostas = uiState.respostas,
                onResponder = viewModel::responder
            )
        }

        item {
            GrupoPerguntas(
                titulo = "Condições a confirmar",
                perguntas = perguntasAtencao,
                respostas = uiState.respostas,
                onResponder = viewModel::responder
            )
        }

        item {
            Button(
                onClick = viewModel::verResultado,
                colors = ButtonDefaults.buttonColors(containerColor = HemareVermelho),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text("Ver resultado")
            }
        }

        if (uiState.erro.isNotEmpty()) {
            item {
                Text(
                    text = uiState.erro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HemareVermelho,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        uiState.resultado?.let { resultado ->
            item {
                CartaoResultado(resultado = resultado)
            }
            item {
                OutlinedButton(
                    onClick = viewModel::refazer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text("Refazer")
                }
            }
        }
    }
}

@Composable
private fun GrupoSobreVoce(uiState: PossoDoarUiState, viewModel: PossoDoarViewModel) {
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
                text = "Sobre você",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))
            CampoNumero(
                rotulo = "Sua idade",
                sufixo = "anos",
                valor = uiState.idadeTexto,
                aviso = uiState.avisoIdade,
                onValorAlterado = viewModel::alterarIdade
            )

            Spacer(modifier = Modifier.height(12.dp))
            CampoNumero(
                rotulo = "Seu peso (kg)",
                sufixo = "kg",
                valor = uiState.pesoTexto,
                aviso = uiState.avisoPeso,
                onValorAlterado = viewModel::alterarPeso
            )
        }
    }
}

@Composable
private fun CampoNumero(
    rotulo: String,
    sufixo: String,
    valor: String,
    aviso: String,
    onValorAlterado: (String) -> Unit
) {
    Column {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = valor,
            onValueChange = onValorAlterado,
            placeholder = { Text(sufixo) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HemareVermelho,
                cursorColor = HemareVermelho
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (aviso.isNotEmpty()) {
            Text(
                text = aviso,
                style = MaterialTheme.typography.bodySmall,
                color = CorAmarela,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun GrupoPerguntas(
    titulo: String,
    perguntas: List<PerguntaSimNao>,
    respostas: Map<String, Boolean>,
    onResponder: (String, Boolean) -> Unit
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
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            perguntas.forEachIndexed { indice, pergunta ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = pergunta.texto,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                SeletorSimNao(
                    respostaAtual = respostas[pergunta.campo],
                    onSelecionar = { resposta -> onResponder(pergunta.campo, resposta) }
                )
                if (indice < perguntas.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SeletorSimNao(respostaAtual: Boolean?, onSelecionar: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OpcaoResposta(
            texto = "Sim",
            selecionado = respostaAtual == true,
            onClick = { onSelecionar(true) }
        )
        OpcaoResposta(
            texto = "Não",
            selecionado = respostaAtual == false,
            onClick = { onSelecionar(false) }
        )
    }
}

@Composable
private fun OpcaoResposta(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    val corFundo = if (selecionado) HemareVermelho else MaterialTheme.colorScheme.surfaceVariant
    val corTexto = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = corFundo,
        modifier = Modifier.widthIn(min = 96.dp)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = corTexto,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        )
    }
}

@Composable
private fun CartaoResultado(resultado: ResultadoTriagem) {
    val (cor, icone) = when (resultado.nivel) {
        NivelResultado.VERDE -> HemareVerde to Icons.Filled.CheckCircle
        NivelResultado.AMARELO -> CorAmarela to Icons.Filled.WarningAmber
        NivelResultado.VERMELHO -> HemareVermelho to Icons.Filled.Cancel
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = resultado.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = cor
            )

            if (resultado.motivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                resultado.motivos.forEach { motivo ->
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•  ", color = cor, fontWeight = FontWeight.Bold)
                        Text(
                            text = motivo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "⚠️ Esta é uma orientação informativa, não substitui a triagem clínica. A avaliação final é feita por um profissional no dia da doação.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
