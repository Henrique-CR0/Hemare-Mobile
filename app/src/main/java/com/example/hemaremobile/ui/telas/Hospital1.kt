package com.example.hemaremobile.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.*
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.compartilharTexto
import com.example.hemaremobile.ui.compartilharWhatsApp
import com.example.hemaremobile.ui.copiarTexto
import com.example.hemaremobile.ui.ligar
import com.example.hemaremobile.ui.theme.HemareVerde
import kotlinx.coroutines.launch
import java.time.LocalDate

private val DIAS_SEMANA = listOf(1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb", 0 to "Dom")

private fun statusNecessidade(s: String) = when (s) { "aberta" -> "🟢 Aberta"; "atendida" -> "✔️ Atendida"; else -> "✖️ Cancelada" }

// ---------------------------------------------------------------------------------------------
// Painel do hospital
// ---------------------------------------------------------------------------------------------

@Composable
fun PainelHospitalScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val usuario = estado.usuario ?: return
    val hospital = estado.hospitalAtual
    var aba by remember { mutableStateOf("estoque") }

    TelaLista("Painel do hospital") {
        item { AbasConta(TipoConta.HOSPITAL, Rotas.PAINEL_HOSPITAL, navegar) }
        bloco {
            Titulo(usuario.nome)
            if (hospital != null) Dica("${hospital.cidade}/${hospital.estado}")
            Espaco(8)
            when (hospital?.status) {
                StatusVerificacao.PENDENTE -> Aviso("⏳ Seu cadastro está em análise por um administrador. Enquanto isso você pode ver o painel, mas não pode publicar necessidades, abrir horários, campanhas nem confirmar doações.")
                StatusVerificacao.RECUSADO -> {
                    Aviso("❌ Seu cadastro foi recusado: ${hospital.motivoRecusa}\n\nCorrija os dados no perfil do hospital para voltar para análise.", CorAlerta)
                    Espaco(6)
                    Botao("Corrigir dados no perfil", { navegar(Rotas.PERFIL_HOSPITAL) })
                }
                StatusVerificacao.APROVADO -> Selo("✓ Hospital verificado", HemareVerde)
                null -> Aviso("Esta conta ainda não tem os dados da instituição. Complete o perfil do hospital.")
            }
        }
        item {
            SubAbas(
                listOf("estoque" to "🩸 Estoque e necessidades", "agenda" to "📅 Agenda", "solidario" to "💝 Campanhas e pacientes", "raro" to "💎 Sangue raro"),
                aba
            ) { aba = it }
        }
        if (hospital != null) when (aba) {
            "estoque" -> { bloco { EstoqueHospital(store, estado, hospital) }; bloco { NecessidadesHospital(store, estado, hospital) } }
            "agenda" -> { bloco { AbrirHorarios(store) }; bloco { AgendaDoDia(store, estado, hospital) } }
            "solidario" -> { bloco { PacientesHospital(store, estado, hospital) }; bloco { CampanhasHospital(store, estado, hospital) } }
            else -> bloco { RaroHospital(store, estado, hospital) }
        }
    }
}

