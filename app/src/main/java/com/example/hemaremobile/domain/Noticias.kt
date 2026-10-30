package com.example.hemaremobile.domain

data class Noticia(
    val id: Int,
    val titulo: String,
    val resumo: String,
    val url: String,
    val imagem: String?,
    val publicadaEm: Long,
    val fonte: String,
    val categoria: String,
    val uf: String?,
    val chave: String,
    val busca: String
)

data class FonteNoticia(val id: String, val nome: String, val url: String, val tipo: String, val uf: String?, val semResumo: Boolean, val ativa: Boolean, val licenca: String)

data class ItemCru(val titulo: String, val resumo: String, val url: String, val imagem: String?, val dataMs: Long?)

/** Feed de notícias estilo Google Notícias (backend/regras/feed.js e fontes.js): limpa, classifica e evita repetição. */
object Noticias {
    val CATEGORIAS = listOf(
        "doacao" to "Doação de sangue", "estoque" to "Estoques e urgência", "campanhas" to "Campanhas",
        "orgaos" to "Órgãos, medula e leite", "saude" to "Saúde"
    )
    val ICONES = mapOf("doacao" to "🩸", "estoque" to "🚨", "campanhas" to "📣", "orgaos" to "💚", "saude" to "⚕️")

    const val MAX_TITULO = 300
    const val MAX_RESUMO = 240
    const val IDADE_MAXIMA_DIAS = 30
    const val POR_PAGINA = 20

    val FONTES = listOf(
        FonteNoticia("agencia-brasil-saude", "Agência Brasil", "https://agenciabrasil.ebc.com.br/rss/saude/feed.xml", "saude", null, false, true,
            "EBC (empresa pública): reprodução com crédito à Agência Brasil."),
        FonteNoticia("agencia-brasil", "Agência Brasil", "https://agenciabrasil.ebc.com.br/rss/ultimasnoticias/feed.xml", "geral", null, false, true,
            "EBC (empresa pública): reprodução com crédito à Agência Brasil."),
        FonteNoticia("agencia-camara", "Agência Câmara", "https://www.camara.leg.br/noticias/rss/ultimas-noticias", "geral", null, false, true,
            "Câmara dos Deputados: notícias podem ser reproduzidas desde que citada a fonte."),
        FonteNoticia("agencia-senado", "Agência Senado", "https://www12.senado.leg.br/noticias/rss", "geral", null, false, true,
            "Senado Federal: reprodução livre mediante citação da Agência Senado."),
        FonteNoticia("fiocruz", "Agência Fiocruz de Notícias", "https://agencia.fiocruz.br/rss.xml", "saude", null, true, true,
            "Fundação pública de saúde; o Hemare mostra só o título e o link."),
        FonteNoticia("hemocentro-ribeirao", "Hemocentro de Ribeirão Preto (USP)", "https://www.hemocentro.fmrp.usp.br/feed/", "sangue", "SP", false, true,
            "Hemocentro universitário: feed público do site; o Hemare mostra título, trecho e link."),
        FonteNoticia("agencia-minas", "Agência Minas", "https://www.agenciaminas.mg.gov.br/rss", "geral", "MG", false, true,
            "Governo de Minas Gerais: agência oficial; o Hemare mostra título, trecho e link."),
        FonteNoticia("g1-saude", "g1", "https://g1.globo.com/rss/g1/saude/", "saude", null, false, false,
            "Veículo comercial: termos do RSS não confirmados. Fica desligado até alguém conferir."),
        FonteNoticia("g1", "g1", "https://g1.globo.com/rss/g1/", "geral", null, false, false,
            "Veículo comercial: termos do RSS não confirmados. Fica desligado até alguém conferir.")
    )

    private val SANGUE = Regex("(doa(cao|coes)? de sangue|doar sangue|doe sangue|doador(es|a|as)? de sangue|sangue doado|bolsas? de sangue|banco(s)? de sangue|transfusa|transfusoes|plaqueta|estoque(s)? de sangue|estoque(s)? .{0,25}sangue|sangue .{0,20}estoque|tipo(s)? sanguineo|sangue raro|junho vermelho|dia (mundial|nacional) do doador)")
    private val HEMO = Regex("(hemocentro|hemoterapi|hemonucleo|hemorrede|hemovida|doenca falciforme|anemia falciforme|hemofilia)")
    private val PEDE_DOADORES = Regex("(doador|doar|doacao|doacoes)")
    private val URGENCIA = Regex("(estoque|urgen|emergencia|baix[oa]s?|critic|queda|falta|reforc|apelo|apela|pede|pedem|convoca|precisa)")
    private val ORGAOS = Regex("(doa(cao|coes) de orgaos?|doador(es|a|as)? de orgaos?|transplante|medula ossea|redome|doador(es|a|as)? de medula|banco(s)? de leite|leite humano|doa(cao|r) de leite)")
    private val CAMPANHA = Regex("(campanha|mutirao|semana (nacional|de)|vacinacao|multivacinacao|outubro rosa|novembro azul|setembro amarelo|janeiro branco|agosto dourado|junho vermelho|maio amarelo|mobilizacao)")
    private val CONTEXTO_SAUDE = Regex("(saude|vacin|\\bsus\\b|hospital|doenca|prevencao|imunizacao|doa(cao|r)|sangue)")

