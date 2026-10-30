package com.example.hemaremobile.domain

/** Ficha PcD (backend/regras/pcd.js): orientação a partir da Portaria GM/MS nº 11.685/2026. Quem decide é sempre a equipe do hemocentro. */
object Pcd {
    val TIPOS = listOf(
        "fisica" to "Física (mobilidade, amputação, paralisia)", "visual" to "Visual (baixa visão ou cegueira)",
        "auditiva" to "Auditiva (surdez ou deficiência auditiva)", "intelectual" to "Intelectual",
        "psicossocial" to "Psicossocial (saúde mental)", "tea" to "Transtorno do espectro autista (TEA)",
        "multipla" to "Múltipla (mais de uma)"
    )
    val APOIOS = listOf(
        "mobilidade" to "Cadeira de rodas ou ajuda para me deslocar", "libras" to "Intérprete de Libras",
        "leitura" to "Ajuda para ler e assinar (leitura em voz alta, letra grande, braile)",
        "linguagem-simples" to "Explicação em linguagem simples, com calma", "acompanhante" to "Ir com um acompanhante de confiança",
        "prioridade" to "Atendimento prioritário"
    )
    val CAUSAS = listOf(
        "nascenca" to "De nascença, genética ou sensorial (surdez, baixa visão, cegueira), sem doença ativa",
        "paralisia-cerebral" to "Paralisia cerebral", "lesao-trauma" to "Acidente ou trauma (lesão medular, politrauma)",
        "amputacao" to "Amputação", "avc" to "AVC (derrame) no passado",
        "neuro-lista" to "Esclerose múltipla ou em placa, ELA, miastenia gravis, neurofibromatose forma maior, hematoma com sequela ou leucoencefalopatia multifocal progressiva",
        "epilepsia" to "Epilepsia", "autoimune" to "Doença autoimune que afeta mais de um órgão (lúpus, artrite reumatoide...)",
        "infecciosa" to "Sequela de infecção (por exemplo, meningite ou poliomielite)", "psiquiatrica" to "Condição psiquiátrica",
        "outra" to "Outra causa ou não sei"
    )
    val MEDICAMENTOS = listOf(
        "anticonvulsivante-convulsao" to "Anticonvulsivante para controlar convulsões",
        "anticonvulsivante-outro" to "Anticonvulsivante para outra coisa (dor, enxaqueca, humor)",
        "antipsicotico" to "Antipsicótico (haloperidol, quetiapina, clorpromazina...)", "antidepressivo" to "Antidepressivo",
        "ansiolitico" to "Ansiolítico ou remédio para dormir", "anticoagulante" to "Anticoagulante (varfarina, rivaroxabana, apixabana...)",
        "antiagregante" to "Antiagregante plaquetário (clopidogrel, ticagrelor...)", "imunossupressor" to "Imunossupressor ou imunobiológico",
        "corticoide" to "Corticoide por via oral ou injetável", "outro" to "Outro remédio de uso contínuo"
    )
    val DECISOES = listOf(
        "sozinho" to "Eu entendo e decido sozinho(a) sobre a minha saúde",
        "com-apoio" to "Eu decido, com o apoio de alguém de confiança para entender",
        "por-responsavel" to "Outra pessoa (curador ou responsável legal) decide por mim e eu não consigo compreender o processo de doação"
    )
    val SITUACOES_EPILEPSIA = listOf(
        "em-tratamento" to "Estou em tratamento com anticonvulsivante",
        "menos-3-anos" to "Parei o tratamento (com meu médico) há menos de 3 anos, ou tive crise nesse período",
        "tres-anos-ou-mais" to "Parei o tratamento (com meu médico) há 3 anos ou mais, sem nenhuma crise"
    )

    const val NOTA_PRATICA = "Cada hemocentro aplica a norma com a sua equipe e alguns são mais restritivos do que a norma federal " +
        "(o Hemominas, por exemplo, lista a síndrome de Down como inaptidão definitiva). Em 01/10/2025 o Ministério Público Federal " +
        "entrou na Justiça contra a exclusão automática de pessoas com deficiência neurológica. Se você receber um \"não\", peça o motivo " +
        "e o prazo: o serviço é obrigado a explicar e a registrar na ficha de triagem (art. 69)."
    const val AVISO = "Esta ficha é uma orientação feita a partir da norma federal. Não é diagnóstico nem autorização: quem decide é a equipe de triagem do hemocentro."
    const val FONTE = "Portaria GM/MS nº 11.685, de 2 de julho de 2026 (altera o Anexo IV da Portaria de Consolidação GM/MS nº 5/2017)"
    const val INTRO = "Pessoas com deficiência podem doar sangue, desde que estejam bem de saúde e atendam aos requisitos gerais da triagem. " +
        "Na norma federal, a deficiência por si só não é impedimento: o que conta é a causa, os remédios e a capacidade de " +
        "entender e consentir. Quem decide é sempre a equipe do hemocentro."

