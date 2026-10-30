package com.example.hemaremobile.domain

import java.time.LocalDate

/** Apadrinhamento de pacientes (backend/regras/apadrinhamento.js). O caso é público: só apelido, tipo, condição e frequência. */
object Apadrinhamento {
    val CONDICOES = listOf(
        "Não informada", "Anemia falciforme", "Talassemia", "Hemofilia", "Tratamento oncológico", "Aplasia de medula", "Outra condição"
    )
    const val FREQUENCIA_MINIMA = 14
    const val FREQUENCIA_MAXIMA = 120
    const val META_MAXIMA = 30
    const val MAX_AFILHADOS = 3
    const val DIAS_ENTRE_CHAMADAS = 7

    /** Quantos padrinhos cobrem o paciente: cada padrinho doa no máximo a cada ~90 dias e cada transfusão usa ~2 bolsas. */
    fun sugerirMeta(frequenciaDias: Int): Int {
        val meta = Math.ceil(90.0 / frequenciaDias).toInt() * 2
        return minOf(META_MAXIMA, maxOf(2, meta))
    }

    /** Apelido público de um paciente (vale para apadrinhamento e campanhas): nunca o nome verdadeiro. */
    fun validarApelido(texto: String): Validacao<String> {
        val apelido = texto.trim().replace(Regex("\\s+"), " ")
        if (apelido.length < 2 || apelido.length > 30) return Validacao.Erro("Dê um apelido de 2 a 30 caracteres (nunca o nome verdadeiro).")
        if (!Regex("^[\\p{L}0-9 -]+$").matches(apelido)) return Validacao.Erro("O apelido só pode ter letras, números, espaço e hífen.")
        if (apelido.count { it.isDigit() } > 3) return Validacao.Erro("O apelido não pode ter documentos ou telefones. Use algo como \"Paciente Aurora\".")
        return Validacao.Ok(apelido)
    }

    data class CasoValidado(val apelido: String, val tipoSanguineo: String, val condicao: String, val frequenciaDias: Int, val metaPadrinhos: Int)

    fun validarCaso(apelido: String, tipo: String, condicao: String, frequencia: String, autorizacao: Boolean): Validacao<CasoValidado> {
        if (!autorizacao) return Validacao.Erro("Confirme que o paciente ou o responsável autorizou divulgar este pedido.")
        val a = validarApelido(apelido)
        if (a is Validacao.Erro) return a
        if (Compatibilidade.doadoresCompativeis(tipo).isEmpty()) return Validacao.Erro("Escolha o tipo sanguíneo do paciente (O-, O+, A-, A+, B-, B+, AB- ou AB+).")
        val cond = if (condicao.isBlank()) CONDICOES[0] else condicao
        if (!CONDICOES.contains(cond)) return Validacao.Erro("Escolha uma condição da lista.")
        val freq = frequencia.trim().toIntOrNull()
        if (freq == null || freq < FREQUENCIA_MINIMA || freq > FREQUENCIA_MAXIMA) {
            return Validacao.Erro("Informe de quantos em quantos dias o paciente precisa de sangue ($FREQUENCIA_MINIMA a $FREQUENCIA_MAXIMA).")
        }
        return Validacao.Ok(CasoValidado((a as Validacao.Ok).valor, tipo, cond, freq, sugerirMeta(freq)))
    }

    /** Um doador pode apadrinhar este paciente? Devolve a frase de erro ou null. */
    fun podeApadrinhar(tipoDoador: String, tipoPaciente: String, afilhadosAtuais: Int, jaEPadrinho: Boolean, casoAtivo: Boolean): String? = when {
        !casoAtivo -> "Este pedido já foi encerrado."
        jaEPadrinho -> "Você já é padrinho ou madrinha deste paciente."
        !Compatibilidade.podeDoar(tipoDoador, tipoPaciente) -> "Seu tipo sanguíneo ($tipoDoador) não é compatível com $tipoPaciente."
        afilhadosAtuais >= MAX_AFILHADOS -> "Você já apadrinha $MAX_AFILHADOS pacientes, o máximo por pessoa."
        else -> null
    }

