package com.example.hemaremobile.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.*
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.theme.HemareVerde
import kotlinx.coroutines.launch

private val FILTROS = listOf("pendente" to "⏳ Pendentes", "aprovado" to "✓ Aprovados", "recusado" to "❌ Recusados", "todos" to "Todos")

// ---------------------------------------------------------------------------------------------
// Admin: hospitais e notícias
// ---------------------------------------------------------------------------------------------

@Composable
fun AdminScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    var aba by remember { mutableStateOf("hospitais") }
    TelaLista("Administração") {
        item { AbasConta(TipoConta.ADMIN, Rotas.ADMIN, navegar) }
        item { SubAbas(listOf("hospitais" to "🏥 Hospitais", "noticias" to "📰 Notícias"), aba) { aba = it } }
        if (aba == "hospitais") bloco { VerificacaoHospitais(store, estado) } else bloco { ColetaNoticias(store, estado) }
    }
}

@Composable
private fun VerificacaoHospitais(store: HemareStore, estado: Estado) {
    var filtro by remember { mutableStateOf("pendente") }
    var recusando by remember { mutableStateOf<Int?>(null) }
    var motivo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    val lista = estado.hospitaisParaAdmin(filtro)

    Paragrafo("Confira o CNPJ e o CNES de cada hospital antes de aprovar. Só hospitais aprovados publicam necessidades, agenda e campanhas.", suave = true)
    Espaco(8)
    ChipsLinha { FILTROS.forEach { (v, r) -> Chip(r, filtro == v) { filtro = v; recusando = null } } }
    Mensagem(mensagem)
    Espaco(8)
    if (lista.isEmpty()) Vazio("Nenhum hospital neste filtro.")
    lista.forEach { h ->
        val u = estado.usuarioPorId(h.id)
        Cartao {
            Text(u?.nome ?: "Hospital", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Dica(u?.email ?: "")
            Espaco(4)
            when (h.status) {
                StatusVerificacao.APROVADO -> Selo("✓ Aprovado", HemareVerde)
                StatusVerificacao.PENDENTE -> Selo("⏳ Pendente", CorAviso)
                StatusVerificacao.RECUSADO -> Selo("❌ Recusado", CorAlerta)
            }
            Espaco(6)
            LinhaChave("CNPJ", h.cnpj)
            LinhaChave("CNES", h.cnes)
            LinhaChave("Endereço", "${h.endereco}, ${h.numero}${h.complemento?.let { " - $it" } ?: ""} · ${h.bairro}")
            LinhaChave("Cidade", "${h.cidade}/${h.estado} · CEP ${h.cep}")
            if (h.status == StatusVerificacao.RECUSADO) Dica("Motivo: ${h.motivoRecusa}")
            Espaco(6)
            if (recusando == h.id) {
                Campo(motivo, { motivo = it.take(500) }, "Motivo da recusa (o hospital vai ver)", linhas = 3, dica = "Mínimo de 10 caracteres.")
                Espaco(6)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Confirmar recusa", false) { val r = store.recusarHospital(h.id, motivo); mensagem = r.texto; if (r.ok) { recusando = null; motivo = "" } }
                    Chip("Cancelar", false) { recusando = null; motivo = "" }
                }
            } else {
                ChipsLinha {
                    if (h.status != StatusVerificacao.APROVADO) Chip("✔️ Aprovar", false) { mensagem = store.aprovarHospital(h.id).texto }
                    if (h.status != StatusVerificacao.RECUSADO) Chip(if (h.status == StatusVerificacao.APROVADO) "Revogar aprovação" else "Recusar", false) { recusando = h.id; motivo = "" }
                }
            }
        }
        Espaco(8)
    }
}

