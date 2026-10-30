package com.example.hemaremobile.data

import com.example.hemaremobile.domain.*
import java.time.LocalDate

// Operações do hospital sobre a loja: estoque, necessidades, match, agenda, campanhas, apadrinhamento e rede de sangue raro.

/** Registra uma doação na cadeia de confiança (cada doação liga o hash à anterior). */
fun Estado.registrarDoacao(doadorId: Int, hospitalId: Int, data: LocalDate, emCampanha: Boolean = false): Estado {
    val anterior = doacoes.lastOrNull()?.hash ?: Cadeia.GENESIS
    val hash = Cadeia.calcularHash(anterior, doadorId, hospitalId, data)
    return copy(doacoes = doacoes + Doacao(proximoId, doadorId, hospitalId, data, hash, emCampanha), proximoId = proximoId + 1)
}

/** O hospital logado, se estiver aprovado; senão a frase de bloqueio. */
private fun Estado.hospitalLiberado(): Pair<Hospital?, String?> {
    val h = hospitalAtual ?: return null to "Entre com uma conta de hospital."
    return when (h.status) {
        StatusVerificacao.APROVADO -> h to null
        StatusVerificacao.RECUSADO -> h to "O cadastro deste hospital foi recusado na verificação. Veja o motivo no seu painel."
        StatusVerificacao.PENDENTE -> h to "Seu hospital ainda está em verificação. Esta ação será liberada assim que um administrador aprovar o cadastro."
    }
}

// ---------------------------------------------------------------------
// Estoque, necessidades e match
// ---------------------------------------------------------------------

fun HemareStore.definirEstoque(tipo: String, nivel: String): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    if (Compatibilidade.TIPOS_PEDIDO.none { it == tipo } || Estoque.NIVEIS.none { it.first == nivel }) return Res.erro("Tipo ou nível inválido.")
    definir(e.copy(estoque = e.estoque + (h.id to ((e.estoque[h.id] ?: emptyMap()) + (tipo to nivel)))))
    return Res.ok("Estoque atualizado.")
}

fun HemareStore.publicarNecessidade(tipo: String, urgencia: String): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    if (Compatibilidade.TIPOS_PEDIDO.none { it == tipo }) return Res.erro("Escolha o tipo sanguíneo necessário (O+, O-, A+, A-, B+, B-, AB+ ou AB-).")
    if (Estoque.NIVEIS.none { it.first == urgencia }) return Res.erro("Urgência inválida. Use estável, alerta, crítico ou emergência.")
    val id = e.proximoId
    var novo = e.copy(necessidades = e.necessidades + Necessidade(id, h.id, tipo, urgencia, "aberta"), proximoId = id + 1)
    var msg = "Necessidade publicada com sucesso."
    if (urgencia == "critico" || urgencia == "emergencia") {
        val sel = Alerta.selecionar(e.candidatos(), Compatibilidade.doadoresCompativeis(tipo), h.cidade)
        val ids = sel.convocados.map { it.id }.toSet()
        novo = novo.copy(doadores = novo.doadores.mapValues { (k, d) -> if (k in ids) d.copy(ultimoAlerta = Datas.hoje()) else d })
        msg += " " + Alerta.descrever(sel.resumo, h.cidade)
    }
    definir(novo)
    return Res.ok(msg, id)
}

fun HemareStore.mudarNecessidade(id: Int, status: String): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    if (status !in listOf("aberta", "atendida", "cancelada")) return Res.erro("Status inválido. Use aberta, atendida ou cancelada.")
    if (e.necessidades.none { it.id == id && it.hospitalId == h.id }) return Res.erro("Necessidade não encontrada.")
    definir(e.copy(necessidades = e.necessidades.map { if (it.id == id) it.copy(status = status) else it }))
    return Res.ok("")
}

fun HemareStore.apagarNecessidade(id: Int): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    definir(e.copy(necessidades = e.necessidades.filter { !(it.id == id && it.hospitalId == h.id) }))
    return Res.ok("")
}

