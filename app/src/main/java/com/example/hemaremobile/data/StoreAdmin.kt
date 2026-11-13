package com.example.hemaremobile.data

import android.util.Xml
import com.example.hemaremobile.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Operações do administrador (verificação de hospitais, notícias, status) e a coleta real de notícias por RSS.

fun Estado.hospitaisParaAdmin(filtro: String): List<Hospital> =
    hospitais.values.filter {
        when (filtro) {
            "pendente" -> it.status == StatusVerificacao.PENDENTE
            "aprovado" -> it.status == StatusVerificacao.APROVADO
            "recusado" -> it.status == StatusVerificacao.RECUSADO
            else -> true
        }
    }.sortedBy { usuarioPorId(it.id)?.criadoEm }

fun HemareStore.aprovarHospital(id: Int): Res {
    val e = atual()
    if (e.papel != TipoConta.ADMIN) return Res.erro("Acesso restrito a administradores.")
    val h = e.hospitais[id] ?: return Res.erro("Hospital não encontrado.")
    definir(e.copy(hospitais = e.hospitais + (id to h.copy(aprovado = true, motivoRecusa = null))))
    return Res.ok("Hospital aprovado.")
}

/** Recusa (ou revoga a aprovação) com o motivo, que o hospital vê no painel dele. */
fun HemareStore.recusarHospital(id: Int, motivo: String): Res {
    val e = atual()
    if (e.papel != TipoConta.ADMIN) return Res.erro("Acesso restrito a administradores.")
    val h = e.hospitais[id] ?: return Res.erro("Hospital não encontrado.")
    val m = motivo.trim()
    if (m.length < 10) return Res.erro("Explique o motivo da recusa (pelo menos 10 caracteres).")
    if (m.length > 500) return Res.erro("O motivo pode ter no máximo 500 caracteres.")
    definir(e.copy(hospitais = e.hospitais + (id to h.copy(aprovado = false, motivoRecusa = m))))
    return Res.ok("Cadastro recusado.")
}

fun HemareStore.ocultarNoticia(id: Int): Res {
    val e = atual()
    if (e.papel != TipoConta.ADMIN) return Res.erro("Acesso restrito a administradores.")
    definir(e.copy(noticiasOcultas = e.noticiasOcultas + id))
    return Res.ok("Notícia escondida do feed.")
}

/** Auditoria pública da cadeia de confiança das doações. */
fun Estado.verificarCadeia(): Cadeia.Resultado = Cadeia.verificar(doacoes.map { Cadeia.Elo(it.id, it.doadorId, it.hospitalId, it.data, it.hash) })

fun Estado.noticiasVisiveis(): List<Noticia> = noticias.filter { it.id !in noticiasOcultas }

/** Números de uso para a página "Status do desenvolvimento" (só números, nenhum dado de pessoa). */
data class UsoSistema(
    val doadores: Int, val hospitais: Int, val hospitaisAprovados: Int, val novasNos7Dias: Int, val doacoesConfirmadas: Int,
    val agendados: Int, val compareceram: Int, val noticias: Int
)

fun Estado.uso(): UsoSistema {
    val hoje = Datas.hoje()
    return UsoSistema(
        doadores = usuarios.count { it.tipo == TipoConta.DOADOR }, hospitais = hospitais.size, hospitaisAprovados = hospitaisAprovados().size,
        novasNos7Dias = usuarios.count { Datas.diasEntre(it.criadoEm, hoje) in 0..6 }, doacoesConfirmadas = doacoes.size,
        agendados = agendamentos.count { it.status == "agendado" }, compareceram = agendamentos.count { it.status == "compareceu" },
        noticias = noticiasVisiveis().size
    )
}

// ---------------------------------------------------------------------
// Coleta de notícias (RSS) — roda no aparelho, no lugar do coletor do servidor do site
// ---------------------------------------------------------------------