@Composable
private fun EstoqueHospital(store: HemareStore, estado: Estado, h: Hospital) {
    var mensagem by remember { mutableStateOf("") }
    val meu = estado.estoque[h.id] ?: emptyMap()
    Cartao {
        Titulo("🌡️ Termômetro do meu estoque")
        Dica("Toque no nível de cada tipo. Isso aparece para o público no Termômetro de estoque (só depois de aprovado).")
        Compatibilidade.TIPOS_PEDIDO.forEach { tipo ->
            Espaco(8)
            Text(tipo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            ChipsLinha {
                Estoque.NIVEIS.forEach { (v, r) -> Chip(r, meu[tipo] == v) { mensagem = store.definirEstoque(tipo, v).texto } }
            }
        }
        Mensagem(mensagem)
    }
}

@Composable
private fun NecessidadesHospital(store: HemareStore, estado: Estado, h: Hospital) {
    var tipo by remember { mutableStateOf("") }
    var urgencia by remember { mutableStateOf("alerta") }
    var mensagem by remember { mutableStateOf("") }
    var verMatch by remember { mutableStateOf<Int?>(null) }
    var avisoMatch by remember { mutableStateOf("") }
    val minhas = estado.necessidades.filter { it.hospitalId == h.id }.sortedByDescending { it.id }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Titulo("📣 Publicar necessidade")
            Dica("Em urgência crítica ou emergência, doadores compatíveis, aptos e da sua cidade são avisados (no máximo uma vez a cada ${Alerta.DIAS_ENTRE_ALERTAS} dias cada um).")
            Espaco(8)
            CampoSelecao("Tipo sanguíneo necessário", tipo, listOf("" to "Escolha...") + Compatibilidade.TIPOS_PEDIDO.map { it to it }, { tipo = it }); Espaco(4)
            CampoSelecao("Urgência", urgencia, Estoque.URGENCIAS_DETALHADAS, { urgencia = it })
            Espaco(8)
            Botao("Publicar necessidade", { val r = store.publicarNecessidade(tipo, urgencia); mensagem = r.texto; if (r.ok) tipo = "" })
            Mensagem(mensagem)
        }
        Cartao {
            Titulo("📋 Minhas necessidades")
            if (minhas.isEmpty()) Vazio("Você ainda não publicou nenhuma necessidade.")
            minhas.forEach { n ->
                Espaco(10)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Selo(n.tipo, MaterialTheme.colorScheme.primary)
                    Selo(Estoque.rotulo(n.urgencia), MaterialTheme.colorScheme.onSurfaceVariant)
                    Selo(statusNecessidade(n.status), MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Espaco(4)
                ChipsLinha {
                    if (n.status == "aberta") {
                        Chip("✔️ Atendida", false) { store.mudarNecessidade(n.id, "atendida") }
                        Chip("Cancelar", false) { store.mudarNecessidade(n.id, "cancelada") }
                    } else Chip("Reabrir", false) { store.mudarNecessidade(n.id, "aberta") }
                    Chip(if (verMatch == n.id) "Ocultar doadores" else "👥 Doadores compatíveis", verMatch == n.id) { verMatch = if (verMatch == n.id) null else n.id; avisoMatch = "" }
                    Chip("🗑️ Apagar", false) { store.apagarNecessidade(n.id) }
                }
                if (verMatch == n.id) {
                    val m = estado.matchDe(n.id)
                    if (m != null) {
                        Espaco(6)
                        Dica("Quem recebe ${m.tipoReceptor} pode receber de: ${m.tiposCompativeis.joinToString(", ")}. Só aparecem nome e telefone de quem se identificou.")
                        if (m.doadores.isEmpty()) Vazio("Nenhum doador compatível cadastrado ainda.")
                        m.doadores.forEach { d ->
                            val el = d.doadorId?.let { id -> estado.doadores[id]?.let { Elegibilidade.verificar(estado.ultimaDoacaoDe(id), it.sexo) } }
                            Espaco(6)
                            Cartao {
                                Text(d.nome + " · ${d.tipo} · ${d.cidade}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (d.identificado) {
                                    Dica("📞 ${d.telefone}" + if (el != null) (if (el.apto) " · ✅ apto" else " · ⏳ faltam ${el.diasRestantes} dias") else "")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val ctx = LocalContext.current
                                        if (!d.telefone.isNullOrBlank() && d.telefone != "Não informado") Chip("📞 Ligar", false) { ligar(ctx, d.telefone) }
                                        if (el?.apto == true) Chip("✔️ Confirmar doação", false) { avisoMatch = store.confirmarDoacao(d.doadorId!!).texto }
                                    }
                                } else Dica("🔒 Anônimo: contato protegido")
                            }
                        }
                        Mensagem(avisoMatch)
                    }
                }
            }
        }
    }
}

