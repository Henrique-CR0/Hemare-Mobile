package com.example.hemaremobile.data

import com.example.hemaremobile.domain.*
import java.time.LocalDate

// Operações do doador sobre a loja: agendar, apadrinhar, prometer em campanhas e rede de sangue raro.

// ---------------------------------------------------------------------
// Agendar doação
// ---------------------------------------------------------------------

data class HorarioVaga(val vagaId: Int, val hora: String, val livres: Int)
data class DiaVagas(val data: LocalDate, val horarios: List<HorarioVaga>)

/** Hospitais aprovados com horário livre daqui para frente (com a quantidade de vagas livres). */
fun Estado.hospitaisComVagas(agoraMs: Long = System.currentTimeMillis()): List<Pair<Hospital, Int>> =
    hospitaisAprovados().map { h -> h to vagasLivresDe(h.id, agoraMs).sumOf { it.second } }.filter { it.second > 0 }.sortedBy { hospitalNome(it.first.id) }

private fun Estado.vagasLivresDe(hospitalId: Int, agoraMs: Long): List<Pair<Vaga, Int>> =
    vagas.filter { it.hospitalId == hospitalId && Agendamento.momentoDaVaga(it.data, it.hora) > agoraMs + Agendamento.ANTECEDENCIA_MIN_HORAS * 3600000L }
        .map { it to (it.capacidade - reservadas(it.id)) }.filter { it.second > 0 }

fun Estado.diasDoHospital(hospitalId: Int, agoraMs: Long = System.currentTimeMillis()): List<DiaVagas> =
    vagasLivresDe(hospitalId, agoraMs).groupBy { it.first.data }.toSortedMap().map { (data, lista) ->
        DiaVagas(data, lista.sortedBy { it.first.hora }.map { HorarioVaga(it.first.id, it.first.hora, it.second) })
    }

private fun HemareStore.tentarAgendar(vagaId: Int, trocandoId: Int?): Res {
    val e = atual()
    val d = e.doadorAtual ?: return Res.erro("Complete seu perfil (informe o sexo) para agendar.")
    val vaga = e.vagaPorId(vagaId) ?: return Res.erro("Horário não encontrado.")
    if (e.hospitais[vaga.hospitalId]?.aprovado != true) return Res.erro("Este hospital ainda não foi aprovado para receber agendamentos.")
    val ativoReg = e.agendamentoAtivoDe(d.id)
    val ativo = if (trocandoId == null) ativoReg?.let { r -> e.vagaPorId(r.vagaId)?.let { it.data to it.hora } } else null
    val erro = Agendamento.podeAgendar(
        d.sexo, e.ultimaDoacaoDe(d.id), e.doacoesDe(d.id).map { it.data }, vaga.data, vaga.hora, vaga.capacidade, e.reservadas(vaga.id), ativo
    )
    if (erro != null) return Res.erro(erro)
    val novoId = e.proximoId
    var agendamentos = e.agendamentos
    if (trocandoId != null) agendamentos = agendamentos.map { if (it.id == trocandoId) it.copy(status = "cancelado") else it }
    definir(e.copy(agendamentos = agendamentos + AgendamentoReg(novoId, vaga.id, d.id, "agendado"), proximoId = novoId + 1))
    return Res.ok("Agendado para ${Datas.formatar(vaga.data)} às ${vaga.hora} em ${e.hospitalNome(vaga.hospitalId)}.", novoId)
}

fun HemareStore.agendar(vagaId: Int): Res = tentarAgendar(vagaId, null)

fun HemareStore.reagendar(agendamentoId: Int, vagaId: Int): Res {
    val e = atual()
    val ag = e.agendamentos.firstOrNull { it.id == agendamentoId && it.doadorId == e.sessaoUsuarioId } ?: return Res.erro("Agendamento não encontrado.")
    val v = e.vagaPorId(ag.vagaId) ?: return Res.erro("Agendamento não encontrado.")
    val erro = Agendamento.podeCancelar(ag.status, v.data, v.hora)
    if (erro != null) return Res.erro(erro)
    return tentarAgendar(vagaId, agendamentoId)
}

