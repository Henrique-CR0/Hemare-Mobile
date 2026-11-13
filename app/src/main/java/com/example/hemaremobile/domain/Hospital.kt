package com.example.hemaremobile.domain

import java.security.MessageDigest
import java.time.LocalDate

/** Candidato a receber um chamado (alerta, padrinho ou sangue raro): o que as regras precisam saber de um doador. */
data class Candidato(
    val id: Int,
    val tipoSanguineo: String,
    val sexo: Char?,
    val cidade: String?,
    val estado: String?,
    val visibilidade: String,
    val ultimaDoacao: LocalDate?,
    val ultimoAlerta: LocalDate?
)

data class ResumoAlerta(val compativeis: Int, val aptos: Int, val naCidade: Int, val convocados: Int, val poupados: Int)
data class SelecaoAlerta(val convocados: List<Candidato>, val resumo: ResumoAlerta)

/** Alerta de emergência inteligente (backend/regras/alerta.js): só chama quem pode ajudar de verdade. */
object Alerta {
    const val DIAS_ENTRE_ALERTAS = 3

    fun selecionar(doadores: List<Candidato>, tiposCompativeis: List<String>, cidadeHospital: String, hoje: LocalDate = Datas.hoje()): SelecaoAlerta {
        val cidadeAlvo = Textos.normalizarCidade(cidadeHospital)
        val compativeis = doadores.filter { tiposCompativeis.contains(it.tipoSanguineo) && it.visibilidade == "identificado" }
        val aptos = compativeis.filter { Elegibilidade.verificar(it.ultimaDoacao, it.sexo, hoje).apto }
        val naCidade = aptos.filter { cidadeAlvo.isNotEmpty() && Textos.normalizarCidade(it.cidade) == cidadeAlvo }
        val convocados = naCidade.filter { it.ultimoAlerta == null || Datas.diasEntre(it.ultimoAlerta, hoje) >= DIAS_ENTRE_ALERTAS }
        return SelecaoAlerta(
            convocados,
            ResumoAlerta(compativeis.size, aptos.size, naCidade.size, convocados.size, naCidade.size - convocados.size)
        )
    }

    fun descrever(r: ResumoAlerta, cidade: String): String = when {
        r.convocados > 0 -> {
            val pessoas = if (r.convocados == 1) "1 doador compatível e apto" else "${r.convocados} doadores compatíveis e aptos"
            "🚨 Alerta enviado para $pessoas em $cidade."
        }
        r.naCidade > 0 -> "Os doadores aptos em $cidade já receberam um alerta nos últimos $DIAS_ENTRE_ALERTAS dias; por isso não foram chamados de novo agora."
        else -> "Nenhum doador compatível, identificado e apto hoje em $cidade. A necessidade fica publicada e visível no painel."
    }
}

/** Cadeia de confiança: cada doação carrega um hash ligado ao da doação anterior (backend/regras/cadeia.js). */
object Cadeia {
    val GENESIS = "0".repeat(64)