private fun lerFeedXml(xml: String): List<ItemCru> {
    val itens = mutableListOf<ItemCru>()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(java.io.StringReader(xml))

    var dentro = false
    val campos = mutableMapOf<String, String>()
    var link: String? = null
    var imagem: String? = null
    var evento = parser.eventType
    while (evento != XmlPullParser.END_DOCUMENT) {
        when (evento) {
            XmlPullParser.START_TAG -> {
                val nome = parser.name
                if (nome == "item" || nome == "entry") {
                    dentro = true; campos.clear(); link = null; imagem = null
                } else if (dentro) {
                    when (nome) {
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            val rel = parser.getAttributeValue(null, "rel")
                            if (href != null) { if (rel == null || rel == "alternate") link = href; parser.nextTag() }
                            else campos["link"] = parser.nextText()
                        }
                        "enclosure" -> {
                            val tipo = parser.getAttributeValue(null, "type") ?: ""
                            val url = parser.getAttributeValue(null, "url")
                            if (imagem == null && url != null && (tipo.startsWith("image/") || Regex("\\.(jpe?g|png|webp)(\\?|$)", RegexOption.IGNORE_CASE).containsMatchIn(url))) imagem = url
                        }
                        "media:thumbnail", "media:content" -> {
                            val url = parser.getAttributeValue(null, "url")
                            val tipo = parser.getAttributeValue(null, "type") ?: ""
                            val medium = parser.getAttributeValue(null, "medium") ?: ""
                            if (imagem == null && url != null && (nome == "media:thumbnail" || medium == "image" || tipo.startsWith("image/"))) imagem = url
                        }
                        "title", "description", "summary", "content", "content:encoded", "pubDate", "published", "updated", "dc:date", "guid" ->
                            if (!campos.containsKey(nome)) campos[nome] = try { parser.nextText() } catch (e: Exception) { "" }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                if (parser.name == "item" || parser.name == "entry") {
                    dentro = false
                    val url = link ?: campos["link"]?.trim()?.takeIf { it.isNotEmpty() } ?: campos["guid"]?.takeIf { it.startsWith("http") } ?: ""
                    val resumoBruto = listOf("summary", "description", "content:encoded", "content").mapNotNull { campos[it] }.firstOrNull { it.isNotBlank() } ?: ""
                    val data = listOf("pubDate", "published", "updated", "dc:date").mapNotNull { campos[it] }.firstNotNullOfOrNull { paraMs(it) }
                    itens.add(
                        ItemCru(
                            Noticias.textoSimples(campos["title"] ?: ""), Noticias.textoSimples(resumoBruto), url.trim(),
                            imagem ?: primeiraImagemDoHtml(resumoBruto), data
                        )
                    )
                }
            }
        }
        evento = parser.next()
    }
    return itens
}

private fun primeiraImagemDoHtml(html: String): String? {
    val m = Regex("<img\\b[^>]*?\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(html) ?: return null
    return m.groupValues[1].takeIf { it.startsWith("https://") }
}

private fun paraMs(texto: String): Long? {
    val t = texto.trim()
    return try { ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) {
        try { OffsetDateTime.parse(t).toInstant().toEpochMilli() } catch (e2: Exception) {
            try { java.time.Instant.parse(t).toEpochMilli() } catch (e3: Exception) { null }
        }
    }
}

private fun baixar(url: String): String {
    val conexao = URL(url).openConnection() as HttpURLConnection
    conexao.connectTimeout = 8000
    conexao.readTimeout = 10000
    conexao.setRequestProperty("User-Agent", "HemareMobile/1.0 (projeto academico)")
    conexao.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
    try {
        if (conexao.responseCode !in 200..299) throw java.io.IOException("HTTP ${conexao.responseCode}")
        return conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        conexao.disconnect()
    }
}

/** Lê os feeds RSS de cada fonte ativa e troca o feed da loja pelas notícias novas (sem mexer no feed se nada for lido). */
suspend fun HemareStore.coletarNoticias(): Res {
    val fontes = Noticias.FONTES.filter { it.ativa }
    val agora = System.currentTimeMillis()
    val resultados = mutableListOf<ResultadoFonte>()
    val todas = mutableListOf<Noticia>()
    var id = 1
    withContext(Dispatchers.IO) {
        for (f in fontes) {
            try {
                val itens = lerFeedXml(baixar(f.url))
                val prontas = Noticias.processar(itens, f, id, agora)
                id += prontas.size
                todas.addAll(prontas)
                resultados.add(ResultadoFonte(f.id, true, itens.size, prontas.size, null))
            } catch (e: Exception) {
                resultados.add(ResultadoFonte(f.id, false, 0, 0, e.message ?: e.javaClass.simpleName))
            }
        }
    }
    // Mesma matéria em mais de uma fonte: fica a primeira.
    val chaves = mutableSetOf<String>()
    val unicas = todas.sortedByDescending { it.publicadaEm }.filter { chaves.add(it.chave) }
    val e = atual()
    definir(
        e.copy(
            noticias = if (unicas.isNotEmpty()) unicas else e.noticias,
            noticiasOcultas = if (unicas.isNotEmpty()) emptySet() else e.noticiasOcultas,
            coleta = ResultadoColeta(agora, resultados)
        )
    )
    val ok = resultados.count { it.ok }
    return if (unicas.isEmpty()) Res.erro("Não consegui ler notícias agora (${ok} de ${fontes.size} fontes responderam). Mostrando as de exemplo.")
    else Res.ok("${unicas.size} notícias atualizadas ($ok de ${fontes.size} fontes responderam).")
}
