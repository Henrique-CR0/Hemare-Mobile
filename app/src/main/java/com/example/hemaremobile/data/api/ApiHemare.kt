package com.example.hemaremobile.data.api

import com.example.hemaremobile.data.ItemEstoque
import com.example.hemaremobile.data.LocalDoacao
import com.example.hemaremobile.domain.Noticia
import com.example.hemaremobile.domain.Noticias
import com.example.hemaremobile.domain.Textos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

class ErroApi(val codigo: Int, mensagem: String) : IOException(mensagem)

data class SessaoRemota(val token: String, val nome: String, val email: String, val tipo: String)

/**
 * Cliente da API do backend do Hemare (docs/API.md do site). Fala só JSON, usa o token JWT do login nas rotas protegidas
 * e traduz os erros do servidor (`{ "erro": "..." }`) para a frase que a pessoa lê.
 * Hoje o app usa as rotas públicas (locais, estoque, notícias); o login existe para conferir a conexão e abrir as demais.
 */
class ApiHemare(base: String, private val token: String? = null) {
    private val base = normalizarBase(base)

    // ---------- rotas ----------
    suspend fun saude(): Boolean = try { pedir("GET", "/saude"); true } catch (e: IOException) { false }

    suspend fun login(email: String, senha: String): SessaoRemota {
        val r = pedir("POST", "/auth/login", JSONObject().put("email", email).put("senha", senha)) as JSONObject
        val u = r.getJSONObject("usuario")
        return SessaoRemota(r.getString("token"), u.optString("nome"), u.optString("email"), u.optString("tipo"))
    }

    suspend fun locais(): List<LocalDoacao> = Conversores.locais(pedir("GET", "/locais") as JSONArray)

    suspend fun estoquePublico(): List<ItemEstoque> = Conversores.estoque(pedir("GET", "/hospital/estoque-publico") as JSONArray)

    /** Páginas do feed de notícias (a API devolve 20 por vez). */
    suspend fun noticias(paginas: Int = 2): List<Noticia> {
        val todas = mutableListOf<Noticia>()
        for (p in 1..paginas) {
            val r = pedir("GET", "/noticias/feed?pagina=$p") as JSONObject
            todas.addAll(Conversores.noticias(r.getJSONArray("itens"), todas.size + 1))
            if (!r.optBoolean("temMais", false)) break
        }
        return todas
    }

    // ---------- HTTP ----------
    private suspend fun pedir(metodo: String, caminho: String, corpo: JSONObject? = null): Any = withContext(Dispatchers.IO) {
        val conexao = URL(base + caminho).openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = metodo
            conexao.connectTimeout = 8000
            conexao.readTimeout = 12000
            conexao.setRequestProperty("Accept", "application/json")
            if (token != null) conexao.setRequestProperty("Authorization", "Bearer $token")
            if (corpo != null) {
                conexao.doOutput = true
                conexao.setRequestProperty("Content-Type", "application/json")
                conexao.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conexao.responseCode
            val texto = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (codigo !in 200..299) throw ErroApi(codigo, Conversores.mensagemDeErro(codigo, texto))
            if (texto.isBlank()) JSONObject() else if (texto.trimStart().startsWith("[")) JSONArray(texto) else JSONObject(texto)
        } finally {
            conexao.disconnect()
        }
    }

    companion object {
        /** Aceita "meuservidor.com", "http://192.168.0.5:3000/" etc. e devolve a base sem barra no fim. */
        fun normalizarBase(texto: String): String {
            var t = texto.trim().trimEnd('/')
            if (t.isNotEmpty() && !t.startsWith("http://") && !t.startsWith("https://")) t = (if (t.startsWith("localhost") || t.first().isDigit()) "http://" else "https://") + t
            return t
        }

        fun parametro(valor: String): String = URLEncoder.encode(valor, "UTF-8")
    }
}

/** Traduz as respostas JSON do servidor para os tipos do app. Separado do HTTP para poder ser conferido sem rede. */
object Conversores {
    fun mensagemDeErro(codigo: Int, corpo: String): String {
        val doServidor = try { JSONObject(corpo).optString("erro").takeIf { it.isNotBlank() } } catch (e: Exception) { null }
        return doServidor ?: when (codigo) {
            401 -> "Sessão expirada. Entre de novo."
            403 -> "Sua conta não tem permissão para isto."
            404 -> "Não encontrado no servidor."
            429 -> "Muitas tentativas. Espere um pouco."
            else -> "O servidor respondeu com erro ($codigo)."
        }
    }

    fun locais(a: JSONArray): List<LocalDoacao> = (0 until a.length()).map { a.getJSONObject(it) }.map {
        LocalDoacao(
            it.getInt("id"), it.getString("nome"), it.optString("cidade"), it.optString("estado"),
            it.optString("endereco").takeIf { s -> s.isNotBlank() && s != "null" }, it.optString("telefone").takeIf { s -> s.isNotBlank() && s != "null" },
            if (it.isNull("latitude")) null else it.optDouble("latitude"), if (it.isNull("longitude")) null else it.optDouble("longitude")
        )
    }

    fun estoque(a: JSONArray): List<ItemEstoque> = (0 until a.length()).map { a.getJSONObject(it) }.map {
        ItemEstoque(it.getString("tipo_sanguineo"), it.getString("hospital"), it.getString("cidade"), it.optString("estado"), it.getString("nivel"))
    }

    fun noticias(itens: JSONArray, primeiroId: Int): List<Noticia> = (0 until itens.length()).map { itens.getJSONObject(it) }.mapIndexed { i, n ->
        val titulo = n.getString("titulo")
        val resumo = n.optString("resumo")
        Noticia(
            primeiroId + i, titulo, resumo, n.getString("url"), n.optString("imagem").takeIf { s -> s.isNotBlank() && s != "null" },
            Instant.parse(n.getString("publicadaEm")).toEpochMilli(), n.optString("fonte"), n.getString("categoria"),
            n.optString("uf").takeIf { s -> s.isNotBlank() && s != "null" }, Noticias.chaveDoTitulo(titulo), Textos.normalizar("$titulo $resumo ${n.optString("fonte")}")
        )
    }
}
