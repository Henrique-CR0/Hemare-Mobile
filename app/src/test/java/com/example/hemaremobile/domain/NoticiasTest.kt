package com.example.hemaremobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticiasTest {
    private val agora = 1_800_000_000_000L
    private val fonte = Noticias.FONTES.first { it.id == "agencia-brasil" }
    private val dia = 86_400_000L

    private fun cru(titulo: String, url: String = "https://exemplo.gov.br/a", idadeDias: Long = 1, resumo: String = "") =
        ItemCru(titulo, resumo, url, null, agora - idadeDias * dia)

    @Test fun `noticia sobre doacao de sangue entra no feed`() {
        val n = Noticias.processar(listOf(cru("Hemocentro convoca doadores de sangue para reforçar estoque")), fonte, 1, agora)
        assertEquals(1, n.size)
        assertNotNull(n[0].categoria)
    }

    @Test fun `assunto fora do tema nao entra`() {
        assertTrue(Noticias.processar(listOf(cru("Campeonato de futebol termina com festa da torcida")), fonte, 1, agora).isEmpty())
    }

    @Test fun `descarta velha com mais de 30 dias`() {
        assertTrue(Noticias.processar(listOf(cru("Hemocentro convoca doadores de sangue para reforçar estoque", idadeDias = 40)), fonte, 1, agora).isEmpty())
    }

    @Test fun `so aceita link seguro`() {
        assertNull(Noticias.urlSegura("javascript:alert(1)"))
        assertNull(Noticias.urlSegura("ftp://x.com/a"))
        assertNotNull(Noticias.urlSegura("https://agenciabrasil.ebc.com.br/saude/a"))
    }

    @Test fun `mesma materia repetida fica uma vez so`() {
        val t = "Hemocentro convoca doadores de sangue para reforçar estoque"
        val n = Noticias.processar(listOf(cru(t, "https://a.gov.br/1"), cru(t, "https://a.gov.br/2")), fonte, 1, agora)
        assertEquals(1, n.size)
    }

    @Test fun `fonte de um estado marca o estado`() {
        assertEquals("MG", Noticias.detectarUf("qualquer texto", "MG"))
        assertEquals("PE", Noticias.detectarUf("Hemocentro de Pernambuco abre campanha de doação", null))
    }

    @Test fun `chave do titulo ignora acento caixa e pontuacao`() {
        assertEquals(Noticias.chaveDoTitulo("Doação: SANGUE!"), Noticias.chaveDoTitulo("doacao sangue"))
    }

    @Test fun `filtra por categoria estado e busca`() {
        val base = Noticias.processar(listOf(cru("Hemocentro convoca doadores de sangue para reforçar estoque")), fonte, 1, agora)
        val n = base[0]
        val todas = listOf(n, n.copy(id = 2, categoria = "campanhas", uf = "SP", busca = "campanha em sao paulo"))
        assertEquals(1, Noticias.filtrar(todas, "campanhas", null, "").size)
        assertEquals(1, Noticias.filtrar(todas, null, "SP", "").size)
        assertEquals(1, Noticias.filtrar(todas, null, null, "Paulo").size)
        assertEquals(2, Noticias.filtrar(todas, null, null, "").size)
        assertEquals(2, Noticias.filtrar(todas, null, null, "").first().id.let { 2 })
    }
}