@Composable
private fun AbrirHorarios(store: HemareStore) {
    var de by remember { mutableStateOf("") }
    var ate by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf(listOf(1, 2, 3, 4, 5)) }
    var primeiro by remember { mutableStateOf("08:00") }
    var ultimo by remember { mutableStateOf("11:00") }
    var intervalo by remember { mutableStateOf("60") }
    var capacidade by remember { mutableStateOf("2") }
    var mensagem by remember { mutableStateOf("") }
    Cartao {
        Titulo("➕ Abrir horários de doação")
        Dica("Crie vários dias e horários de uma vez. Quem doa escolhe o horário pelo app.")
        Espaco(8)
        Campo(de, { de = Datas.mascaraDmy(it) }, "De (dd/mm/aaaa)", teclado = KeyboardType.Number); Espaco(4)
        Campo(ate, { ate = Datas.mascaraDmy(it) }, "Até (dd/mm/aaaa)", teclado = KeyboardType.Number); Espaco(8)
        Rotulo("DIAS DA SEMANA")
        ChipsLinha { DIAS_SEMANA.forEach { (v, r) -> Chip(r, dias.contains(v)) { dias = if (dias.contains(v)) dias - v else dias + v } } }
        Espaco(8)
        Campo(primeiro, { primeiro = it.take(5) }, "Primeiro horário (HH:MM)"); Espaco(4)
        Campo(ultimo, { ultimo = it.take(5) }, "Último horário (HH:MM)"); Espaco(4)
        Campo(intervalo, { intervalo = it.filter { c -> c.isDigit() }.take(3) }, "Intervalo entre horários (minutos)", teclado = KeyboardType.Number); Espaco(4)
        Campo(capacidade, { capacidade = it.filter { c -> c.isDigit() }.take(2) }, "Pessoas por horário", teclado = KeyboardType.Number)
        Espaco(8)
        Botao("Abrir horários", {
            val horarios = Datas.gerarHorarios(primeiro, ultimo, intervalo.toIntOrNull() ?: 0)
            mensagem = if (horarios.isEmpty()) "❌ Confira o primeiro e o último horário (HH:MM) e o intervalo."
            else store.abrirHorarios(Datas.lerDmy(de), Datas.lerDmy(ate), dias, horarios, capacidade.toIntOrNull() ?: 0).texto
        })
        Mensagem(mensagem)
    }
}

