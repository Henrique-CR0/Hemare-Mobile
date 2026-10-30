package com.example.hemaremobile.domain

enum class NivelTriagem { VERDE, AMARELO, VERMELHO }

data class ResultadoTriagem(val nivel: NivelTriagem, val titulo: String, val motivos: List<String>)

data class PerguntaSimNao(val campo: String, val texto: String)

/** Regra e textos da triagem "Posso doar?" (frontend/src/regras/triagem.js e paginas/Triagem.jsx do site). */
object Triagem {
    const val IDADE_MIN = 16
    const val IDADE_MAX = 69
    const val PESO_MIN = 50

    val PERGUNTAS_RECENTES = listOf(
        PerguntaSimNao("tatuagemRecente", "Fez tatuagem, piercing, maquiagem definitiva ou botox/preenchimento nos últimos 4 meses?"),
        PerguntaSimNao("endoscopiaRecente", "Fez endoscopia ou colonoscopia nos últimos 4 meses?"),
        PerguntaSimNao("gripeResfriado", "Está com gripe ou resfriado (ou teve há poucos dias)?"),
        PerguntaSimNao("bebidaAlcoolica", "Ingeriu bebida alcoólica nas últimas 12 horas?"),
        PerguntaSimNao("gravidezOuPosParto", "Está grávida ou teve parto recentemente?")
    )

    val PERGUNTAS_SAUDE = listOf(
        PerguntaSimNao("temHIV", "Você tem HIV/AIDS?"),
        PerguntaSimNao("temHepatiteB", "Você tem Hepatite B?"),
        PerguntaSimNao("temHepatiteC", "Você tem Hepatite C?"),
        PerguntaSimNao("temHTLV", "Você tem HTLV?"),
        PerguntaSimNao("temChagas", "Você tem Doença de Chagas?"),
        PerguntaSimNao("hepatiteAposOnzeAnos", "Teve hepatite após os 11 anos de idade?"),
        PerguntaSimNao("usaDrogasInjetaveis", "Faz uso de drogas injetáveis?")
    )

    val PERGUNTAS_ATENCAO = listOf(
        PerguntaSimNao("temDiabetes", "Você tem diabetes?"),
        PerguntaSimNao("temHipertensao", "Você tem hipertensão (pressão alta)?"),
        PerguntaSimNao("usaMedicacaoContinua", "Você usa algum medicamento controlado ou de uso contínuo?")
    )

    /** Aviso em tempo real embaixo do campo idade ("" = sem aviso). */
    fun avisoIdade(idade: Int?): String = when {
        idade == null || idade == 0 -> ""
        idade < IDADE_MIN -> "Idade mínima para doação: $IDADE_MIN anos."
        idade > IDADE_MAX -> "Acima de $IDADE_MAX anos, só doadores regulares, após avaliação da triagem clínica."
        else -> ""
    }

    fun avisoPeso(peso: Int?): String =
        if (peso != null && peso > 0 && peso < PESO_MIN) "Peso mínimo para doação: $PESO_MIN kg." else ""

    fun avaliar(idade: Int, peso: Int, r: Map<String, Boolean>): ResultadoTriagem {
        fun sim(campo: String) = r[campo] == true
        val impedimentos = mutableListOf<String>()
        val atencoes = mutableListOf<String>()

        if (sim("temHIV")) impedimentos += "Você marcou HIV/AIDS."
        if (sim("temHepatiteB") || sim("temHepatiteC")) impedimentos += "Você marcou Hepatite B ou C."
        if (sim("temHTLV")) impedimentos += "Você marcou HTLV."
        if (sim("temChagas")) impedimentos += "Você marcou Doença de Chagas."
        if (sim("usaDrogasInjetaveis")) impedimentos += "Você marcou uso de drogas injetáveis."
        if (sim("hepatiteAposOnzeAnos")) impedimentos += "Você marcou hepatite após os 11 anos de idade."

        if (peso > 0 && peso < 50) impedimentos += "Seu peso está abaixo de 50 kg, que é o mínimo para doar."
        if (idade in 1..15) impedimentos += "A idade mínima para doar é 16 anos."
        if (idade in 16..17) atencoes += "Menores de 18 anos precisam do consentimento formal dos pais ou responsáveis."
        if (idade in 61..69) atencoes += "Se esta for a sua primeira doação, o limite é 60 anos. Quem já doa de repetição pode continuar até os 69."
        if (idade >= 70) atencoes += "Acima de 70 anos, só continua doando quem já é doador regular e passa pela avaliação da triagem clínica. Alguns hemocentros, como o Hemope, pedem ao menos uma doação nos últimos 12 meses e intervalo de 6 meses entre as doações."
        if (r["dormiuBem"] == false) atencoes += "É importante ter dormido bem antes de doar."
        if (r["alimentado"] == false) atencoes += "Não vá em jejum: é preciso estar alimentado."

        if (sim("tatuagemRecente")) atencoes += "Tatuagem, piercing, maquiagem definitiva ou botox/preenchimento pedem 4 meses de espera (eram 12). O prazo cai para 7 dias se o local tiver alvará sanitário e você levar o comprovante. Piercing na boca ou na região genital: 4 meses depois de retirar."
        if (sim("endoscopiaRecente")) atencoes += "Endoscopia ou colonoscopia pedem 4 meses de espera (eram 6)."
        if (sim("gripeResfriado")) atencoes += "Gripe ou resfriado recente pede aguardar alguns dias."
        if (sim("bebidaAlcoolica")) atencoes += "Bebida alcoólica nas últimas 12 horas impede a doação hoje."
        if (sim("gravidezOuPosParto")) atencoes += "Gravidez ou pós-parto recente pede um período de espera."

        if (sim("temDiabetes")) atencoes += "Diabetes: se controlada, geralmente não impede — confirme na triagem."
        if (sim("temHipertensao")) atencoes += "Hipertensão: se controlada, geralmente não impede — confirme na triagem."
        if (sim("usaMedicacaoContinua")) atencoes += "Você usa medicação contínua/controlada. Muitos remédios não impedem a doação, mas alguns pedem um tempo de espera. NUNCA pare um remédio por conta própria para doar — entre em contato com o hemocentro onde vai doar para confirmar."

        return when {
            impedimentos.isNotEmpty() -> ResultadoTriagem(NivelTriagem.VERMELHO, "Há um ponto importante a verificar", impedimentos)
            atencoes.isNotEmpty() -> ResultadoTriagem(NivelTriagem.AMARELO, "Atenção: confirme alguns pontos no hemocentro", atencoes)
            else -> ResultadoTriagem(NivelTriagem.VERDE, "Tudo indica que você pode doar!", emptyList())
        }
    }
}