data class MatchDoador(val doadorId: Int?, val nome: String, val tipo: String, val cidade: String, val telefone: String?, val identificado: Boolean)
data class Match(val tipoReceptor: String, val tiposCompativeis: List<String>, val doadores: List<MatchDoador>)

/** Doadores compatíveis com uma necessidade, passando pela UMA função que decide o que o hospital vê (privacidade). */
fun Estado.matchDe(necessidadeId: Int): Match? {
    val n = necessidades.firstOrNull { it.id == necessidadeId } ?: return null
    val tipos = Compatibilidade.doadoresCompativeis(n.tipo)
    val lista = doadores.values.filter { it.tipoSanguineo in tipos }.map { d ->
        val v = Perfil.visaoDoHospital(nomeDe(d.id), d.nomeSocial, d.tipoSanguineo, d.cidade, d.visibilidade, d.telefone)
        MatchDoador(if (v.identificado) d.id else null, v.ve[0].second, d.tipoSanguineo, d.cidade, if (v.identificado) (d.telefone ?: "Não informado") else null, v.identificado)
    }.sortedByDescending { it.identificado }
    return Match(n.tipo, tipos, lista)
}

fun HemareStore.confirmarDoacao(doadorId: Int): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val d = e.doadores[doadorId] ?: return Res.erro("Doador não encontrado.")
    if (d.visibilidade != "identificado") return Res.erro("Só dá para confirmar a doação de quem autorizou ser identificado.")
    val el = Elegibilidade.verificar(e.ultimaDoacaoDe(doadorId), d.sexo)
    if (!el.apto) return Res.erro("Este doador ainda está no intervalo entre doações (faltam ${el.diasRestantes} dias).")
    definir(e.registrarDoacao(doadorId, h.id, Datas.hoje()))
    return Res.ok("Doação confirmada e registrada na cadeia de confiança.")
}

// ---------------------------------------------------------------------
// Agenda do hospital
// ---------------------------------------------------------------------

data class VagaDoDia(val vaga: Vaga, val reservadas: Int)
data class AgendadoDoDia(val id: Int, val hora: String, val nome: String, val tipo: String, val telefone: String?, val status: String, val data: LocalDate)

fun Estado.vagasDoDia(hospitalId: Int, dia: LocalDate): List<VagaDoDia> =
    vagas.filter { it.hospitalId == hospitalId && it.data == dia }.sortedBy { it.hora }.map { VagaDoDia(it, reservadas(it.id)) }

fun Estado.agendadosDoDia(hospitalId: Int, dia: LocalDate): List<AgendadoDoDia> =
    agendamentos.mapNotNull { ag ->
        val v = vagaPorId(ag.vagaId) ?: return@mapNotNull null
        if (v.hospitalId != hospitalId || v.data != dia) return@mapNotNull null
        val d = doadores[ag.doadorId]
        val identificado = d?.visibilidade == "identificado"
        AgendadoDoDia(ag.id, v.hora, nomeExibido(ag.doadorId), d?.tipoSanguineo ?: "—", if (identificado) d?.telefone else null, ag.status, v.data)
    }.sortedBy { it.hora }

fun HemareStore.abrirHorarios(de: LocalDate?, ate: LocalDate?, diasSemana: List<Int>, horarios: List<String>, capacidade: Int): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val v = Agendamento.validarLote(de, ate, diasSemana, horarios, capacidade)
    if (v is Validacao.Erro) return Res.erro(v.mensagem)
    val novas = (v as Validacao.Ok).valor
    var id = e.proximoId
    val jaExistem = e.vagas.filter { it.hospitalId == h.id }.map { it.data to it.hora }.toSet()
    val criar = novas.filter { (it.data to it.hora) !in jaExistem }
    val vagas = criar.map { Vaga(id++, h.id, it.data, it.hora, it.capacidade) }
    definir(e.copy(vagas = e.vagas + vagas, proximoId = id))
    return Res.ok("${vagas.size} horário(s) aberto(s)" + if (criar.size < novas.size) " (${novas.size - criar.size} já existiam)." else ".")
}