@Composable
private fun ColetaNoticias(store: HemareStore, estado: Estado) {
    val escopo = rememberCoroutineScope()
    var coletando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    val coleta = estado.coleta

    Paragrafo("O Hemare lê feeds RSS públicos e guarda só título, resumo curto e link. Quem lê a notícia vai para o site de origem.", suave = true)
    Espaco(8)
    Botao(if (coletando) "Coletando..." else "🔄 Coletar agora", {
        if (!coletando) escopo.launch { coletando = true; mensagem = store.coletarNoticias().texto; coletando = false }
    }, habilitado = !coletando)
    Mensagem(mensagem)
    Espaco(6)
    Dica("${estado.noticiasVisiveis().size} notícias no feed · ${estado.noticiasOcultas.size} escondidas por você." +
        (coleta?.let { " Última coleta: ${Datas.haQuanto(it.terminouEmMs)}." } ?: " Ainda não coletou neste aparelho."))
    Espaco(10)
    Titulo("Fontes")
    Noticias.FONTES.forEach { f ->
        val r = coleta?.fontes?.firstOrNull { it.id == f.id }
        Espaco(6)
        Cartao {
            Text(f.nome, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Dica(f.url)
            Dica("Licença/uso: ${f.licenca}" + (f.uf?.let { " · $it" } ?: ""))
            when {
                !f.ativa -> Selo("⏸️ Desativada", CorAviso)
                r == null -> Selo("Sem coleta ainda", MaterialTheme.colorScheme.onSurfaceVariant)
                r.ok -> Selo("✓ ${r.lidos} lidas · ${r.novos} novas", HemareVerde)
                else -> Selo("❌ ${r.erro ?: "falhou"}", CorAlerta)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Admin: status do desenvolvimento
// ---------------------------------------------------------------------------------------------

@Composable
fun AdminStatusScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val uso = estado.uso()
    TelaLista("Status do desenvolvimento") {
        item { AbasConta(TipoConta.ADMIN, Rotas.ADMIN_STATUS, navegar) }
        bloco {
            Cartao {
                Titulo("📈 Uso (neste aparelho)")
                Dica("Só números, nenhum dado de pessoa. Neste app os dados são de demonstração.")
                Espaco(8)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Numero("${uso.doadores}", "doadores")
                    Numero("${uso.hospitais}", "hospitais")
                    Numero("${uso.hospitaisAprovados}", "aprovados")
                }
                Espaco(8)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Numero("${uso.doacoesConfirmadas}", "doações")
                    Numero("${uso.agendados}", "agendados")
                    Numero("${uso.compareceram}", "compareceram")
                }
                Espaco(8)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Numero("${uso.novasNos7Dias}", "novas contas (7 dias)")
                    Numero("${uso.noticias}", "notícias")
                }
            }
            Espaco(12)
            Cartao {
                Titulo("🚦 Situação do projeto (relatório de 30/10/2026)")
                Dica("Resumo do docs/STATUS.md do site. O relatório completo fica no repositório.")
                Espaco(6)
                listOf(
                    "🟢 Funcionalidades" to "Tudo do roadmap pronto, menos a expansão de fontes de notícias.",
                    "🟢 Qualidade e testes" to "1.325 verificações no site, todas passando.",
                    "🟡 Segurança e privacidade" to "Falta tirar o token do localStorage e verificar o e-mail no cadastro.",
                    "🟡 Operação" to "Envio de e-mail ainda usa o remetente de teste.",
                    "🟡 Conteúdo e dados" to "Locais só de hemocentros públicos de capitais.",
                    "🟢 Documentação" to "API, arquitetura, acesso por tipo de conta e notícias."
                ).forEach { (a, b) -> Espaco(6); Text(a, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Dica(b) }
            }
            Espaco(12)
            Cartao {
                Titulo("📱 Sobre este app")
                Dica("O app mobile reproduz as telas e as regras do site. Ainda não conversa com um servidor: as contas e os dados ficam só na memória do aparelho (demonstração), e as notícias e o CEP são lidos da internet.")
            }
        }
    }
}