@Composable
private fun AgendaDoDia(store: HemareStore, estado: Estado, h: Hospital) {
    var dia by remember { mutableStateOf(Datas.hoje()) }
    var mensagem by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val vagas = estado.vagasDoDia(h.id, dia)
    val agendados = estado.agendadosDoDia(h.id, dia)
    Cartao {
        Titulo("🗓️ Agenda de ${Datas.formatar(dia)}")
        Espaco(4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("◀ Dia anterior", false) { dia = dia.minusDays(1) }
            Chip("Hoje", dia == Datas.hoje()) { dia = Datas.hoje() }
            Chip("Próximo dia ▶", false) { dia = dia.plusDays(1) }
        }
        Espaco(8)
        Rotulo("HORÁRIOS ABERTOS")
        if (vagas.isEmpty()) Vazio("Nenhum horário aberto neste dia.")
        vagas.forEach { v ->
            Espaco(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("${v.vaga.hora} — ${v.reservadas}/${v.vaga.capacidade}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Chip("−", false, v.vaga.capacidade > 1) { mensagem = store.mudarCapacidade(v.vaga.id, v.vaga.capacidade - 1).texto }
                Chip("+", false, v.vaga.capacidade < Agendamento.CAPACIDADE_MAXIMA) { mensagem = store.mudarCapacidade(v.vaga.id, v.vaga.capacidade + 1).texto }
                Chip("🗑️", false, v.reservadas == 0) { mensagem = store.apagarVaga(v.vaga.id).texto }
            }
        }
        Espaco(10)
        Rotulo("QUEM AGENDOU")
        if (agendados.isEmpty()) Vazio("Ninguém agendou neste dia.")
        agendados.forEach { a ->
            Espaco(8)
            Cartao {
                Text("${a.hora} · ${a.nome} (${a.tipo})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Dica("${Agendamento.ICONES_STATUS[a.status] ?: ""} ${Agendamento.NOMES_STATUS[a.status] ?: a.status}" + (a.telefone?.let { " · 📞 $it" } ?: " · 🔒 contato protegido"))
                if (a.status == "agendado") {
                    ChipsLinha {
                        Chip("🩸 Compareceu", false) { mensagem = store.mudarStatusAgendamento(a.id, "compareceu").texto }
                        Chip("⚠️ Faltou", false) { mensagem = store.mudarStatusAgendamento(a.id, "faltou").texto }
                        Chip("Cancelar", false) { mensagem = store.mudarStatusAgendamento(a.id, "cancelado").texto }
                        if (!a.telefone.isNullOrBlank()) Chip("📞 Ligar", false) { ligar(ctx, a.telefone) }
                    }
                }
            }
        }
        Mensagem(mensagem)
    }
}

@Composable
private fun PacientesHospital(store: HemareStore, estado: Estado, h: Hospital) {
    var apelido by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var condicao by remember { mutableStateOf(Apadrinhamento.CONDICOES[0]) }
    var frequencia by remember { mutableStateOf("") }
    var autorizacao by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    val freq = frequencia.toIntOrNull()
    val casos = estado.casosDoHospital(h.id)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Titulo("💝 Publicar paciente para apadrinhamento")
            Dica("Para quem precisa de transfusão com regularidade. O paciente aparece só por apelido, nunca com o nome verdadeiro.")
            Espaco(8)
            Campo(apelido, { apelido = it }, "Apelido do paciente *", dica = "Ex.: Guerreiro Léo. Não use o nome real."); Espaco(4)
            CampoSelecao("Tipo sanguíneo *", tipo, listOf("" to "Escolha...") + Compatibilidade.TIPOS_PEDIDO.map { it to it }, { tipo = it }); Espaco(4)
            CampoSelecao("Condição", condicao, Apadrinhamento.CONDICOES.map { it to it }, { condicao = it }); Espaco(4)
            Campo(frequencia, { frequencia = it.filter { c -> c.isDigit() }.take(3) }, "A cada quantos dias precisa de sangue? *", teclado = KeyboardType.Number,
                dica = if (freq != null && freq in Apadrinhamento.FREQUENCIA_MINIMA..Apadrinhamento.FREQUENCIA_MAXIMA) "Meta sugerida: ${Apadrinhamento.sugerirMeta(freq)} padrinhos."
                else "De ${Apadrinhamento.FREQUENCIA_MINIMA} a ${Apadrinhamento.FREQUENCIA_MAXIMA} dias.")
            CaixaMarcar("O paciente ou a família autorizou divulgar este pedido (só com apelido).", autorizacao, { autorizacao = it })
            Espaco(6)
            Botao("Publicar paciente", {
                val r = store.publicarCaso(apelido, tipo, condicao, frequencia, autorizacao)
                mensagem = r.texto
                if (r.ok) { apelido = ""; tipo = ""; frequencia = ""; autorizacao = false }
            })
            Mensagem(mensagem)
        }
        Cartao {
            Titulo("👥 Meus pacientes")
            if (casos.isEmpty()) Vazio("Nenhum paciente publicado.")
            casos.forEach { c ->
                val meta = Apadrinhamento.situacaoMeta(c.padrinhos.size, c.meta)
                val pode = Apadrinhamento.podeChamarAgora(c.ultimaChamada)
                var aviso by remember(c.id) { mutableStateOf("") }
                Espaco(10)
                Text("${c.apelido} · ${c.tipo}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Dica("${c.condicao} · transfusão a cada ${c.frequenciaDias} dias")
                Espaco(4)
                BarraProgresso(meta.porcentagem)
                Dica("${meta.padrinhos} de ${meta.meta} padrinhos" + if (meta.completo) " 🎉" else "")
                Espaco(4)
                ChipsLinha {
                    Chip(if (pode.pode) "📣 Chamar padrinhos" else "Novo chamado em ${pode.diasRestantes} d", false, pode.pode && c.padrinhos.isNotEmpty()) { aviso = store.chamarPadrinhos(c.id).texto }
                    Chip("Encerrar", false) { store.encerrarCaso(c.id) }
                }
                Mensagem(aviso)
            }
        }
    }
}

@Composable
private fun CampanhasHospital(store: HemareStore, estado: Estado, h: Hospital) {
    val ctx = LocalContext.current
    var apelido by remember { mutableStateOf("") }
    var meta by remember { mutableStateOf("") }
    var prazo by remember { mutableStateOf("") }
    var autorizacao by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    val campanhas = estado.campanhasDoHospital(h.id)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Titulo("🩸 Abrir campanha de reposição")
            Dica("Para um paciente internado: família e amigos prometem doar e você confirma cada doação feita. Só o apelido aparece.")
            Espaco(8)
            Campo(apelido, { apelido = it }, "Apelido do paciente *"); Espaco(4)
            Campo(meta, { meta = it.filter { c -> c.isDigit() }.take(2) }, "Bolsas pedidas (1 a ${Campanha.META_MAXIMA}) *", teclado = KeyboardType.Number); Espaco(4)
            Campo(prazo, { prazo = it.filter { c -> c.isDigit() }.take(2) }, "Prazo em dias (padrão ${Campanha.PRAZO_PADRAO})", teclado = KeyboardType.Number)
            CaixaMarcar("O paciente ou a família autorizou divulgar este pedido.", autorizacao, { autorizacao = it })
            Espaco(6)
            Botao("Abrir campanha", {
                val r = store.abrirCampanha(apelido, meta, prazo, autorizacao)
                mensagem = r.texto
                if (r.ok) { apelido = ""; meta = ""; prazo = ""; autorizacao = false }
            })
            Mensagem(mensagem)
        }
        Cartao {
            Titulo("📌 Minhas campanhas")
            if (campanhas.isEmpty()) Vazio("Nenhuma campanha aberta.")
            campanhas.forEach { c ->
                val sit = estado.situacaoCampanha(c)
                var aviso by remember(c.codigo) { mutableStateOf("") }
                Espaco(10)
                Text("${c.apelido} · código ${c.codigo}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Dica("${Campanha.ROTULOS[sit.status]} · ${sit.confirmadas}/${sit.metaBolsas} confirmadas · ${sit.prometidas} prometidas · ${sit.diasRestantes} dias")
                BarraProgresso(sit.porcentagem)
                val pendentes = c.promessas.filter { !it.confirmada }
                pendentes.forEach { p ->
                    Espaco(4)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${estado.nomeExibido(p.doadorId)} · ${Datas.dm(p.data)}", color = MaterialTheme.colorScheme.onSurface)
                        Chip("✔️ Doou", false) { aviso = store.confirmarPromessa(c.codigo, p.doadorId).texto }
                    }
                }
                Espaco(4)
                ChipsLinha {
                    val texto = Campanha.mensagemCompartilhar(c.apelido, estado.hospitalNome(h.id), h.cidade, sit.faltam, "Código da campanha no app Hemare: ${c.codigo}")
                    Chip("📤 Compartilhar", false) { compartilharTexto(ctx, texto) }
                    Chip("WhatsApp", false) { compartilharWhatsApp(ctx, texto) }
                    Chip("Copiar código", false) { copiarTexto(ctx, c.codigo); aviso = "✅ Código copiado." }
                    Chip("Encerrar", false) { store.encerrarCampanha(c.codigo) }
                }
                Mensagem(aviso)
            }
        }
    }
}

