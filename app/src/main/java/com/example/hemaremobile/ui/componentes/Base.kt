package com.example.hemaremobile.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.CabecalhoVermelho
import com.example.hemaremobile.ui.theme.HemareVerde

// Peças de interface compartilhadas por todas as telas do Hemare Mobile.

val CorAviso = Color(0xFFE0B000)
val CorAlerta = Color(0xFFE8112D)

// ---------------------------------------------------------------- estrutura

/**
 * Tela rolável com o cabeçalho vermelho no topo. Cada bloco usa `bloco { }` (já com a margem lateral).
 * `rodape` (opcional) fica fixo embaixo, sem rolar com o conteúdo: é o lugar dos botões de ação dos formulários.
 */
@Composable
fun TelaLista(titulo: String, rodape: (@Composable ColumnScope.() -> Unit)? = null, conteudo: LazyListScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { CabecalhoVermelho(titulo = titulo) }
            conteudo()
        }
        if (rodape != null) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), content = rodape)
            }
        }
    }
}

fun LazyListScope.bloco(conteudo: @Composable ColumnScope.() -> Unit) {
    item {
        Column(modifier = Modifier.padding(horizontal = 20.dp), content = conteudo)
    }
}

@Composable
fun Cartao(modifier: Modifier = Modifier, aoClicar: (() -> Unit)? = null, conteudo: @Composable ColumnScope.() -> Unit) {
    val cores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val forma = RoundedCornerShape(16.dp)
    val borda = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (aoClicar != null) {
        Card(onClick = aoClicar, shape = forma, colors = cores, border = borda, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = conteudo)
        }
    } else {
        Card(shape = forma, colors = cores, border = borda, modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), content = conteudo)
        }
    }
}

@Composable
fun Espaco(altura: Int = 12) {
    Spacer(modifier = Modifier.height(altura.dp))
}

// ---------------------------------------------------------------- textos

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
}

@Composable
fun Paragrafo(texto: String, modifier: Modifier = Modifier, suave: Boolean = false, centro: Boolean = false) {
    Text(
        texto, style = MaterialTheme.typography.bodyMedium,
        color = if (suave) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textAlign = if (centro) TextAlign.Center else TextAlign.Start, modifier = modifier
    )
}

@Composable
fun Dica(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun Rotulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Feedback de uma ação: verde para ✅, vermelho para ❌, neutro para o resto. */
@Composable
fun Mensagem(texto: String?, modifier: Modifier = Modifier) {
    if (texto.isNullOrBlank()) return
    val cor = when {
        texto.startsWith("✅") -> HemareVerde
        texto.startsWith("❌") || texto.startsWith("⚠️") -> CorAlerta
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = cor, modifier = modifier.padding(top = 8.dp))
}

@Composable
fun Aviso(texto: String, cor: Color = CorAviso, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = cor.copy(alpha = 0.14f), modifier = modifier.fillMaxWidth()) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(12.dp))
    }
}