fun HemareStore.cancelarAgendamento(agendamentoId: Int): Res {
    val e = atual()
    val ag = e.agendamentos.firstOrNull { it.id == agendamentoId && it.doadorId == e.sessaoUsuarioId } ?: return Res.erro("Agendamento não encontrado.")
    val v = e.vagaPorId(ag.vagaId) ?: return Res.erro("Agendamento não encontrado.")
    val erro = Agendamento.podeCancelar(ag.status, v.data, v.hora)
    if (erro != null) return Res.erro(erro)
    definir(e.copy(agendamentos = e.agendamentos.map { if (it.id == agendamentoId) it.copy(status = "cancelado") else it }))
    return Res.ok("Agendamento cancelado.")
}

// ---------------------------------------------------------------------
// Apadrinhamento
// ---------------------------------------------------------------------

/** Pacientes que procuram padrinhos: casos ativos de hospitais aprovados. */
fun Estado.casosPublicos(): List<Caso> = casos.filter { it.ativo && hospitais[it.hospitalId]?.aprovado == true }

fun HemareStore.apadrinhar(casoId: Int): Res {
    val e = atual()
    val d = e.doadorAtual ?: return Res.erro("Complete seu perfil para apadrinhar.")
    val caso = e.casos.firstOrNull { it.id == casoId } ?: return Res.erro("Pedido não encontrado.")
    val erro = Apadrinhamento.podeApadrinhar(d.tipoSanguineo, caso.tipo, e.afilhadosDe(d.id).size, caso.padrinhos.contains(d.id), caso.ativo)
    if (erro != null) return Res.erro(erro)
    definir(e.copy(casos = e.casos.map { if (it.id == casoId) it.copy(padrinhos = it.padrinhos + d.id) else it }))
    return Res.ok("Obrigado! Você agora é padrinho de ${caso.apelido}. 💝")
}

fun HemareStore.deixarDeApadrinhar(casoId: Int): Res {
    val e = atual()
    val id = e.sessaoUsuarioId ?: return Res.erro("Entre na sua conta.")
    definir(e.copy(casos = e.casos.map { if (it.id == casoId) it.copy(padrinhos = it.padrinhos - id) else it }))
    return Res.ok("Você deixou de ser padrinho deste paciente.")
}

// ---------------------------------------------------------------------
// Campanhas de reposição
// ---------------------------------------------------------------------

data class PromessaDoador(val campanha: CampanhaReg, val promessa: Promessa)

fun Estado.promessasDe(doadorId: Int): List<PromessaDoador> =
    campanhas.mapNotNull { c -> c.promessas.firstOrNull { it.doadorId == doadorId }?.let { PromessaDoador(c, it) } }

fun HemareStore.prometer(codigo: String, data: LocalDate?): Res {
    val e = atual()
    val d = e.doadorAtual ?: return Res.erro("Só contas de doador com perfil completo podem prometer doação.")
    val c = e.campanhaPorCodigo(codigo) ?: return Res.erro("Campanha não encontrada.")
    val erro = Campanha.validarPromessa(
        data, e.situacaoCampanha(c), c.expiraEm, c.promessas.any { it.doadorId == d.id }, e.ultimaDoacaoDe(d.id), d.sexo
    )
    if (erro != null) return Res.erro(erro)
    definir(e.copy(campanhas = e.campanhas.map { if (it.codigo == c.codigo) it.copy(promessas = it.promessas + Promessa(d.id, data!!, false)) else it }))
    return Res.ok("Obrigado! Sua promessa para ${c.apelido} em ${Datas.dmy(data!!)} foi registrada. Só conta depois que o hospital confirmar a doação.")
}

fun HemareStore.cancelarPromessa(codigo: String): Res {
    val e = atual()
    val id = e.sessaoUsuarioId ?: return Res.erro("Entre na sua conta.")
    val c = e.campanhaPorCodigo(codigo) ?: return Res.erro("Campanha não encontrada.")
    if (c.promessas.any { it.doadorId == id && it.confirmada }) return Res.erro("Esta doação já foi confirmada pelo hospital.")
    definir(e.copy(campanhas = e.campanhas.map { if (it.codigo == c.codigo) it.copy(promessas = it.promessas.filter { p -> p.doadorId != id }) else it }))
    return Res.ok("Promessa cancelada.")
}