    data class ItemFicha(val nivel: String, val texto: String, val base: String?) // nivel: ok | amarelo | vermelho

    data class ResultadoFicha(
        val nivel: String, val titulo: String, val resumo: String, val avaliacao: List<ItemFicha>, val apoios: List<String>,
        val levar: List<String>, val depois: List<String>
    )

    private fun item(nivel: String, texto: String, base: String? = null) = ItemFicha(nivel, texto, base)

    private fun itensGerais(idade: Int?, peso: Int?): List<ItemFicha> {
        val itens = mutableListOf<ItemFicha>()
        if (idade != null) {
            itens += when {
                idade < 16 -> item("amarelo", "Abaixo de 16 anos só é possível com avaliação do médico do serviço e relatório que justifique a necessidade.", "Art. 39, § 4º")
                idade < 18 -> item("ok", "Entre 16 e 17 anos é preciso levar autorização escrita do responsável legal para cada doação, com cópia da identidade de quem assina.", "Art. 39, § 1º")
                idade >= 70 -> item("amarelo", "Acima de 69 anos, só continua doando quem já era doador de repetição, após avaliação médica.", "Art. 39, § 5º")
                idade >= 61 -> item("amarelo", "Se esta for a sua primeira doação, o limite é 60 anos; quem já doa de repetição pode seguir até 69.", "Art. 39, § 3º")
                else -> item("ok", "Sua idade está dentro da faixa de 16 a 69 anos.", "Art. 39")
            }
        }
        if (peso != null) {
            itens += if (peso < 50) item("amarelo", "O peso mínimo é 50 kg. Abaixo disso, só com avaliação médica e volume de coleta reduzido.", "Art. 40")
            else item("ok", "Seu peso atende ao mínimo de 50 kg.", "Art. 40")
        }
        return itens
    }

    private fun itemDaCausa(causa: String, meses: Int?, epilepsia: String?): ItemFicha = when (causa) {
        "nascenca" -> item("ok", "Uma deficiência de nascença, genética ou sensorial, sem doença ativa, não aparece como motivo de inaptidão na norma federal. O que conta é a sua saúde hoje e os remédios que você usa.")
        "paralisia-cerebral" -> item("amarelo", "A norma federal não cita a paralisia cerebral entre as causas de inaptidão, mas a decisão é caso a caso. Leve um relatório do seu médico e avise o hemocentro antes. Alguns hemocentros pedem que você consiga ir até a poltrona ou maca de coleta e estender o braço (por exemplo, o Hemominas).")
        "lesao-trauma" ->
            if (meses != null && meses >= 12) item("ok", "Já passou o prazo de 12 meses depois de politrauma. Cirurgias ortopédicas e de coluna pedem 6 meses. Se houve transplante de órgão ou tecido, a inaptidão é definitiva.", "Anexo 2")
            else item("amarelo", "Depois de politrauma a espera é de 12 meses; cirurgias ortopédicas e de coluna (laminectomia, artrodese) pedem 6 meses. Se houve transplante de órgão ou tecido, a inaptidão é definitiva.", "Anexo 2")
        "amputacao" ->
            if (meses != null && meses >= 6) item("ok", "A amputação em si não é impedimento. A cirurgia ortopédica pede 6 meses de espera e esse prazo já passou. O que conta é a causa da amputação (por exemplo, diabetes com insulina é inaptidão definitiva).", "Anexo 1 e 2")
            else item("amarelo", "A amputação em si não é impedimento. Cirurgia ortopédica pede 6 meses de espera. O que também conta é a causa (por exemplo, diabetes com insulina é inaptidão definitiva).", "Anexo 1 e 2")
        "avc" -> item("vermelho", "Antecedente de AVC é causa de inaptidão definitiva.", "Anexo 1-A")
        "neuro-lista" -> item("vermelho", "Essas doenças neurológicas estão na lista de inaptidão definitiva.", "Anexo 1-A")
        "epilepsia" -> when (epilepsia) {
            "em-tratamento" -> item("vermelho", "Enquanto está em tratamento para convulsão, a norma não permite doar. Se um dia o seu médico suspender o tratamento, a espera é de 3 anos sem crises. Nunca pare o remédio por conta própria para poder doar.", "Anexo 1-B e Anexo 3")
            "menos-3-anos" -> item("amarelo", "Depois de suspender o tratamento (com o seu médico), a espera é de 3 anos sem nenhuma crise. Ainda não completou esse tempo.", "Anexo 1-B")
            else -> item("amarelo", "Com 3 anos ou mais sem tratamento e sem crise, a norma permite a avaliação. Leve um relatório do neurologista confirmando.", "Anexo 1-B")
        }
        "autoimune" -> item("vermelho", "Doenças autoimunes que comprometem mais de um órgão são causa de inaptidão definitiva.", "Anexo 1-A")
        "infecciosa" -> item("amarelo", "A infecção precisa estar curada (meningite infecciosa pede 6 meses após a cura). A sequela em si não é citada como impedimento. Leve um relatório do médico.", "Anexo 1-B")
        "psiquiatrica" -> item("ok", "Uma condição psiquiátrica só impede a doação se gerar incapacidade jurídica. Os remédios têm regras próprias (veja abaixo).", "Anexo 1-A")
        else -> item("amarelo", "Essa causa não está na lista da ficha. Leve um relatório do médico que acompanha você, explicando a causa e se há alguma restrição, e converse com o hemocentro antes de ir.")
    }

