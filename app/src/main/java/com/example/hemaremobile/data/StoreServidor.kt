package com.example.hemaremobile.data

import com.example.hemaremobile.data.api.ApiHemare
import com.example.hemaremobile.domain.Textos
import java.io.IOException

// Ligação com o servidor do Hemare (o backend do site). É opcional: sem endereço, o app segue com os dados do aparelho.

fun HemareStore.definirServidor(endereco: String?) {
    val limpo = endereco?.trim()?.takeIf { it.isNotEmpty() }?.let { ApiHemare.normalizarBase(it) }
    definir(atual().copy(servidor = limpo))
}

/** Junta a lista do servidor com a do aparelho: mantém as coordenadas (que o servidor não manda) dos locais com o mesmo nome. */
fun mesclarLocais(doServidor: List<LocalDoacao>, locais: List<LocalDoacao>): List<LocalDoacao> {
    fun chave(nome: String) = Textos.limpar(Textos.normalizar(nome))
    val porNome = locais.associateBy { chave(it.nome) }
    return doServidor.map { novo ->
        val antigo = porNome[chave(novo.nome)]
        if (novo.lat == null && antigo?.lat != null) novo.copy(lat = antigo.lat, lng = antigo.lng) else novo
    }
}

/** Baixa locais, termômetro de estoque e notícias do servidor e troca os dados públicos do aparelho por eles. */
suspend fun HemareStore.sincronizarComServidor(): Res {
    val base = atual().servidor ?: return Res.erro("Informe o endereço do servidor do Hemare.")
    val api = ApiHemare(base)
    if (!api.saude()) return Res.erro("Não consegui falar com $base. Confira o endereço e a internet.")

    val partes = mutableListOf<String>()
    val falhas = mutableListOf<String>()
    var e = atual()

    try { api.locais().let { if (it.isNotEmpty()) { e = e.copy(locais = mesclarLocais(it, e.locais)); partes.add("${it.size} locais") } } }
    catch (x: IOException) { falhas.add("locais (${x.message})") }

    try { api.estoquePublico().let { e = e.copy(estoqueExterno = it); partes.add("${it.size} níveis de estoque") } }
    catch (x: IOException) { falhas.add("estoque (${x.message})") }

    try { api.noticias().let { if (it.isNotEmpty()) { e = e.copy(noticias = it, noticiasOcultas = emptySet()); partes.add("${it.size} notícias") } } }
    catch (x: IOException) { falhas.add("notícias (${x.message})") }

    if (partes.isEmpty()) return Res.erro("O servidor respondeu, mas nada foi baixado: ${falhas.joinToString("; ")}.")
    // as mudanças feitas enquanto baixava (ex.: uma ação da pessoa) valem: só os campos públicos vêm do servidor
    val agora = atual()
    definir(agora.copy(locais = e.locais, estoqueExterno = e.estoqueExterno, noticias = e.noticias, noticiasOcultas = e.noticiasOcultas, sincronizadoEm = System.currentTimeMillis()))
    return Res.ok("Sincronizado: ${partes.joinToString(", ")}." + if (falhas.isNotEmpty()) " Falhou: ${falhas.joinToString("; ")}." else "")
}

/** Confere o login no servidor (usa a conta real do site). Não guarda a senha. */
suspend fun testarLoginNoServidor(base: String, email: String, senha: String): Res = try {
    val s = ApiHemare(base).login(email, senha)
    Res.ok("Conectado como ${s.nome.ifBlank { s.email }} (${s.tipo}).")
} catch (x: IOException) {
    Res.erro(x.message ?: "Falha de conexão.")
}