    data class Meta(val padrinhos: Int, val meta: Int, val porcentagem: Int, val completo: Boolean)

    fun situacaoMeta(padrinhos: Int, meta: Int): Meta {
        val qtd = maxOf(0, padrinhos)
        val alvo = maxOf(1, meta)
        return Meta(qtd, alvo, minOf(100, Math.round(qtd * 100f / alvo)), qtd >= alvo)
    }

    data class Chamada(val pode: Boolean, val diasRestantes: Int)

    fun podeChamarAgora(ultimaChamada: LocalDate?, hoje: LocalDate = Datas.hoje()): Chamada {
        if (ultimaChamada == null) return Chamada(true, 0)
        val passados = Datas.diasEntre(ultimaChamada, hoje)
        return if (passados >= DIAS_ENTRE_CHAMADAS) Chamada(true, 0) else Chamada(false, DIAS_ENTRE_CHAMADAS - passados)
    }

    data class ResumoChamada(val padrinhos: Int, val aptos: Int, val convocados: Int)

    /** Quem chamar: apto hoje e que não recebeu outro chamado nos últimos dias. */
    fun selecionarPadrinhos(padrinhos: List<Candidato>, hoje: LocalDate = Datas.hoje()): Pair<List<Candidato>, ResumoChamada> {
        val aptos = padrinhos.filter { Elegibilidade.verificar(it.ultimaDoacao, it.sexo, hoje).apto }
        val convocados = aptos.filter { it.ultimoAlerta == null || Datas.diasEntre(it.ultimoAlerta, hoje) >= Alerta.DIAS_ENTRE_ALERTAS }
        return convocados to ResumoChamada(padrinhos.size, aptos.size, convocados.size)
    }

    fun descreverChamada(r: ResumoChamada): String = when {
        r.convocados > 0 -> "💌 Chamado enviado para " + (if (r.convocados == 1) "1 padrinho apto" else "${r.convocados} padrinhos aptos") + "."
        r.padrinhos == 0 -> "Este paciente ainda não tem padrinhos."
        r.aptos > 0 -> "Os padrinhos aptos já receberam um chamado nos últimos ${Alerta.DIAS_ENTRE_ALERTAS} dias."
        else -> "Nenhum padrinho está apto a doar hoje (todos ainda estão no intervalo entre doações)."
    }
}

/** Campanhas de reposição "Quem doa por mim" (backend/regras/campanha.js). */
object Campanha {
    const val META_MAXIMA = 30
    const val PRAZO_PADRAO = 15
    const val PRAZO_MAXIMO = 60
    const val JANELA_PROMESSA = 30

    val ROTULOS = mapOf(
        "aberta" to "🟢 Aberta", "meta-atingida" to "🎉 Meta atingida", "expirada" to "⏳ Prazo encerrado", "encerrada" to "✔️ Encerrada"
    )

    data class CampanhaValidada(val apelido: String, val metaBolsas: Int, val prazoDias: Int)

    fun validar(apelido: String, meta: String, prazo: String, autorizacao: Boolean): Validacao<CampanhaValidada> {
        if (!autorizacao) return Validacao.Erro("Confirme que o paciente ou a família autorizou divulgar este pedido.")
        val a = Apadrinhamento.validarApelido(apelido)
        if (a is Validacao.Erro) return a
        val m = meta.trim().toIntOrNull()
        if (m == null || m < 1 || m > META_MAXIMA) return Validacao.Erro("Informe quantas bolsas o hospital pediu (de 1 a $META_MAXIMA).")
        var p = PRAZO_PADRAO
        if (prazo.isNotBlank()) {
            p = prazo.trim().toIntOrNull() ?: -1
            if (p < 1 || p > PRAZO_MAXIMO) return Validacao.Erro("O prazo da campanha deve ser de 1 a $PRAZO_MAXIMO dias.")
        }
        return Validacao.Ok(CampanhaValidada((a as Validacao.Ok).valor, m, p))
    }