@Composable
fun Vazio(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun Selo(texto: String, cor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = cor.copy(alpha = 0.16f)) {
        Text(texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = cor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun LinhaChave(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun Numero(valor: String, rotulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(rotulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun BarraProgresso(porcentagem: Int, cor: Color = MaterialTheme.colorScheme.primary) {
    val fracao = (porcentagem.coerceIn(0, 100)) / 100f
    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (fracao > 0f) {
            Box(modifier = Modifier.fillMaxWidth(fracao).height(10.dp).clip(RoundedCornerShape(5.dp)).background(cor))
        }
    }
}

// ---------------------------------------------------------------- botões

@Composable
fun Botao(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Button(
        onClick = aoClicar, enabled = habilitado, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        modifier = modifier.fillMaxWidth()
    ) { Text(texto, fontWeight = FontWeight.Bold) }
}

@Composable
fun BotaoVazado(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true, perigo: Boolean = false) {
    OutlinedButton(
        onClick = aoClicar, enabled = habilitado, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (perigo) CorAlerta else MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth()
    ) { Text(texto, color = if (perigo) CorAlerta else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun BotaoVerde(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Button(
        onClick = aoClicar, enabled = habilitado, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HemareVerde, contentColor = Color.White), modifier = modifier
    ) { Text(texto, fontWeight = FontWeight.Bold) }
}

@Composable
fun BotaoTexto(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = aoClicar, modifier = modifier) { Text(texto, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center) }
}

// ---------------------------------------------------------------- escolhas

@Composable
fun Chip(texto: String, selecionado: Boolean, habilitado: Boolean = true, aoClicar: () -> Unit) {
    Surface(
        onClick = aoClicar, enabled = habilitado, shape = RoundedCornerShape(20.dp),
        color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selecionado) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = if (selecionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = if (habilitado) 1f else 0.38f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsLinha(conteudo: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { conteudo() }
}

/** Abas dentro de uma tela (ex.: Dados e privacidade | Saúde e doação | Carteirinha | Segurança). */
@Composable
fun SubAbas(abas: List<Pair<String, String>>, atual: String, aoTrocar: (String) -> Unit) {
    val rolagem = rememberScrollState()
    Box {
        Row(
            modifier = Modifier.horizontalScroll(rolagem).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            abas.forEach { (id, rotulo) -> Chip(rotulo, atual == id) { aoTrocar(id) } }
        }
        // Dica visual: há mais abas à direita (antes elas só ficavam cortadas, sem indicar que dá para deslizar).
        if (rolagem.canScrollForward) {
            Box(
                Modifier.align(Alignment.CenterEnd).width(36.dp).height(40.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background)))
            )
        }
    }
}

@Composable
fun CaixaMarcar(texto: String, marcado: Boolean, aoMudar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = marcado, onCheckedChange = aoMudar,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, checkmarkColor = MaterialTheme.colorScheme.onPrimary)
        )
        Text(
            texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
fun OpcaoRadio(texto: String, selecionado: Boolean, aoClicar: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selecionado, onClick = aoClicar,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Text(
            texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

/** Pergunta com os botões Sim / Não (usada na triagem). */
@Composable
fun PerguntaSimNao(texto: String, resposta: Boolean?, aoResponder: (Boolean) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip("Sim", resposta == true) { aoResponder(true) }
            Chip("Não", resposta == false) { aoResponder(false) }
        }
    }
}

// ---------------------------------------------------------------- campos

@Composable
fun Campo(
    valor: String, aoMudar: (String) -> Unit, rotulo: String, modifier: Modifier = Modifier,
    teclado: KeyboardType = KeyboardType.Text, senha: Boolean = false, aviso: String = "", dica: String = "",
    linhas: Int = 1, habilitado: Boolean = true, placeholder: String = "",
    /** Se informado, o botão do teclado vira "Concluído" e chama esta ação (ex.: entrar ao digitar a senha). */
    aoConfirmar: (() -> Unit)? = null
) {
    var mostrar by remember { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = valor, onValueChange = aoMudar, label = { Text(rotulo) }, enabled = habilitado,
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        singleLine = linhas == 1, minLines = linhas, maxLines = if (linhas == 1) 1 else linhas + 2,
        isError = aviso.isNotEmpty(),
        visualTransformation = if (senha && !mostrar) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (senha) KeyboardType.Password else teclado,
            imeAction = when { aoConfirmar != null -> ImeAction.Done; linhas > 1 -> ImeAction.Default; else -> ImeAction.Next }
        ),
        keyboardActions = KeyboardActions(onDone = { foco.clearFocus(); aoConfirmar?.invoke() }),
        trailingIcon = if (senha) ({
            IconButton(onClick = { mostrar = !mostrar }) {
                Icon(if (mostrar) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (mostrar) "Ocultar senha" else "Mostrar senha")
            }
        }) else null,
        supportingText = when {
            aviso.isNotEmpty() -> ({ Text(aviso) })
            dica.isNotEmpty() -> ({ Text(dica) })
            else -> null
        },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth()
    )
}

/** Lista suspensa: opcoes = (valor, rótulo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoSelecao(rotulo: String, valor: String, opcoes: List<Pair<String, String>>, aoSelecionar: (String) -> Unit, modifier: Modifier = Modifier) {
    var aberto by remember { mutableStateOf(false) }
    val atual = opcoes.firstOrNull { it.first == valor }?.second ?: ""
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = atual, onValueChange = {}, readOnly = true, label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (v, r) ->
                DropdownMenuItem(text = { Text(r) }, onClick = { aoSelecionar(v); aberto = false })
            }
        }
    }
}

/** Pequeno emblema redondo com um emoji dentro (o "ícone em círculo" do app). */
@Composable
fun IconeEmoji(emoji: String, tamanho: Int = 44, cor: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(50), color = cor.copy(alpha = 0.16f), modifier = Modifier.size(tamanho.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Linha de ícone + título + descrição, clicável: itens de menu/lista. */
@Composable
fun ItemDeLista(emoji: String, titulo: String, descricao: String, trava: Boolean = false, aoClicar: () -> Unit) {
    Cartao(aoClicar = aoClicar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconeEmoji(emoji)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text((if (trava) "🔒 " else "") + titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
