package com.example.hemaremobile.ui

import com.example.hemaremobile.data.TipoConta

/** Rotas do app. As que terminam em "?..." recebem argumentos opcionais. */
object Rotas {
    const val INICIO = "inicio"
    const val LISTA = "lista"
    const val CONTA = "conta" // aba "Entrar" do visitante
    const val CONFIGURACAO = "configuracao"

    const val LOCAIS = "locais"
    const val TRIAGEM = "triagem"
    const val ESTOQUE = "estoque"
    const val NOTICIAS = "noticias"
    const val CLIMA = "clima"
    const val APADRINHAR = "apadrinhar"
    const val AGENDAR = "agendar"
    const val ORIENTACOES = "orientacoes"
    const val MITOS = "mitos"
    const val DIREITOS = "direitos"
    const val PLACAR = "placar"
    const val RADAR = "radar"

    const val AREA_DOADOR = "area-doador"
    const val MEU_PERFIL = "meu-perfil"
    const val MINHAS_DOACOES = "minhas-doacoes"
    const val COMPLETAR_PERFIL = "completar-perfil"
    const val PAINEL_HOSPITAL = "painel-hospital"
    const val PERFIL_HOSPITAL = "perfil-hospital"
    const val ADMIN = "admin"
    const val ADMIN_STATUS = "admin/status"

    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val CADASTRO_HOSPITAL = "cadastro-hospital"
    const val RECUPERAR_SENHA = "recuperar-senha"
    const val CAMPANHA = "campanha"

    val LOGIN_COM_DESTINO = "login?destino={destino}"
    val CADASTRO_COM_DESTINO = "cadastro?destino={destino}&ref={ref}"
    val CAMPANHA_COM_CODIGO = "campanha/{codigo}"

    fun login(destino: String? = null) = if (destino.isNullOrBlank()) LOGIN else "$LOGIN?destino=" + android.net.Uri.encode(destino)
    fun cadastro(destino: String? = null, ref: String? = null): String {
        val partes = mutableListOf<String>()
        if (!destino.isNullOrBlank()) partes.add("destino=" + android.net.Uri.encode(destino))
        if (!ref.isNullOrBlank()) partes.add("ref=" + android.net.Uri.encode(ref))
        return if (partes.isEmpty()) CADASTRO else CADASTRO + "?" + partes.joinToString("&")
    }
    fun campanha(codigo: String) = "$CAMPANHA/$codigo"

    /** Para onde cada tipo de conta vai ao entrar (frontend/src/regras/rotaInicial.js). */
    fun inicialDe(papel: TipoConta?): String = when (papel) {
        TipoConta.ADMIN -> ADMIN
        TipoConta.HOSPITAL -> PAINEL_HOSPITAL
        TipoConta.DOADOR -> INICIO
        null -> INICIO
    }

    /** "Minha área" de cada tipo de conta (o botão Sair e o logotipo levam para cá). */
    fun areaDe(papel: TipoConta?): String = when (papel) {
        TipoConta.ADMIN -> ADMIN
        TipoConta.HOSPITAL -> PAINEL_HOSPITAL
        TipoConta.DOADOR -> AREA_DOADOR
        null -> CONTA
    }

    /** Caminho seguro para voltar depois de entrar: só rotas do próprio app (nada de links de fora). */
    fun destinoSeguro(valor: String?, padrao: String?): String? {
        val t = valor?.trim() ?: return padrao
        if (t.isEmpty() || t.length > 200 || t.startsWith("http") || t.contains("//") || t.contains("\\")) return padrao
        val base = t.substringBefore("?")
        if (base in listOf(LOGIN, CADASTRO, CADASTRO_HOSPITAL, RECUPERAR_SENHA)) return padrao
        return t
    }
}

/** O que a tela que exige conta explica ao visitante (o "portão de acesso" do site: frontend/src/regras/portoes.js). */
data class DadosPortao(
    val icone: String, val titulo: String, val porque: String, val beneficios: List<String>,
    val abertas: List<Pair<String, String>> // rótulo -> rota
)

object Portoes {
    val TRIAGEM = DadosPortao(
        "🩺", "A triagem \"Posso doar?\" é para quem tem conta",
        "A triagem pergunta sobre a sua saúde (remédios, doenças, procedimentos recentes). Isso é dado sensível: por respeito à LGPD, só aparece para quem criou conta e aceitou usar o Hemare. Suas respostas não ficam guardadas.",
        listOf(
            "Descobrir em 2 minutos se você parece apto e o que levar",
            "Saber quando poderá doar de novo, pelo intervalo desde a sua última doação",
            "Agendar o horário num hospital e acompanhar o seu histórico"
        ),
        listOf("Guia da doação (requisitos gerais)" to Rotas.ORIENTACOES, "Mitos e verdades" to Rotas.MITOS, "Onde doar" to Rotas.LOCAIS)
    )