    fun trecho(texto: String, max: Int): String {
        val t = texto.replace(Regex("\\s+"), " ").trim()
        if (t.length <= max) return t
        val corte = t.substring(0, max - 1)
        val ultimoEspaco = corte.lastIndexOf(' ')
        val base = if (ultimoEspaco > max * 0.6) corte.substring(0, ultimoEspaco) else corte
        return base.replace(Regex("[\\s,;:.\\-–—]+$"), "") + "…"
    }

    /** Tira o HTML e as entidades e deixa só o texto corrido. */
    fun textoSimples(html: String): String {
        var t = html
            .replace(Regex("<(script|style)[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<!--[\\s\\S]*?-->"), " ")
            .replace(Regex("</?(p|br|div|li|h[1-6]|tr|blockquote)\\b[^>]*>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[a-zA-Z/!][^>]*>"), "")
        t = t.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'").replace("&hellip;", "…")
            .replace("&ndash;", "–").replace("&mdash;", "—").replace("&rsquo;", "’").replace("&lsquo;", "‘")
            .replace("&ldquo;", "“").replace("&rdquo;", "”")
        t = Regex("&#(\\d+);").replace(t) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: "" }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    fun resumoLimpo(texto: String, titulo: String): String {
        var t = texto.replace(Regex("\\s+"), " ").trim()
        t = t.replace(Regex("\\s*O post .{0,200} apareceu primeiro em .{0,100}$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[(…|\\.\\.\\.)\\]\\s*$"), "…")
        val nt = Textos.normalizar(titulo).trim()
        if (nt.isNotEmpty() && Textos.normalizar(t).startsWith(nt)) {
            t = t.substring(minOf(titulo.length, t.length)).replace(Regex("^[\\s:.\\-–—]+"), "")
        }
        return trecho(t, MAX_RESUMO)
    }

    fun urlSegura(texto: String?): String? {
        if (texto == null || texto.length > 500) return null
        val t = texto.trim()
        return if (t.startsWith("https://") || t.startsWith("http://")) t else null
    }

    /** Devolve a categoria ou null se o item não interessa. */
    fun classificar(titulo: String, resumo: String, fonte: FonteNoticia): String? {
        val t = Textos.normalizar("$titulo $resumo")
        val hemo = HEMO.containsMatchIn(t)
        val sangue = SANGUE.containsMatchIn(t) || (hemo && PEDE_DOADORES.containsMatchIn(t))
        if (sangue && URGENCIA.containsMatchIn(t)) return "estoque"
        if (sangue) return "doacao"
        if (ORGAOS.containsMatchIn(t)) return "orgaos"
        if (CAMPANHA.containsMatchIn(t) && (fonte.tipo != "geral" || CONTEXTO_SAUDE.containsMatchIn(t))) return "campanhas"
        if (hemo || fonte.tipo == "sangue" || fonte.tipo == "saude") return "saude"
        return null
    }

    private val UFS_POR_NOME = Estados.UFS.sortedByDescending { it.second.length }

    /** Estado citado no texto. Só vale se for UM estado só. */
    fun detectarUf(texto: String, ufDaFonte: String?): String? {
        if (ufDaFonte != null) return ufDaFonte
        var resto = texto
        val achadas = mutableSetOf<String>()
        for ((sigla, nome) in UFS_POR_NOME) {
            val re = if (sigla == "PA") Regex("(?<!\\p{L})Pará(?!\\p{L})")
            else Regex("(?<!\\p{L})" + nome.replace(" ", "\\s+") + "(?!\\p{L})", RegexOption.IGNORE_CASE)
            if (re.containsMatchIn(resto)) {
                achadas.add(sigla)
                resto = re.replace(resto, " ")
            }
        }
        if (Regex("(?<!\\p{L})Bras[ií]lia(?!\\p{L})").containsMatchIn(resto)) achadas.add("DF")
        return if (achadas.size == 1) achadas.first() else null
    }