    data class Situacao(
        val status: String, val metaBolsas: Int, val confirmadas: Int, val prometidas: Int, val faltam: Int,
        val porcentagem: Int, val diasRestantes: Int, val aceitaPromessas: Boolean
    )

    fun situacao(metaBolsas: Int, confirmadas: Int, prometidas: Int, expiraEm: LocalDate, ativo: Boolean, hoje: LocalDate = Datas.hoje()): Situacao {
        val meta = maxOf(1, metaBolsas)
        val conf = maxOf(0, confirmadas)
        val status = when {
            !ativo -> "encerrada"
            Datas.diasEntre(hoje, expiraEm) < 0 -> "expirada"
            conf >= meta -> "meta-atingida"
            else -> "aberta"
        }
        return Situacao(
            status, meta, conf, maxOf(0, prometidas), maxOf(0, meta - conf), minOf(100, Math.round(conf * 100f / meta)),
            maxOf(0, Datas.diasEntre(hoje, expiraEm)), status == "aberta" || status == "meta-atingida"
        )
    }

    /** Um doador pode prometer doar nesta data? Devolve a frase de erro ou null. */
    fun validarPromessa(
        data: LocalDate?, situacao: Situacao, expiraEm: LocalDate, jaPrometeu: Boolean, ultimaDoacao: LocalDate?, sexo: Char?,
        hoje: LocalDate = Datas.hoje()
    ): String? {
        if (!situacao.aceitaPromessas) return "Esta campanha não está mais recebendo promessas."
        if (jaPrometeu) return "Você já prometeu doar por este paciente."
        if (data == null) return "Escolha a data em que você vai doar."
        if (Datas.diasEntre(hoje, data) < 0) return "A data não pode ser no passado."
        if (Datas.diasEntre(hoje, data) > JANELA_PROMESSA) return "Escolha uma data dentro dos próximos $JANELA_PROMESSA dias."
        if (Datas.diasEntre(data, expiraEm) < 0) return "A campanha termina antes dessa data. Escolha um dia até ${Datas.dmy(expiraEm)}."
        val e = Elegibilidade.verificar(ultimaDoacao, sexo, data)
        if (!e.apto) {
            return if (e.diasRestantes != null) "Nessa data você ainda estará no intervalo entre doações (faltam ${e.diasRestantes} dias). Escolha uma data mais à frente."
            else "Informe o sexo no seu perfil para conferir a data."
        }
        return null
    }

    fun mensagemCompartilhar(apelido: String, hospital: String, cidade: String, faltam: Int, link: String): String {
        val quanto = if (faltam > 0) "Faltam $faltam " + (if (faltam == 1) "doação" else "doações") + ". " else "A meta já foi atingida, mas toda doação ajuda! "
        return "🩸 Doe por $apelido. O $hospital ($cidade) pediu reposição de sangue. ${quanto}Prometa sua doação e acompanhe o progresso: $link"
    }
}

/** Rede de sangue raro (backend/regras/raros.js). */
object Raros {
    val FENOTIPOS = listOf(
        "rh-nulo" to "Rh nulo (sangue dourado)", "bombay" to "Bombay (Oh)", "vel-negativo" to "Vel negativo",
        "jk-nulo" to "Jk(a-b-) (Kidd nulo)", "kell-nulo" to "Kell nulo (K0)"
    )
    val ALCANCES = listOf("cidade" to "Só na minha cidade", "estado" to "No meu estado", "pais" to "Em qualquer lugar do Brasil")
    const val DIAS_ENTRE_CHAMADOS = 7
    const val VALIDADE_PEDIDO_HORAS = 72
    const val MAX_PEDIDOS_ABERTOS = 3

    fun rotuloFenotipo(valor: String): String? = FENOTIPOS.firstOrNull { it.first == valor }?.second