    val APADRINHAR = DadosPortao(
        "💝", "Apadrinhar é para quem tem conta de doador",
        "Os pacientes que procuram padrinhos precisam de cuidado: só doadores com conta veem a lista, e cada padrinho só é chamado quando está apto e dentro do limite combinado. Os pacientes aparecem apenas por apelido, nunca com o nome verdadeiro.",
        listOf(
            "Ver quem precisa de sangue com regularidade e escolher quem apadrinhar",
            "Ser chamado só quando já puder doar (sem insistência)",
            "Ganhar o emblema de padrinho e acompanhar o seu afilhado"
        ),
        listOf("Notícias de doação por estado" to Rotas.NOTICIAS, "Termômetro de estoque" to Rotas.ESTOQUE, "Onde doar" to Rotas.LOCAIS)
    )

    val GENERICO = DadosPortao(
        "🔒", "Esta tela é para quem tem conta",
        "Aqui aparecem dados da sua conta (agenda, histórico, perfil). Entre ou crie uma conta gratuita para continuar; depois você volta direto para esta tela.",
        listOf("Agendar a doação e receber lembretes", "Acompanhar suas doações e conquistas", "Cuidar do seu perfil e da sua carteirinha"),
        listOf("Onde doar" to Rotas.LOCAIS, "Guia da doação" to Rotas.ORIENTACOES, "Notícias" to Rotas.NOTICIAS)
    )
}

data class ItemMenu(val rota: String, val emoji: String, val titulo: String, val descricao: String, val exigeConta: Boolean = false)
data class SecaoMenu(val titulo: String, val itens: List<ItemMenu>)

/** O que cada tipo de conta vê na aba Lista (frontend/src/regras/menu.js). */
fun menuDe(papel: TipoConta?): List<SecaoMenu> {
    val agendar = ItemMenu(Rotas.AGENDAR, "📅", "Agendar doação", "Escolha o hospital, o dia e o horário")
    val ondeDoar = ItemMenu(Rotas.LOCAIS, "📍", "Onde doar", "Hemocentros perto de você")
    val posso = ItemMenu(Rotas.TRIAGEM, "🩺", "Posso doar?", "Triagem rápida em 2 minutos")
    val apadrinhe = ItemMenu(Rotas.APADRINHAR, "💝", "Apadrinhe um paciente", "Quem precisa de sangue com regularidade")
    val estoque = ItemMenu(Rotas.ESTOQUE, "🌡️", "Termômetro de estoque", "Onde falta sangue agora")
    val noticias = ItemMenu(Rotas.NOTICIAS, "📰", "Notícias", "Doação, campanhas e saúde")
    val clima = ItemMenu(Rotas.CLIMA, "🌦️", "Previsão do sangue", "O \"clima\" do estoque por cidade")
    val radar = ItemMenu(Rotas.RADAR, "🔮", "Radar de aptidão", "Quantos doadores ficam aptos em breve")
    val aprenda = SecaoMenu(
        "Aprenda",
        listOf(
            ItemMenu(Rotas.ORIENTACOES, "📖", "Guia da doação", "Antes, durante e depois"),
            ItemMenu(Rotas.MITOS, "💡", "Mitos e verdades", "Tire suas dúvidas"),
            ItemMenu(Rotas.DIREITOS, "⚖️", "Seus direitos", "O que a lei garante a quem doa"),
            ItemMenu(Rotas.PLACAR, "🏙️", "Placar das cidades", "Quem mais doa sangue")
        )
    )
    return when (papel) {
        TipoConta.DOADOR -> listOf(
            SecaoMenu("Doar", listOf(agendar, ondeDoar, posso, apadrinhe)),
            SecaoMenu("Acompanhe", listOf(estoque, noticias, clima)),
            aprenda
        )
        TipoConta.HOSPITAL -> listOf(SecaoMenu("Acompanhe", listOf(estoque, noticias, clima, radar)))
        TipoConta.ADMIN -> listOf(SecaoMenu("Acompanhe", listOf(noticias, clima, radar)))
        null -> listOf(
            SecaoMenu("Doar", listOf(ondeDoar, posso.copy(exigeConta = true), apadrinhe.copy(exigeConta = true))),
            SecaoMenu("Acompanhe", listOf(estoque, noticias, clima)),
            aprenda
        )
    }
}