// ---------------------------------------------------------------------
// Rede de sangue raro (lado do doador)
// ---------------------------------------------------------------------

data class ChamadoRaro(val pedido: PedidoRaro, val hospital: String, val cidade: String, val estado: String, val resposta: String?, val horasRestantes: Int)

fun Estado.chamadosDo(doadorId: Int, agoraMs: Long = System.currentTimeMillis()): List<ChamadoRaro> =
    pedidosRaros.filter { it.aberto && it.expiraEmMs > agoraMs }.mapNotNull { p ->
        val conv = p.convocados.firstOrNull { it.doadorId == doadorId } ?: return@mapNotNull null
        val h = hospitais[p.hospitalId]
        ChamadoRaro(p, hospitalNome(p.hospitalId), h?.cidade ?: "", h?.estado ?: "", conv.resposta,
            Math.ceil((p.expiraEmMs - agoraMs) / 3600000.0).toInt())
    }

fun HemareStore.participarDaRede(fenotipo: String, alcance: String, consentimento: Boolean): Res {
    val e = atual()
    val d = e.doadorAtual ?: return Res.erro("Complete seu perfil primeiro.")
    if (Raros.FENOTIPOS.none { it.first == fenotipo }) return Res.erro("Escolha o seu fenótipo raro na lista.")
    if (Raros.ALCANCES.none { it.first == alcance }) return Res.erro("Escolha até onde você pode ir.")
    if (!consentimento) return Res.erro("Para entrar na rede, confirme que autoriza hospitais aprovados a contatar você em emergências reais.")
    if (d.telefone.isNullOrBlank()) return Res.erro("Para entrar, informe um telefone no seu perfil e salve.")
    val mesmo = d.raro.fenotipo == fenotipo
    val info = RaroInfo(
        fenotipo = fenotipo, alcance = alcance, consentimento = true,
        status = if (mesmo && d.raro.status == "confirmado") "confirmado" else "declarado",
        codigo = if (mesmo && d.raro.codigo != null) d.raro.codigo else Raros.gerarCodigo(), ultimaNotificacao = d.raro.ultimaNotificacao
    )
    definir(e.copy(doadores = e.doadores + (d.id to d.copy(raro = info))))
    return Res.ok(if (d.raro.participa) "Mudanças salvas." else "Pronto! Você está na rede.")
}

fun HemareStore.sairDaRede(): Res {
    val e = atual()
    val d = e.doadorAtual ?: return Res.erro("Complete seu perfil primeiro.")
    definir(e.copy(doadores = e.doadores + (d.id to d.copy(raro = RaroInfo()))))
    return Res.ok("Você saiu da rede. Pode voltar quando quiser.")
}

fun HemareStore.responderChamado(pedidoId: Int, resposta: String): Res {
    val e = atual()
    val id = e.sessaoUsuarioId ?: return Res.erro("Entre na sua conta.")
    if (resposta != "disponivel" && resposta != "indisponivel") return Res.erro("Resposta inválida.")
    val p = e.pedidosRaros.firstOrNull { it.id == pedidoId && it.convocados.any { c -> c.doadorId == id } } ?: return Res.erro("Chamado não encontrado.")
    if (!p.aberto || p.expiraEmMs <= System.currentTimeMillis()) return Res.erro("Este chamado já foi encerrado.")
    definir(e.copy(pedidosRaros = e.pedidosRaros.map { ped ->
        if (ped.id == pedidoId) ped.copy(convocados = ped.convocados.map { if (it.doadorId == id) it.copy(resposta = resposta) else it }) else ped
    }))
    return Res.ok(if (resposta == "disponivel") "Obrigado! O hospital vai falar com você pelo telefone do seu perfil." else "Tudo bem, obrigado por avisar!")
}
