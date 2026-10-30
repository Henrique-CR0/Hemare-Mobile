package com.example.hemaremobile.data

import com.example.hemaremobile.data.api.ApiHemare
import com.example.hemaremobile.domain.Cadeia
import com.example.hemaremobile.domain.Datas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HemareStoreTest {
    private fun loja() = HemareStore()
    private fun idDe(s: HemareStore, email: String) = s.atual().usuarios.first { it.email == email }.id

    // ---------- sessão ----------
    @Test fun `entra com a conta de teste do doador`() {
        val s = loja()
        val r = s.entrar("doador@hemare.com", "doador123")
        assertTrue(r.msg, r.ok)
        assertEquals(TipoConta.DOADOR, s.atual().papel)
    }

    @Test fun `senha errada nao entra e sair limpa a sessao`() {
        val s = loja()
        assertFalse(s.entrar("doador@hemare.com", "errada123").ok)
        assertNull(s.atual().papel)
        s.entrar("doador@hemare.com", "doador123")
        s.sair()
        assertNull(s.atual().usuario)
    }

    @Test fun `a senha nunca fica guardada em texto`() {
        val u = loja().atual().usuarios.first { it.email == "doador@hemare.com" }
        assertNotEquals("doador123", u.senha)
        assertEquals(64, u.senha.length)
    }

    @Test fun `cada tipo de conta cai na sua area`() {
        val s = loja()
        s.entrar("admin@hemare.com", "admin1234"); assertEquals(TipoConta.ADMIN, s.atual().papel)
        s.entrar("hospital@hemare.com", "hospital123"); assertEquals(TipoConta.HOSPITAL, s.atual().papel)
    }

    // ---------- cadastro e senha ----------
    @Test fun `cadastra doador novo e ja entra`() {
        val s = loja()
        val r = s.cadastrarDoador("Pessoa Nova", "nova.pessoa@hemare.com", "Senha#Forte1")
        assertTrue(r.msg, r.ok)
        assertTrue(s.entrar("nova.pessoa@hemare.com", "Senha#Forte1").ok)
    }

    @Test fun `nao cadastra email repetido nem senha fraca`() {
        val s = loja()
        assertFalse(s.cadastrarDoador("Outra Maria", "doador@hemare.com", "Senha#Forte1").ok)
        assertFalse(s.cadastrarDoador("Pessoa Fraca", "fraca@hemare.com", "aaaaaaaa").ok)
        assertFalse(s.cadastrarDoador("X", "x@hemare.com", "Senha#Forte1").ok)
    }

    @Test fun `trocar a senha exige a atual e vale na proxima entrada`() {
        val s = loja()
        s.cadastrarDoador("Troca Senha", "troca.senha@hemare.com", "Senha#Forte1")
        s.entrar("troca.senha@hemare.com", "Senha#Forte1")
        assertFalse(s.alterarSenha("errada", "Outra#Senha2", "Outra#Senha2").ok)
        assertFalse(s.alterarSenha("Senha#Forte1", "Outra#Senha2", "diferente").ok)
        assertTrue(s.alterarSenha("Senha#Forte1", "Outra#Senha2", "Outra#Senha2").ok)
        s.sair()
        assertFalse(s.entrar("troca.senha@hemare.com", "Senha#Forte1").ok)
        assertTrue(s.entrar("troca.senha@hemare.com", "Outra#Senha2").ok)
    }

    @Test fun `recuperacao de senha com codigo`() {
        val s = loja()
        val codigo = s.pedirCodigoRecuperacao("doador@hemare.com")
        assertNotNull(codigo)
        assertFalse(s.redefinirSenha("doador@hemare.com", "000000x", "Nova#Senha12").ok)
        assertTrue(s.redefinirSenha("doador@hemare.com", codigo!!, "Nova#Senha12").ok)
        assertTrue(s.entrar("doador@hemare.com", "Nova#Senha12").ok)
    }

    // ---------- hospital: verificação e permissões ----------
    @Test fun `hospital pendente nao publica ate o admin aprovar`() {
        val s = loja()
        s.entrar("pendente@hemare.com", "hospital123")
        val bloqueado = s.publicarNecessidade("O+", "alerta")
        assertFalse(bloqueado.ok)
        assertTrue(bloqueado.msg.contains("verificação"))

        s.entrar("admin@hemare.com", "admin1234")
        assertTrue(s.aprovarHospital(idDe(s, "pendente@hemare.com")).ok)

        s.entrar("pendente@hemare.com", "hospital123")
        assertTrue(s.publicarNecessidade("O+", "alerta").ok)
    }

    @Test fun `recusa exige motivo com 10 caracteres e o hospital e bloqueado`() {
        val s = loja()
        s.entrar("admin@hemare.com", "admin1234")
        val id = idDe(s, "pendente@hemare.com")
        assertFalse(s.recusarHospital(id, "curto").ok)
        assertTrue(s.recusarHospital(id, "CNPJ não confere com o CNES informado").ok)
        s.entrar("pendente@hemare.com", "hospital123")
        assertFalse(s.definirEstoque("O+", "critico").ok)
    }

    @Test fun `so o admin aprova hospital`() {
        val s = loja()
        s.entrar("doador@hemare.com", "doador123")
        assertFalse(s.aprovarHospital(idDe(s, "pendente@hemare.com")).ok)
    }

    @Test fun `hospital aprovado define estoque que aparece no termometro publico`() {
        val s = loja()
        s.entrar("hospital@hemare.com", "hospital123")
        assertTrue(s.definirEstoque("AB-", "emergencia").ok)
        assertTrue(s.atual().estoquePublico().any { it.tipo == "AB-" && it.nivel == "emergencia" })
        assertFalse(s.definirEstoque("ZZ", "critico").ok)
        assertFalse(s.definirEstoque("O+", "inventado").ok)
    }

    // ---------- privacidade e cadeia de confiança ----------
    @Test fun `o match nunca mostra nome ou telefone de doador anonimo`() {
        val s = loja()
        s.entrar("hospital@hemare.com", "hospital123")
        val id = s.publicarNecessidade("AB+", "alerta").id
        val m = s.atual().matchDe(id)!!
        assertTrue(m.doadores.isNotEmpty())
        m.doadores.filter { !it.identificado }.forEach {
            assertNull(it.telefone)
            assertNull(it.doadorId)
        }
    }

    @Test fun `cada doacao encadeia no hash da anterior`() {
        val e0 = loja().atual()
        val a = e0.registrarDoacao(1, 2, Datas.hoje())
        val b = a.registrarDoacao(3, 2, Datas.hoje())
        val ultimaAntes = a.doacoes.last()
        val nova = b.doacoes.last()
        assertEquals(Cadeia.calcularHash(ultimaAntes.hash, 3, 2, Datas.hoje()), nova.hash)
        assertEquals(e0.doacoes.size + 2, b.doacoes.size)
    }

    @Test fun `hospital nao confirma doacao de quem ainda esta no intervalo`() {
        val s = loja()
        s.entrar("hospital@hemare.com", "hospital123")
        val recente = s.atual().doadores.values.firstOrNull { d ->
            d.visibilidade == "identificado" && s.atual().ultimaDoacaoDe(d.id)?.let { Datas.diasEntre(it, Datas.hoje()) < 60 } == true
        }
        if (recente != null) assertFalse(s.confirmarDoacao(recente.id).ok)
    }

    // ---------- LGPD ----------
    @Test fun `excluir a conta exige a senha e apaga os dados pessoais`() {
        val s = loja()
        s.cadastrarDoador("Apagar Conta", "apagar.conta@hemare.com", "Senha#Forte1")
        s.entrar("apagar.conta@hemare.com", "Senha#Forte1")
        val id = s.atual().usuario!!.id
        assertFalse(s.excluirConta("errada").ok)
        assertTrue(s.excluirConta("Senha#Forte1").ok)
        assertNull(s.atual().usuario)
        assertNull(s.atual().usuarioPorId(id))
        assertNull(s.atual().doadores[id])
        assertFalse(s.entrar("apagar.conta@hemare.com", "Senha#Forte1").ok)
    }

    // ---------- servidor ----------
    @Test fun `endereco do servidor e normalizado`() {
        assertEquals("http://192.168.0.10:3000", ApiHemare.normalizarBase(" 192.168.0.10:3000/ "))
        assertEquals("http://localhost:3000", ApiHemare.normalizarBase("localhost:3000"))
        assertEquals("https://hemare.exemplo.com", ApiHemare.normalizarBase("hemare.exemplo.com/"))
        assertEquals("https://hemare.exemplo.com", ApiHemare.normalizarBase("https://hemare.exemplo.com"))
    }

    @Test fun `guarda e limpa o endereco do servidor`() {
        val s = loja()
        s.definirServidor("localhost:3000")
        assertEquals("http://localhost:3000", s.atual().servidor)
        s.definirServidor("   ")
        assertNull(s.atual().servidor)
    }

    @Test fun `sincronizar sem endereco da erro claro`() {
        val s = loja()
        val r = kotlinx.coroutines.runBlocking { s.sincronizarComServidor() }
        assertFalse(r.ok)
    }

    @Test fun `locais do servidor mantem as coordenadas dos que o aparelho ja conhece`() {
        val local = LocalDoacao(1, "Hemope Recife", "Recife", "PE", "Rua A", null, -8.05, -34.88)
        val doServidor = listOf(
            LocalDoacao(10, "HEMOPE  Recife", "Recife", "PE", "Rua A, 100", "(81) 3182-4600", null, null),
            LocalDoacao(11, "Hemocentro Novo", "Natal", "RN", null, null, null, null)
        )
        val r = mesclarLocais(doServidor, listOf(local))
        assertEquals(-8.05, r[0].lat!!, 0.0001)
        assertNull(r[1].lat)
        assertEquals("(81) 3182-4600", r[0].telefone)
    }

    @Test fun `estoque baixado do servidor entra no termometro e no clima`() {
        val s = loja()
        s.definir(s.atual().copy(estoqueExterno = listOf(ItemEstoque("O-", "Hospital de Fora", "Natal", "RN", "emergencia"))))
        assertTrue(s.atual().estoquePublico().any { it.hospital == "Hospital de Fora" })
        assertTrue(s.atual().climaPorCidade().any { it.cidade == "Natal" && it.clima.rotulo == "Emergência" })
    }
}