@Composable
private fun RaroHospital(store: HemareStore, estado: Estado, h: Hospital) {
    var codigo by remember { mutableStateOf("") }
    var achado by remember { mutableStateOf<AchadoRaro?>(null) }
    var avisoCodigo by remember { mutableStateOf("") }
    var fenotipo by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var apelido by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    val pedidos = estado.pedidosRarosDoHospital(h.id)
    val previa = if (fenotipo.isNotEmpty()) estado.previaRaro(fenotipo, h) else null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Titulo("🔎 Confirmar fenótipo de um doador")
            Dica("O doador mostra o código e o laudo. Só confirme depois de conferir o laudo pessoalmente.")
            Espaco(8)
            Campo(codigo, { codigo = it.uppercase().take(10); achado = null; avisoCodigo = "" }, "Código do doador")
            Espaco(6)
            BotaoVazado("Buscar código", { achado = estado.buscarCodigoRaro(codigo); avisoCodigo = if (achado == null) "❌ Código não encontrado." else "" })
            achado?.let {
                Espaco(6)
                Paragrafo("${it.primeiroNome} declarou ${it.fenotipoRotulo}" + if (it.status == "confirmado") " (já confirmado)." else ".")
                if (it.status != "confirmado") Botao("✔️ Conferi o laudo: confirmar", { avisoCodigo = store.confirmarCodigoRaro(codigo).texto; achado = null })
            }
            Mensagem(avisoCodigo)
        }
        Cartao {
            Titulo("🆘 Pedido de sangue raro")
            Dica("Chama só doadores confirmados, no alcance escolhido por eles, aptos e não chamados na última semana. Até ${Raros.MAX_PEDIDOS_ABERTOS} pedidos abertos; cada um vale ${Raros.VALIDADE_PEDIDO_HORAS} h.")
            Espaco(8)
            CampoSelecao("Fenótipo procurado *", fenotipo, listOf("" to "Escolha...") + Raros.FENOTIPOS, { fenotipo = it })
            previa?.let { Dica("Na rede: ${it.naRede} · no alcance: ${it.noAlcance} · aptos: ${it.aptos} · seriam chamados: ${it.convocados}") }
            Espaco(4)
            Campo(motivo, { motivo = it }, "Motivo (sem nome do paciente) *"); Espaco(4)
            Campo(apelido, { apelido = it }, "Apelido do paciente (opcional)")
            Espaco(8)
            Botao("Chamar a rede", { val r = store.criarPedidoRaro(fenotipo, motivo, apelido); mensagem = r.texto; if (r.ok) { motivo = ""; apelido = ""; fenotipo = "" } })
            Mensagem(mensagem)
        }
        Cartao {
            Titulo("📨 Meus pedidos")
            if (pedidos.isEmpty()) Vazio("Nenhum pedido de sangue raro.")
            pedidos.forEach { p ->
                var aviso by remember(p.id) { mutableStateOf("") }
                val vivo = p.aberto && p.expiraEmMs > System.currentTimeMillis()
                Espaco(10)
                Text("${Raros.rotuloFenotipo(p.fenotipo)}${p.apelidoPaciente?.let { " · $it" } ?: ""}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Dica("${if (vivo) "🟢 Aberto" else "⏹️ Encerrado"} · ${p.convocados.size} chamados · ${p.convocados.count { it.resposta == "disponivel" }} disponíveis")
                if (vivo) {
                    p.convocados.filter { it.resposta == "disponivel" }.forEach { c ->
                        val d = estado.doadores[c.doadorId]
                        Espaco(4)
                        Text("${estado.nomeExibido(c.doadorId)} · 📞 ${d?.telefone ?: "—"}", color = MaterialTheme.colorScheme.onSurface)
                        Chip(if (c.doacaoConfirmada) "✓ Doação confirmada" else "✔️ Confirmar doação", false, !c.doacaoConfirmada) { aviso = store.confirmarDoacaoRaro(p.id, c.doadorId).texto }
                    }
                    Espaco(4)
                    Chip("Encerrar pedido", false) { aviso = store.encerrarPedidoRaro(p.id).texto }
                }
                Mensagem(aviso)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Perfil do hospital
// ---------------------------------------------------------------------------------------------

@Composable
fun PerfilHospitalScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val usuario = estado.usuario ?: return
    val h = estado.hospitalAtual
    var aba by remember { mutableStateOf("dados") }

    TelaLista("Perfil do hospital") {
        item { AbasConta(TipoConta.HOSPITAL, Rotas.PERFIL_HOSPITAL, navegar) }
        item { SubAbas(listOf("dados" to "🏥 Dados", "verificacao" to "✅ Verificação", "conta" to "🔑 Conta"), aba) { aba = it } }
        when {
            aba == "conta" -> bloco { SegurancaConta(store); Espaco(12); ExcluirContaHospital(store) }
            h == null -> bloco { Aviso("Esta conta ainda não tem os dados da instituição.") }
            aba == "verificacao" -> bloco {
                Cartao {
                    Titulo("Situação do cadastro")
                    Espaco(6)
                    when (h.status) {
                        StatusVerificacao.APROVADO -> { Selo("✓ Hospital verificado", HemareVerde); Espaco(6); Paragrafo("Você pode publicar necessidades, abrir horários, campanhas e confirmar doações.") }
                        StatusVerificacao.PENDENTE -> { Selo("⏳ Em análise", CorAviso); Espaco(6); Paragrafo("Um administrador confere o CNPJ e o CNES. Enquanto isso o painel fica só para consulta.") }
                        StatusVerificacao.RECUSADO -> { Selo("❌ Recusado", CorAlerta); Espaco(6); Paragrafo("Motivo: ${h.motivoRecusa}"); Dica("Corrija os dados na aba “Dados” e salve: o cadastro volta para análise.") }
                    }
                    Espaco(8)
                    Dica("Mudar o CNPJ ou o CNES também faz o cadastro voltar para análise.")
                }
            }
            else -> bloco { FormularioHospital(store, usuario.nome, h) }
        }
    }
}

@Composable
private fun FormularioHospital(store: HemareStore, nomeAtual: String, h: Hospital) {
    val escopo = rememberCoroutineScope()
    var nome by remember(h.id) { mutableStateOf(nomeAtual) }
    var cnpj by remember(h.id) { mutableStateOf(h.cnpj) }
    var cnes by remember(h.id) { mutableStateOf(h.cnes) }
    var cep by remember(h.id) { mutableStateOf(h.cep) }
    var endereco by remember(h.id) { mutableStateOf(h.endereco) }
    var numero by remember(h.id) { mutableStateOf(h.numero) }
    var bairro by remember(h.id) { mutableStateOf(h.bairro) }
    var complemento by remember(h.id) { mutableStateOf(h.complemento ?: "") }
    var cidade by remember(h.id) { mutableStateOf(h.cidade) }
    var uf by remember(h.id) { mutableStateOf(h.estado) }
    var buscando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Titulo("🏥 Instituição")
            Espaco(8)
            Campo(nome, { nome = it }, "Nome do hospital *"); Espaco(4)
            Campo(cnpj, { cnpj = Documentos.mascaraCnpj(it) }, "CNPJ *", teclado = KeyboardType.Number,
                aviso = if (cnpj.length == 18 && !Documentos.cnpjValido(cnpj)) "CNPJ inválido." else ""); Espaco(4)
            Campo(cnes, { cnes = it.filter { c -> c.isDigit() }.take(7) }, "CNES (7 números) *", teclado = KeyboardType.Number)
        }
        Cartao {
            Titulo("📍 Endereço")
            Espaco(8)
            Campo(cep, {
                cep = Documentos.mascaraCep(it)
                if (cep.length == 9) escopo.launch {
                    buscando = true
                    val achado = buscarCep(cep)
                    buscando = false
                    if (achado != null) {
                        if (achado.logradouro.isNotBlank()) endereco = achado.logradouro
                        if (achado.bairro.isNotBlank()) bairro = achado.bairro
                        if (achado.cidade.isNotBlank()) cidade = achado.cidade
                        if (achado.uf.isNotBlank()) uf = achado.uf
                    }
                }
            }, "CEP *", teclado = KeyboardType.Number, dica = if (buscando) "Buscando endereço..." else "Preenche o endereço automaticamente."); Espaco(4)
            Campo(endereco, { endereco = it }, "Rua / avenida *"); Espaco(4)
            Campo(numero, { numero = it }, "Número *"); Espaco(4)
            Campo(bairro, { bairro = it }, "Bairro *"); Espaco(4)
            Campo(complemento, { complemento = it }, "Complemento"); Espaco(4)
            Campo(cidade, { cidade = it }, "Cidade *"); Espaco(4)
            CampoSelecao("Estado *", uf, Estados.SIGLAS.map { it to it }, { uf = it })
        }
        Botao("Salvar dados", { mensagem = store.salvarPerfilHospital(nome, cnpj, cnes, cep, endereco, numero, bairro, complemento, cidade, uf).texto })
        Mensagem(mensagem)
    }
}