    fun calcularHash(hashAnterior: String, doadorId: Int, hospitalId: Int, data: LocalDate): String {
        val conteudo = "$hashAnterior|$doadorId|$hospitalId|$data"
        return MessageDigest.getInstance("SHA-256").digest(conteudo.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    data class Elo(val id: Int, val doadorId: Int, val hospitalId: Int, val data: LocalDate, val hash: String)
    data class Resultado(val total: Int, val integra: Boolean, val primeiroInvalidoId: Int?)

    /** Recalcula a cadeia inteira, na ordem das doações: qualquer registro alterado ou apagado quebra o resto. */
    fun verificar(elos: List<Elo>): Resultado {
        var anterior = GENESIS
        for (e in elos.sortedBy { it.id }) {
            if (calcularHash(anterior, e.doadorId, e.hospitalId, e.data) != e.hash) return Resultado(elos.size, false, e.id)
            anterior = e.hash
        }
        return Resultado(elos.size, true, null)
    }
}

enum class StatusVerificacao { PENDENTE, APROVADO, RECUSADO }

/** Níveis de estoque e urgência (necessidade.js, estoque do hospital e "clima" do sangue). */
object Estoque {
    val NIVEIS = listOf("estavel" to "🟢 Estável", "alerta" to "🟡 Alerta", "critico" to "🔴 Crítico", "emergencia" to "⚫ Emergência")
    val URGENCIAS_DETALHADAS = listOf(
        "estavel" to "🟢 Estável — reposição de rotina",
        "alerta" to "🟡 Alerta — estoque baixo",
        "critico" to "🔴 Crítico — poucos dias de estoque",
        "emergencia" to "⚫ Emergência — situação extrema"
    )
    val PESO = mapOf("emergencia" to 0, "critico" to 1, "alerta" to 2, "estavel" to 3)

    fun rotulo(nivel: String): String = NIVEIS.firstOrNull { it.first == nivel }?.second ?: nivel

    /** "Previsão do sangue": o pior nível de estoque da cidade vira um clima. */
    data class Clima(val icone: String, val rotulo: String)

    fun climaDe(niveis: List<String>): Clima {
        val pior = niveis.maxOfOrNull { mapOf("emergencia" to 4, "critico" to 3, "alerta" to 2, "estavel" to 1)[it] ?: 1 } ?: 1
        return when (pior) {
            4 -> Clima("⛈️", "Emergência")
            3 -> Clima("🌧️", "Crítico")
            2 -> Clima("🌥️", "Atenção")
            else -> Clima("☀️", "Estável")
        }
    }
}

data class ResumoRadar(val tipo: String, val aptoAgora: Int, val em7Dias: Int, val em30Dias: Int, val maisDe30Dias: Int)

object Radar {
    /** Quantos doadores de cada tipo estão aptos agora, em até 7 e em até 30 dias (só números). */
    fun calcular(doadores: List<Candidato>, hoje: LocalDate = Datas.hoje()): List<ResumoRadar> {
        val ordem = Compatibilidade.TIPOS_PEDIDO
        return doadores.filter { it.sexo == 'M' || it.sexo == 'F' }.groupBy { it.tipoSanguineo }.map { (tipo, lista) ->
            var agora = 0; var em7 = 0; var em30 = 0; var mais = 0
            lista.forEach {
                val e = Elegibilidade.verificar(it.ultimaDoacao, it.sexo, hoje)
                val dias = e.diasRestantes ?: 0
                when {
                    e.apto -> agora++
                    dias <= 7 -> em7++
                    dias <= 30 -> em30++
                    else -> mais++
                }
            }
            ResumoRadar(tipo, agora, em7, em30, mais)
        }.sortedBy { ordem.indexOf(it.tipo).let { i -> if (i < 0) 99 else i } }
    }
}

/** Regras da agenda de doação (backend/regras/agendamento.js). */
object Agendamento {
    const val ANTECEDENCIA_MIN_HORAS = 2
    const val MAX_DIAS_A_FRENTE = 60
    const val MAX_DIAS_DO_LOTE = 62
    const val MAX_VAGAS_POR_LOTE = 600
    const val CAPACIDADE_MAXIMA = 20
    const val MAX_HORARIOS_POR_DIA = 24
    const val HORA_MIN = "05:00"
    const val HORA_MAX = "22:00"

    val NOMES_STATUS = mapOf("agendado" to "Agendado", "compareceu" to "Compareceu", "faltou" to "Faltou", "cancelado" to "Cancelado")
    val ICONES_STATUS = mapOf("agendado" to "📅", "compareceu" to "🩸", "faltou" to "⚠️", "cancelado" to "✖️")

    fun horaValida(hora: String): Boolean =
        Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(hora) && hora >= HORA_MIN && hora <= HORA_MAX

    /** Instante exato (ms) de uma vaga, no horário de Brasília (UTC-3). */
    fun momentoDaVaga(data: LocalDate, hora: String): Long {
        val h = hora.split(":")
        return data.atTime(h[0].toInt(), h[1].toInt()).toInstant(java.time.ZoneOffset.of("-03:00")).toEpochMilli()
    }

    data class VagaNova(val data: LocalDate, val hora: String, val capacidade: Int)

    /** Abre vagas em lote. diasSemana: 0 = domingo ... 6 = sábado. */
    fun validarLote(
        de: LocalDate?, ate: LocalDate?, diasSemana: List<Int>, horarios: List<String>, capacidade: Int, hoje: LocalDate = Datas.hoje()
    ): Validacao<List<VagaNova>> {
        if (de == null || ate == null) return Validacao.Erro("Informe as datas inicial e final.")
        if (de.isBefore(hoje)) return Validacao.Erro("A data inicial não pode ser no passado.")
        if (ate.isBefore(de)) return Validacao.Erro("A data final precisa ser igual ou depois da inicial.")
        if (Datas.diasEntre(de, ate) > MAX_DIAS_DO_LOTE) return Validacao.Erro("Abra no máximo $MAX_DIAS_DO_LOTE dias por vez.")
        if (Datas.diasEntre(hoje, ate) > MAX_DIAS_A_FRENTE) return Validacao.Erro("As vagas só podem ser abertas até $MAX_DIAS_A_FRENTE dias à frente.")
        if (diasSemana.isEmpty()) return Validacao.Erro("Escolha os dias da semana.")
        if (horarios.isEmpty()) return Validacao.Erro("Informe ao menos um horário.")
        if (horarios.size > MAX_HORARIOS_POR_DIA) return Validacao.Erro("No máximo $MAX_HORARIOS_POR_DIA horários por dia.")
        if (!horarios.all { horaValida(it) }) return Validacao.Erro("Horários devem estar no formato HH:MM, entre $HORA_MIN e $HORA_MAX.")
        if (capacidade < 1 || capacidade > CAPACIDADE_MAXIMA) return Validacao.Erro("A capacidade de cada horário deve ser de 1 a $CAPACIDADE_MAXIMA pessoas.")

        val horas = horarios.distinct().sorted()
        val vagas = mutableListOf<VagaNova>()
        var d: LocalDate = de
        while (!d.isAfter(ate)) {
            if (diasSemana.contains(d.dayOfWeek.value % 7)) horas.forEach { vagas.add(VagaNova(d, it, capacidade)) }
            d = d.plusDays(1)
        }
        if (vagas.isEmpty()) return Validacao.Erro("Nenhum dia do período cai nos dias da semana escolhidos.")
        if (vagas.size > MAX_VAGAS_POR_LOTE) return Validacao.Erro("Isso daria ${vagas.size} vagas; o máximo é $MAX_VAGAS_POR_LOTE por vez.")
        return Validacao.Ok(vagas)
    }

    fun validarCapacidade(valor: Int, reservadas: Int): String? = when {
        valor < 1 || valor > CAPACIDADE_MAXIMA -> "A capacidade deve ser de 1 a $CAPACIDADE_MAXIMA pessoas."
        valor < reservadas -> "Já há $reservadas pessoa(s) agendada(s) neste horário. Cancele os agendamentos antes de reduzir."
        else -> null
    }

    fun doacoesNosUltimos12Meses(datas: List<LocalDate>, hoje: LocalDate): Int =
        datas.count { val n = Datas.diasEntre(it, hoje); n in 0..364 }

    /** Pode agendar esta vaga? Devolve a frase de erro ou null se pode. */
    fun podeAgendar(
        sexo: Char?, ultimaDoacao: LocalDate?, datasDeDoacao: List<LocalDate>, vagaData: LocalDate, vagaHora: String,
        capacidade: Int, reservadas: Int, ativo: Pair<LocalDate, String>?, agoraMs: Long = System.currentTimeMillis()
    ): String? {
        if (sexo != 'M' && sexo != 'F') return "Complete seu perfil (informe o sexo) para agendar."
        val hoje = Datas.hoje()
        if (momentoDaVaga(vagaData, vagaHora) < agoraMs + ANTECEDENCIA_MIN_HORAS * 3600 * 1000L) {
            return "Escolha um horário com pelo menos $ANTECEDENCIA_MIN_HORAS horas de antecedência."
        }
        if (Datas.diasEntre(hoje, vagaData) > MAX_DIAS_A_FRENTE) return "Só dá para agendar até $MAX_DIAS_A_FRENTE dias à frente."
        if (reservadas >= capacidade) return "Este horário já está lotado. Escolha outro."
        if (ativo != null) {
            return "Você já tem um agendamento (${Datas.dm(ativo.first)} às ${ativo.second}). Reagende ou cancele antes de marcar outro."
        }
        val aptoEm = Elegibilidade.aptoEmDia(ultimaDoacao, sexo, vagaData)
        if (aptoEm != null && aptoEm.isAfter(vagaData)) {
            return "Nesta data você ainda não estará apto. Você fica apto em ${Datas.dm(aptoEm)}: escolha esse dia ou depois."
        }
        val limite = Elegibilidade.LIMITE_ANUAL[sexo] ?: 3
        if (doacoesNosUltimos12Meses(datasDeDoacao, vagaData) >= limite) {
            return "Você já atingiu o limite de $limite doações em 12 meses. Escolha uma data mais à frente."
        }
        return null
    }

    fun podeCancelar(status: String, vagaData: LocalDate, vagaHora: String, agoraMs: Long = System.currentTimeMillis()): String? = when {
        status != "agendado" -> "Este agendamento já foi encerrado."
        momentoDaVaga(vagaData, vagaHora) <= agoraMs -> "O horário já passou."
        else -> null
    }

    fun podeMudarStatus(statusAtual: String, novo: String, vagaData: LocalDate, vagaHora: String, agoraMs: Long = System.currentTimeMillis()): String? = when {
        novo !in listOf("compareceu", "faltou", "cancelado") -> "Status inválido."
        statusAtual != "agendado" -> "Este agendamento já foi encerrado ($statusAtual)."
        novo == "compareceu" && vagaData != Datas.hoje() -> "Só dá para confirmar a presença no dia da doação."
        novo == "faltou" && momentoDaVaga(vagaData, vagaHora) > agoraMs -> "O horário ainda não passou."
        else -> null
    }

    // ----- histórico "Minhas doações" -----
    data class ItemDoacao(val data: LocalDate, val hospital: String, val cidade: String, val hash: String?)
    data class ItemAgendamento(val id: Int, val status: String, val data: LocalDate, val hora: String, val hospital: String, val cidade: String)
    data class LinhaDoTempo(
        val tipo: String, val data: LocalDate, val hora: String?, val status: String?, val hospital: String, val cidade: String, val selo: String?
    )
    data class ResumoHistorico(
        val totalDoacoes: Int, val vidasAte: Int, val ultimaDoacao: LocalDate?, val aptoAgora: Boolean, val aptoEm: LocalDate?,
        val nosUltimos12Meses: Int, val limiteAnual: Int?, val restantesNoAno: Int?, val porAno: Map<String, Int>,
        val compareceu: Int, val faltou: Int, val taxaDeComparecimento: Int?, val proximo: ItemAgendamento?
    )
    data class Historico(val resumo: ResumoHistorico, val linhaDoTempo: List<LinhaDoTempo>)

    fun montarHistorico(doacoes: List<ItemDoacao>, agendamentos: List<ItemAgendamento>, sexo: Char?, hoje: LocalDate = Datas.hoje()): Historico {
        val lista = doacoes.sortedByDescending { it.data }
        val total = lista.size
        val ultima = lista.firstOrNull()?.data
        val limite = Elegibilidade.LIMITE_ANUAL[sexo]
        val noAno = doacoesNosUltimos12Meses(lista.map { it.data }, hoje)
        val intervalo = Elegibilidade.INTERVALO_DIAS[sexo]
        val aptoEm = if (intervalo == null) null else if (ultima != null) ultima.plusDays(intervalo.toLong()) else hoje
        val porAno = lista.groupingBy { it.data.year.toString() }.eachCount()
        val compareceu = agendamentos.count { it.status == "compareceu" }
        val faltou = agendamentos.count { it.status == "faltou" }
        val encerrados = compareceu + faltou
        val proximo = agendamentos.filter { it.status == "agendado" }.minWithOrNull(compareBy({ it.data }, { it.hora }))

        val linha = (lista.map { LinhaDoTempo("doacao", it.data, null, null, it.hospital, it.cidade, it.hash?.take(12)) } +
            agendamentos.filter { it.status != "compareceu" }.map { LinhaDoTempo("agendamento", it.data, it.hora, it.status, it.hospital, it.cidade, null) })
            .sortedByDescending { it.data }

        return Historico(
            ResumoHistorico(
                totalDoacoes = total, vidasAte = total * Gamificacao.VIDAS_POR_DOACAO, ultimaDoacao = ultima,
                aptoAgora = aptoEm != null && !aptoEm.isAfter(hoje), aptoEm = aptoEm, nosUltimos12Meses = noAno, limiteAnual = limite,
                restantesNoAno = limite?.let { maxOf(0, it - noAno) }, porAno = porAno, compareceu = compareceu, faltou = faltou,
                taxaDeComparecimento = if (encerrados > 0) Math.round(compareceu * 100f / encerrados) else null, proximo = proximo
            ),
            linha
        )
    }

    fun proximoPasso(r: ResumoHistorico): String = when {
        r.proximo != null -> "Seu próximo agendamento é em ${Datas.dm(r.proximo.data)} às ${r.proximo.hora}."
        r.restantesNoAno == 0 -> "Você atingiu o limite de doações em 12 meses. Você volta a poder doar quando a doação mais antiga completar 1 ano."
        r.aptoAgora -> "Você já pode doar! Que tal agendar um horário?"
        r.aptoEm != null -> "Você fica apto em ${Datas.dm(r.aptoEm)}. Já dá para agendar para esse dia ou depois."
        else -> "Complete seu perfil para ver quando você pode doar."
    }
}