    data class PedidoValidado(val fenotipo: String, val motivo: String, val apelidoPaciente: String?)

    fun validarPedido(fenotipo: String, motivo: String, apelido: String): Validacao<PedidoValidado> {
        if (FENOTIPOS.none { it.first == fenotipo }) return Validacao.Erro("Escolha o fenótipo que o paciente precisa.")
        val m = Textos.limpar(motivo)
        if (m.length < 20 || m.length > 300) return Validacao.Erro("Descreva a emergência em 20 a 300 caracteres (sem nome do paciente). O registro fica guardado para auditoria.")
        var ap: String? = null
        if (Textos.limpar(apelido).isNotEmpty()) {
            val a = Apadrinhamento.validarApelido(apelido)
            if (a is Validacao.Erro) return a
            ap = (a as Validacao.Ok).valor
        }
        return Validacao.Ok(PedidoValidado(fenotipo, m, ap))
    }

    data class DoadorRaro(
        val id: Int, val fenotipo: String?, val status: String?, val consentimento: Boolean, val alcance: String?,
        val cidade: String?, val estado: String?, val sexo: Char?, val ultimaDoacao: LocalDate?, val ultimaNotificacao: LocalDate?
    )

    fun noAlcance(d: DoadorRaro, cidadeHospital: String, estadoHospital: String?): Boolean {
        if (d.alcance == "pais") return true
        val mesmaCidade = Textos.normalizarCidade(d.cidade).isNotEmpty() && Textos.normalizarCidade(d.cidade) == Textos.normalizarCidade(cidadeHospital)
        if (d.alcance == "cidade") return mesmaCidade
        if (d.alcance == "estado") {
            val a = (d.estado ?: "").uppercase()
            val b = (estadoHospital ?: "").uppercase()
            return if (a.isNotEmpty() && b.isNotEmpty()) a == b else mesmaCidade
        }
        return false
    }

    data class ResumoRaro(val naRede: Int, val noAlcance: Int, val aptos: Int, val convocados: Int)

    fun selecionar(doadores: List<DoadorRaro>, fenotipo: String, cidadeHospital: String, estadoHospital: String?, hoje: LocalDate = Datas.hoje()): Pair<List<DoadorRaro>, ResumoRaro> {
        val confirmados = doadores.filter { it.fenotipo == fenotipo && it.status == "confirmado" && it.consentimento }
        val alcance = confirmados.filter { noAlcance(it, cidadeHospital, estadoHospital) }
        val aptos = alcance.filter { Elegibilidade.verificar(it.ultimaDoacao, it.sexo, hoje).apto }
        val convocados = aptos.filter { it.ultimaNotificacao == null || Datas.diasEntre(it.ultimaNotificacao, hoje) >= DIAS_ENTRE_CHAMADOS }
        return convocados to ResumoRaro(confirmados.size, alcance.size, aptos.size, convocados.size)
    }

    fun descrever(r: ResumoRaro): String = when {
        r.convocados > 0 -> "🆘 Chamado enviado para " + (if (r.convocados == 1) "1 doador compatível" else "${r.convocados} doadores compatíveis") +
            ". Quem responder \"posso ajudar\" aparece aqui com nome e telefone."
        r.naRede == 0 -> "Ainda não há doadores confirmados com esse fenótipo na rede."
        r.noAlcance == 0 -> "Há doadores na rede, mas nenhum que possa chegar até a sua cidade."
        r.aptos == 0 -> "Há doadores ao alcance, mas nenhum está apto a doar hoje (intervalo entre doações)."
        else -> "Os doadores aptos já receberam um chamado de sangue raro nos últimos $DIAS_ENTRE_CHAMADOS dias."
    }

    /** Código curto para o doador mostrar ao hospital (mesmo alfabeto do convite: sem 0/O e 1/I). */
    const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun gerarCodigo(aleatorio: java.util.Random = java.util.Random()): String =
        (1..8).map { ALFABETO[aleatorio.nextInt(ALFABETO.length)] }.joinToString("")
}