@Composable
private fun ExcluirContaHospital(store: HemareStore) {
    var aberto by remember { mutableStateOf(false) }
    var senha by remember { mutableStateOf("") }
    var entendi by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    Cartao {
        Titulo("🗑️ Excluir conta")
        Dica("Apaga os dados da instituição. As doações já registradas ficam na cadeia de auditoria sem dados pessoais.")
        Espaco(6)
        BotaoVazado(if (aberto) "Cancelar" else "Excluir minha conta", { aberto = !aberto }, perigo = true)
        if (aberto) {
            Espaco(6)
            Campo(senha, { senha = it }, "Digite sua senha para confirmar", senha = true)
            CaixaMarcar("Entendo que a exclusão é definitiva.", entendi, { entendi = it })
            BotaoVazado("Excluir definitivamente", { aviso = store.excluirConta(senha).texto }, habilitado = entendi && senha.isNotEmpty(), perigo = true)
        }
        Mensagem(aviso)
    }
}

// ---------------------------------------------------------------------------------------------
// Radar de aptidão (hospital e admin)
// ---------------------------------------------------------------------------------------------

@Composable
fun RadarScreen(store: HemareStore) {
    val estado by store.estado.collectAsState()
    val linhas = Radar.calcular(estado.candidatos())
    TelaLista("Radar de aptidão") {
        bloco {
            Paragrafo("Quantos doadores de cada tipo estão aptos agora ou ficam aptos em breve. Só números: nenhum nome aparece aqui.", suave = true)
            Espaco(10)
            if (linhas.isEmpty()) Vazio("Ainda não há doadores com perfil completo.")
            linhas.forEach { r ->
                Cartao {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(r.tipo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Numero("${r.aptoAgora}", "aptos agora")
                        Numero("${r.em7Dias}", "em até 7 dias")
                        Numero("${r.em30Dias}", "em até 30 dias")
                    }
                    if (r.maisDe30Dias > 0) Dica("+ ${r.maisDe30Dias} depois de 30 dias")
                }
                Espaco(8)
            }
        }
    }
}