fun HemareStore.mudarCapacidade(vagaId: Int, nova: Int): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    val v = e.vagas.firstOrNull { it.id == vagaId && it.hospitalId == h.id } ?: return Res.erro("Horário não encontrado.")
    val erro = Agendamento.validarCapacidade(nova, e.reservadas(vagaId))
    if (erro != null) return Res.erro(erro)
    definir(e.copy(vagas = e.vagas.map { if (it.id == v.id) it.copy(capacidade = nova) else it }))
    return Res.ok("")
}

fun HemareStore.apagarVaga(vagaId: Int): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    if (e.vagas.none { it.id == vagaId && it.hospitalId == h.id }) return Res.erro("Horário não encontrado.")
    if (e.reservadas(vagaId) > 0) return Res.erro("Há pessoas agendadas neste horário. Cancele os agendamentos antes de apagar.")
    definir(e.copy(vagas = e.vagas.filter { it.id != vagaId }))
    return Res.ok("")
}

fun HemareStore.mudarStatusAgendamento(agendamentoId: Int, novo: String): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val ag = e.agendamentos.firstOrNull { it.id == agendamentoId } ?: return Res.erro("Agendamento não encontrado.")
    val v = e.vagaPorId(ag.vagaId)?.takeIf { it.hospitalId == h.id } ?: return Res.erro("Agendamento não encontrado.")
    val erro = Agendamento.podeMudarStatus(ag.status, novo, v.data, v.hora)
    if (erro != null) return Res.erro(erro)
    var novoEstado = e.copy(agendamentos = e.agendamentos.map { if (it.id == agendamentoId) it.copy(status = novo) else it })
    if (novo == "compareceu") novoEstado = novoEstado.registrarDoacao(ag.doadorId, h.id, v.data)
    definir(novoEstado)
    return Res.ok(if (novo == "compareceu") "Presença confirmada: a doação entrou na cadeia de confiança." else "Agendamento atualizado.")
}

// ---------------------------------------------------------------------
// Apadrinhamento (lado do hospital)
// ---------------------------------------------------------------------

fun Estado.casosDoHospital(hospitalId: Int): List<Caso> = casos.filter { it.hospitalId == hospitalId && it.ativo }

fun HemareStore.publicarCaso(apelido: String, tipo: String, condicao: String, frequencia: String, autorizacao: Boolean): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val v = Apadrinhamento.validarCaso(apelido, tipo, condicao, frequencia, autorizacao)
    if (v is Validacao.Erro) return Res.erro(v.mensagem)
    val c = (v as Validacao.Ok).valor
    val id = e.proximoId
    definir(e.copy(casos = e.casos + Caso(id, h.id, c.apelido, c.tipoSanguineo, c.condicao, c.frequenciaDias, c.metaPadrinhos, true, null, emptyList()), proximoId = id + 1))
    return Res.ok("Paciente publicado. A meta sugerida é de ${c.metaPadrinhos} padrinhos.", id)
}

fun HemareStore.chamarPadrinhos(casoId: Int): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val caso = e.casos.firstOrNull { it.id == casoId && it.hospitalId == h.id } ?: return Res.erro("Pedido não encontrado.")
    val pode = Apadrinhamento.podeChamarAgora(caso.ultimaChamada)
    if (!pode.pode) return Res.erro("Novo chamado em ${pode.diasRestantes} " + if (pode.diasRestantes == 1) "dia." else "dias.")
    val cands = e.doadores.values.filter { it.id in caso.padrinhos }.map { e.candidato(it) }
    val (convocados, resumo) = Apadrinhamento.selecionarPadrinhos(cands)
    val ids = convocados.map { it.id }.toSet()
    definir(
        e.copy(
            casos = e.casos.map { if (it.id == casoId) it.copy(ultimaChamada = Datas.hoje()) else it },
            doadores = e.doadores.mapValues { (k, d) -> if (k in ids) d.copy(ultimoAlerta = Datas.hoje()) else d }
        )
    )
    return Res.ok(Apadrinhamento.descreverChamada(resumo))
}