    fun chaveDoTitulo(titulo: String): String =
        Textos.normalizar(titulo).replace(Regex("[^a-z0-9]+"), " ").trim().take(70)

    /** Itens crus de um feed -> notícias prontas (descarta velhas, repetidas e as que não interessam). */
    fun processar(crus: List<ItemCru>, fonte: FonteNoticia, proximoId: Int, agoraMs: Long = System.currentTimeMillis()): List<Noticia> {
        val limite = agoraMs - IDADE_MAXIMA_DIAS * 86400000L
        val vistos = mutableSetOf<String>()
        val prontos = mutableListOf<Noticia>()
        var id = proximoId
        for (cru in crus) {
            val url = urlSegura(cru.url) ?: continue
            val titulo = trecho(cru.titulo, MAX_TITULO)
            if (titulo.length < 15) continue
            val data = cru.dataMs ?: continue
            val publicada = minOf(data, agoraMs)
            if (publicada < limite) continue
            val resumo = if (fonte.semResumo) "" else resumoLimpo(cru.resumo, titulo)
            val categoria = classificar(titulo, resumo, fonte) ?: continue
            val chave = chaveDoTitulo(titulo)
            if (vistos.contains(url) || vistos.contains("t:$chave")) continue
            vistos.add(url); vistos.add("t:$chave")
            prontos.add(
                Noticia(id++, titulo, resumo, url, cru.imagem, publicada, fonte.nome, categoria, detectarUf("$titulo $resumo", fonte.uf), chave,
                    Textos.normalizar("$titulo $resumo ${fonte.nome}"))
            )
        }
        return prontos
    }

    fun filtrar(todas: List<Noticia>, categoria: String?, uf: String?, busca: String): List<Noticia> {
        val q = Textos.normalizar(busca).trim()
        return todas.filter { n ->
            (categoria == null || n.categoria == categoria) && (uf == null || n.uf == uf) && (q.isEmpty() || n.busca.contains(q))
        }.sortedByDescending { it.publicadaEm }
    }

    /** Duas letras para o "logo" da fonte: "Agência Brasil" -> "AB". */
    fun iniciais(nome: String): String {
        val palavras = nome.split(Regex("\\s+")).filter { it.isNotEmpty() && it[0].isLetter() && !it.lowercase().matches(Regex("de|da|do|das|dos|e")) }
        return palavras.take(2).joinToString("") { it.substring(0, 1).uppercase() }.ifEmpty { "?" }
    }

    /** Cor da fonte (a mesma fonte tem sempre a mesma cor): matiz 0..359. */
    fun matizDaFonte(nome: String): Float {
        var h = 0
        for (c in nome) h = (h * 31 + c.code) % 360
        return h.toFloat()
    }
}

/** Notícias de exemplo, usadas só se a internet estiver fora do ar (a coleta real lê os RSS acima). */
object NoticiasExemplo {
    fun lista(agoraMs: Long = System.currentTimeMillis()): List<Noticia> {
        fun n(id: Int, titulo: String, resumo: String, fonte: String, cat: String, uf: String?, horas: Int, url: String) =
            Noticia(id, titulo, resumo, url, null, agoraMs - horas * 3600000L, fonte, cat, uf, Noticias.chaveDoTitulo(titulo),
                Textos.normalizar("$titulo $resumo $fonte"))
        return listOf(
            n(-1, "Hemocentros pedem doações de sangue para reforçar estoques", "Unidades de várias regiões convocam doadores para manter os bancos de sangue abastecidos.", "Agência Brasil", "estoque", null, 3, "https://agenciabrasil.ebc.com.br"),
            n(-2, "Quem pode doar sangue? Veja os requisitos básicos", "Idade entre 16 e 69 anos, peso mínimo de 50 kg e boas condições de saúde são alguns critérios.", "Agência Brasil", "doacao", null, 9, "https://agenciabrasil.ebc.com.br"),
            n(-3, "Campanha de doação de medula óssea recebe novos cadastros", "Mutirão estimula a entrada de voluntários no cadastro nacional de doadores de medula.", "Agência Câmara", "orgaos", null, 26, "https://www.camara.leg.br/noticias"),
            n(-4, "Hemocentro de Ribeirão Preto reforça campanha de doação", "Serviço universitário abre horários estendidos para receber doadores.", "Hemocentro de Ribeirão Preto (USP)", "campanhas", "SP", 30, "https://www.hemocentro.fmrp.usp.br"),
            n(-5, "Fiocruz divulga estudo sobre doenças do sangue", "Pesquisa acompanha pacientes com anemia falciforme no SUS.", "Agência Fiocruz de Notícias", "saude", null, 50, "https://agencia.fiocruz.br")
        )
    }
}
