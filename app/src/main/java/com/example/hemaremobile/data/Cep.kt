package com.example.hemaremobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EnderecoCep(val logradouro: String, val bairro: String, val cidade: String, val uf: String)

/** Busca o endereço de um CEP no ViaCEP (o mesmo serviço que o site usa). Devolve null se não achar ou sem internet. */
suspend fun buscarCep(cep: String): EnderecoCep? = withContext(Dispatchers.IO) {
    val so = cep.filter { it.isDigit() }
    if (so.length != 8) return@withContext null
    try {
        val c = URL("https://viacep.com.br/ws/$so/json/").openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 6000
        val texto = try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
        val j = JSONObject(texto)
        if (j.optBoolean("erro", false)) null
        else EnderecoCep(j.optString("logradouro"), j.optString("bairro"), j.optString("localidade"), j.optString("uf"))
    } catch (e: Exception) {
        null
    }
}