fun HemareStore.encerrarCaso(casoId: Int): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    definir(e.copy(casos = e.casos.map { if (it.id == casoId && it.hospitalId == h.id) it.copy(ativo = false) else it }))
    return Res.ok("")
}

// ---------------------------------------------------------------------
// Campanhas de reposição (lado do hospital)
// ---------------------------------------------------------------------

fun Estado.campanhasDoHospital(hospitalId: Int): List<CampanhaReg> = campanhas.filter { it.hospitalId == hospitalId && it.ativo }

fun HemareStore.abrirCampanha(apelido: String, meta: String, prazo: String, autorizacao: Boolean): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val v = Campanha.validar(apelido, meta, prazo, autorizacao)
    if (v is Validacao.Erro) return Res.erro(v.mensagem)
    val c = (v as Validacao.Ok).valor
    var codigo: String
    do { codigo = Raros.gerarCodigo() } while (e.campanhaPorCodigo(codigo) != null)
    definir(e.copy(campanhas = e.campanhas + CampanhaReg(codigo, h.id, c.apelido, c.metaBolsas, Datas.hoje().plusDays(c.prazoDias.toLong()), true, emptyList())))
    return Res.ok("Campanha aberta! Compartilhe o link com a família e os amigos.", 0)
}

fun HemareStore.confirmarPromessa(codigo: String, doadorId: Int): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val c = e.campanhaPorCodigo(codigo)?.takeIf { it.hospitalId == h.id } ?: return Res.erro("Campanha não encontrada.")
    val p = c.promessas.firstOrNull { it.doadorId == doadorId } ?: return Res.erro("Promessa não encontrada.")
    if (p.confirmada) return Res.erro("Esta doação já foi confirmada.")
    val d = e.doadores[doadorId] ?: return Res.erro("Doador não encontrado.")
    val el = Elegibilidade.verificar(e.ultimaDoacaoDe(doadorId), d.sexo)
    if (!el.apto) return Res.erro("Este doador ainda está no intervalo entre doações (faltam ${el.diasRestantes} dias).")
    definir(
        e.registrarDoacao(doadorId, h.id, Datas.hoje(), emCampanha = true).let { novo ->
            novo.copy(campanhas = novo.campanhas.map { camp ->
                if (camp.codigo == c.codigo) camp.copy(promessas = camp.promessas.map { if (it.doadorId == doadorId) it.copy(confirmada = true) else it }) else camp
            })
        }
    )
    return Res.ok("Doação confirmada e contada na meta de ${c.apelido}.")
}

fun HemareStore.encerrarCampanha(codigo: String): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    definir(e.copy(campanhas = e.campanhas.map { if (it.codigo == codigo && it.hospitalId == h.id) it.copy(ativo = false) else it }))
    return Res.ok("")
}

// ---------------------------------------------------------------------
// Rede de sangue raro (lado do hospital)
// ---------------------------------------------------------------------

data class AchadoRaro(val primeiroNome: String, val fenotipoRotulo: String, val status: String)

fun Estado.buscarCodigoRaro(codigo: String): AchadoRaro? {
    val c = codigo.trim().uppercase()
    val d = doadores.values.firstOrNull { it.raro.codigo == c && it.raro.consentimento } ?: return null
    return AchadoRaro(nomeExibido(d.id).split(" ").first(), Raros.rotuloFenotipo(d.raro.fenotipo ?: "") ?: "", d.raro.status ?: "declarado")
}