    private fun itemDoMedicamento(m: String): ItemFicha = when (m) {
        "anticonvulsivante-convulsao" -> item("vermelho", "Anticonvulsivante usado para convulsão: a norma não permite doar enquanto estiver em uso. Nunca pare o remédio por conta própria para poder doar.", "Anexo 3")
        "anticonvulsivante-outro" -> item("ok", "Anticonvulsivante usado para outra coisa que não convulsão não causa inaptidão.", "Anexo 3")
        "antipsicotico" -> item("amarelo", "Antipsicóticos pedem 7 dias após a suspensão feita pelo médico que acompanha você, e o caso é avaliado individualmente. Nunca pare o remédio por conta própria.", "Anexo 3")
        "antidepressivo" -> item("ok", "Antidepressivos não contraindicam a doação, mas o médico da triagem vai avaliar você.", "Anexo 3")
        "ansiolitico" -> item("ok", "Ansiolíticos e remédios para dormir só contraindicam a doação em dose elevada.", "Anexo 3")
        "anticoagulante" -> item("amarelo", "Anticoagulantes pedem de 2 a 7 dias após a última dose, conforme o remédio, e a equipe avalia o motivo da prescrição.", "Anexo 3")
        "antiagregante" -> item("amarelo", "Antiagregantes plaquetários pedem de 3 a 30 dias, conforme o remédio, e a equipe avalia o motivo da prescrição.", "Anexo 3")
        "imunossupressor" -> item("amarelo", "Imunossupressores e imunobiológicos pedem 12 meses após interromper o remédio, avaliando o motivo e as sequelas.", "Anexo 3")
        "corticoide" -> item("amarelo", "Corticoides por via oral ou injetável pedem no mínimo 48 horas após suspender, e dependem da doença para a qual foram usados.", "Anexo 3")
        else -> item("amarelo", "Cada remédio é avaliado individualmente. Leve a receita ou a caixa para a equipe conferir.", "Art. 44")
    }

    private fun itemDaDecisao(decisao: String): ItemFicha = when (decisao) {
        "por-responsavel" -> item("vermelho", "Quando a deficiência intelectual ou a condição psiquiátrica gera incapacidade jurídica, a norma não permite a doação. A deficiência por si só não retira a sua capacidade (Lei 13.146/2015, arts. 6º e 85, § 1º), então quem avalia isso é a equipe, caso a caso.", "Anexo 1-A")
        "com-apoio" -> item("amarelo", "Você precisa entender e assinar o termo de consentimento. É permitido receber apoio para compreender, mas a decisão final é sua e a equipe confere isso na entrevista.", "Art. 34 e Lei 13.146/2015, art. 12")
        else -> item("ok", "Você entende e decide sobre a sua saúde. Isso é o que o termo de consentimento exige.", "Art. 34")
    }