fun HemareStore.confirmarCodigoRaro(codigo: String): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val c = codigo.trim().uppercase()
    val d = e.doadores.values.firstOrNull { it.raro.codigo == c && it.raro.consentimento } ?: return Res.erro("Código não encontrado.")
    definir(e.copy(doadores = e.doadores + (d.id to d.copy(raro = d.raro.copy(status = "confirmado")))))
    return Res.ok("Fenótipo confirmado. O doador agora faz parte da rede.")
}

private fun Estado.doadoresRaros(): List<Raros.DoadorRaro> = doadores.values.map { d ->
    Raros.DoadorRaro(d.id, d.raro.fenotipo, d.raro.status, d.raro.consentimento, d.raro.alcance, d.cidade, d.estado, d.sexo, ultimaDoacaoDe(d.id), d.raro.ultimaNotificacao)
}

fun Estado.previaRaro(fenotipo: String, hospital: Hospital): Raros.ResumoRaro =
    Raros.selecionar(doadoresRaros(), fenotipo, hospital.cidade, hospital.estado).second

fun Estado.pedidosRarosDoHospital(hospitalId: Int): List<PedidoRaro> = pedidosRaros.filter { it.hospitalId == hospitalId }.sortedByDescending { it.criadoEmMs }

fun HemareStore.criarPedidoRaro(fenotipo: String, motivo: String, apelido: String): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val v = Raros.validarPedido(fenotipo, motivo, apelido)
    if (v is Validacao.Erro) return Res.erro(v.mensagem)
    val p = (v as Validacao.Ok).valor
    val agora = System.currentTimeMillis()
    if (e.pedidosRaros.count { it.hospitalId == h.id && it.aberto && it.expiraEmMs > agora } >= Raros.MAX_PEDIDOS_ABERTOS) {
        return Res.erro("Você já tem ${Raros.MAX_PEDIDOS_ABERTOS} pedidos abertos. Encerre um antes de abrir outro.")
    }
    val (convocados, resumo) = Raros.selecionar(e.doadoresRaros(), p.fenotipo, h.cidade, h.estado)
    val ids = convocados.map { it.id }.toSet()
    val id = e.proximoId
    definir(
        e.copy(
            pedidosRaros = e.pedidosRaros + PedidoRaro(
                id, h.id, p.fenotipo, p.motivo, p.apelidoPaciente, agora, agora + Raros.VALIDADE_PEDIDO_HORAS * 3600000L, true,
                convocados.map { ConvocadoRaro(it.id, null, false) }
            ),
            doadores = e.doadores.mapValues { (k, d) -> if (k in ids) d.copy(raro = d.raro.copy(ultimaNotificacao = Datas.hoje())) else d },
            proximoId = id + 1
        )
    )
    return Res.ok(Raros.descrever(resumo), id)
}

fun HemareStore.confirmarDoacaoRaro(pedidoId: Int, doadorId: Int): Res {
    val e = atual()
    val (h, bloqueio) = e.hospitalLiberado()
    if (bloqueio != null || h == null) return Res.erro(bloqueio ?: "")
    val p = e.pedidosRaros.firstOrNull { it.id == pedidoId && it.hospitalId == h.id } ?: return Res.erro("Pedido não encontrado.")
    val c = p.convocados.firstOrNull { it.doadorId == doadorId && it.resposta == "disponivel" } ?: return Res.erro("Este doador não respondeu \"posso ajudar\".")
    if (c.doacaoConfirmada) return Res.erro("Esta doação já foi confirmada.")
    definir(
        e.registrarDoacao(doadorId, h.id, Datas.hoje()).let { novo ->
            novo.copy(pedidosRaros = novo.pedidosRaros.map { ped ->
                if (ped.id == pedidoId) ped.copy(convocados = ped.convocados.map { if (it.doadorId == doadorId) it.copy(doacaoConfirmada = true) else it }) else ped
            })
        }
    )
    return Res.ok("Doação confirmada e registrada na cadeia de confiança.")
}

fun HemareStore.encerrarPedidoRaro(pedidoId: Int): Res {
    val e = atual()
    val h = e.hospitalAtual ?: return Res.erro("Entre com uma conta de hospital.")
    definir(e.copy(pedidosRaros = e.pedidosRaros.map { if (it.id == pedidoId && it.hospitalId == h.id) it.copy(aberto = false) else it }))
    return Res.ok("Pedido encerrado. Os contatos voltam a ficar protegidos.")
}

// ---------------------------------------------------------------------
// Perfil do hospital
// ---------------------------------------------------------------------

fun HemareStore.salvarPerfilHospital(
    nome: String, cnpj: String, cnes: String, cep: String, endereco: String, numero: String, bairro: String,
    complemento: String, cidade: String, estado: String
): Res {
    val e = atual()
    val u = e.usuario ?: return Res.erro("Entre na sua conta.")
    val h = e.hospitalAtual ?: return Res.erro("Esta conta ainda não tem os dados da instituição.")
    if (listOf(nome, cnpj, cnes, cep, endereco, numero, bairro, cidade, estado).any { it.isBlank() }) return Res.erro("Preencha todos os campos obrigatórios.")
    val mudouCnpj = Documentos.digitos(cnpj) != Documentos.digitos(h.cnpj)
    val mudouCnes = Documentos.digitos(cnes) != Documentos.digitos(h.cnes)
    if (mudouCnpj && !Documentos.cnpjValido(cnpj)) return Res.erro("CNPJ inválido.")
    if (mudouCnes && Documentos.digitos(cnes).length != 7) return Res.erro("O CNES tem 7 números.")
    val n = Auth.validarNomeCadastro(nome)
    if (n is Validacao.Erro) return Res.erro(n.mensagem)
    val voltaParaAnalise = mudouCnpj || mudouCnes || h.status == StatusVerificacao.RECUSADO
    val novo = h.copy(
        cnpj = cnpj, cnes = Documentos.digitos(cnes), cep = cep, endereco = endereco.trim(), numero = numero.trim(), bairro = bairro.trim(),
        complemento = complemento.trim().ifEmpty { null }, cidade = cidade.trim(), estado = estado,
        aprovado = if (voltaParaAnalise) false else h.aprovado, motivoRecusa = if (voltaParaAnalise) null else h.motivoRecusa
    )
    definir(e.copy(hospitais = e.hospitais + (h.id to novo), usuarios = e.usuarios.map { if (it.id == u.id) it.copy(nome = (n as Validacao.Ok).valor) else it }))
    return Res.ok(if (voltaParaAnalise && h.status != StatusVerificacao.PENDENTE) "Dados salvos. O cadastro voltou para análise de um administrador." else "Dados salvos.")
}

// ---------------------------------------------------------------------
// Consultas públicas
// ---------------------------------------------------------------------

data class ItemEstoque(val tipo: String, val hospital: String, val cidade: String, val estado: String, val nivel: String)

/** Termômetro público: só hospitais aprovados, mais urgente primeiro. */
fun Estado.estoquePublico(): List<ItemEstoque> =
    hospitaisAprovados().flatMap { h ->
        (estoque[h.id] ?: emptyMap()).map { (tipo, nivel) -> ItemEstoque(tipo, hospitalNome(h.id), h.cidade, h.estado, nivel) }
    }.sortedWith(compareBy({ Estoque.PESO[it.nivel] ?: 9 }, { it.cidade }, { it.tipo }))

data class ClimaCidade(val cidade: String, val estado: String, val clima: Estoque.Clima, val tipos: List<String>)

/** "Previsão do sangue": o pior nível de estoque da cidade vira um clima. */
fun Estado.climaPorCidade(): List<ClimaCidade> =
    estoquePublico().groupBy { it.cidade to it.estado }.map { (chave, lista) ->
        ClimaCidade(chave.first, chave.second, Estoque.climaDe(lista.map { it.nivel }), lista.map { it.tipo }.distinct())
    }.sortedBy { it.cidade }