    private val TEXTO_APOIO = mapOf(
        "mobilidade" to "Pergunte antes se o serviço tem rampa ou elevador, e como é a passagem da cadeira para a poltrona ou maca de coleta.",
        "libras" to "Peça um intérprete de Libras com antecedência. A norma manda explicar os cuidados e os riscos de forma acessível antes de você assinar o termo.",
        "leitura" to "Peça que leiam o termo e o questionário em voz alta ou em letra ampliada. A explicação precisa ser acessível e detalhada.",
        "linguagem-simples" to "Peça que a equipe explique em linguagem simples e sem pressa. A norma pede informações em linguagem compreensível.",
        "acompanhante" to "Leve um acompanhante adulto de confiança. Ele ajuda você, mas a entrevista e o termo de consentimento continuam sendo seus.",
        "prioridade" to "Você tem direito a atendimento prioritário (Lei 10.048/2000). Avise na recepção."
    )

    /** Valida a declaração no perfil. Devolve a frase de erro ou null. */
    fun validarDeclaracao(declarado: Boolean, tipos: List<String>, consentimento: Boolean): String? = when {
        !declarado -> null
        tipos.isEmpty() -> "Escolha ao menos um tipo de deficiência."
        !consentimento -> "Para guardar esta informação, confirme que você entende que ela é sensível e só aparece para você."
        else -> null
    }

    fun avaliarFicha(
        idade: Int?, pesoKg: Int?, causas: List<String>, epilepsiaSituacao: String?, mesesDesdeProcedimento: Int?,
        medicamentos: List<String>, decisao: String, apoios: List<String>
    ): Validacao<ResultadoFicha> {
        if (idade != null && (idade < 0 || idade > 120)) return Validacao.Erro("Idade inválida.")
        if (pesoKg != null && (pesoKg < 20 || pesoKg > 300)) return Validacao.Erro("Peso inválido.")
        if (causas.contains("epilepsia") && SITUACOES_EPILEPSIA.none { it.first == epilepsiaSituacao }) {
            return Validacao.Erro("Para epilepsia, informe se está em tratamento ou há quanto tempo parou.")
        }
        val meses = if (causas.contains("lesao-trauma") || causas.contains("amputacao")) mesesDesdeProcedimento else null

        val avaliacao = itensGerais(idade, pesoKg) +
            causas.map { itemDaCausa(it, meses, epilepsiaSituacao) } +
            medicamentos.map { itemDoMedicamento(it) } +
            itemDaDecisao(decisao)

        val nivel: String
        val titulo: String
        val resumo: String
        when {
            avaliacao.any { it.nivel == "vermelho" } -> {
                nivel = "vermelho"
                titulo = "Há um ponto que, pela norma, costuma impedir a doação"
                resumo = "Converse com o hemocentro: a decisão é da equipe, que avalia o seu caso. Se a resposta for \"não\", você ainda pode ajudar de outras formas."
            }
            avaliacao.any { it.nivel == "amarelo" } -> {
                nivel = "amarelo"
                titulo = "Dá para tentar, mas confirme alguns pontos antes"
                resumo = "Nada aqui impede de forma definitiva, mas alguns pontos dependem de prazo, de relatório médico ou da avaliação da equipe."
            }
            else -> {
                nivel = "verde"
                titulo = "Pelo que você respondeu, nada na norma impede de tentar"
                resumo = "A equipe do hemocentro ainda faz a triagem completa no dia (pulso, hemoglobina, entrevista)."
            }
        }

        val levar = mutableListOf("Documento oficial com foto.")
        if (idade != null && idade in 16..17) levar += "Autorização escrita do responsável legal e cópia da identidade de quem assina (para cada doação)."
        if (causas.any { it != "nascenca" && it != "psiquiatrica" }) levar += "Relatório recente do médico que acompanha você, explicando a causa e se há alguma restrição."
        if (medicamentos.isNotEmpty()) levar += "Lista dos remédios que você usa, com nome e dose (a receita ou a caixa)."
        if (decisao == "com-apoio") levar += "Uma pessoa de confiança junto, para apoiar você a entender o processo."

        val depois = listOf(
            "Você tem direito de saber o motivo e o prazo de qualquer inaptidão: o serviço registra na ficha de triagem e explica (art. 69).",
            "Quem não pode doar ainda ajuda: no Hemare, convide amigos com o \"Traga um amigo\" e divulgue campanhas de reposição. O serviço de hemoterapia deve estimular isso (art. 69, § 3º)."
        )
        return Validacao.Ok(ResultadoFicha(nivel, titulo, resumo, avaliacao, apoios.mapNotNull { TEXTO_APOIO[it] }, levar, depois))
    }
}
