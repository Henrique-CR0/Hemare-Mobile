package com.example.hemaremobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AuthTest {
    @Test fun `email precisa de arroba e dominio`() {
        assertTrue(Auth.emailValido("maria@hemare.com"))
        assertFalse(Auth.emailValido("maria@hemare"))
        assertFalse(Auth.emailValido("maria hemare.com"))
        assertEquals("maria@hemare.com", Auth.normalizarEmail("  Maria@Hemare.COM "))
    }

    @Test fun `medidor de forca vai de 0 a 3`() {
        assertEquals(0, Auth.forcaSenha("abc"))
        assertEquals(1, Auth.forcaSenha("abcdefgh"))
        assertEquals(2, Auth.forcaSenha("Abcdefgh"))
        assertEquals(3, Auth.forcaSenha("Abcdef1!"))
    }

    @Test fun `recusa senha curta repetida ou igual ao email`() {
        assertTrue(Auth.validarSenha("curta") is Validacao.Erro)
        assertTrue(Auth.validarSenha("aaaaaaaa") is Validacao.Erro)
        assertTrue(Auth.validarSenha("maria1234", "maria1234@hemare.com") is Validacao.Erro)
        assertTrue(Auth.validarSenha("Maria Silva", "x@hemare.com", "maria silva") is Validacao.Erro)
        assertTrue(Auth.validarSenha("Senha#Forte1", "maria@hemare.com", "Maria") is Validacao.Ok)
    }

    @Test fun `nome do cadastro e limpo e validado`() {
        assertEquals("Maria Silva", (Auth.validarNomeCadastro("  Maria   Silva ") as Validacao.Ok).valor)
        assertTrue(Auth.validarNomeCadastro("A") is Validacao.Erro)
        assertTrue(Auth.validarNomeCadastro("<script>") is Validacao.Erro)
    }

    @Test fun `oito erros seguidos travam o login e limpar libera`() {
        val chave = "teste-rate@hemare.com"
        val t0 = 1_000_000L
        repeat(7) { Auth.registrarFalha(chave, t0) }
        assertEquals(0, Auth.bloqueio(chave, t0))
        Auth.registrarFalha(chave, t0)
        assertTrue(Auth.bloqueio(chave, t0 + 1000) > 0)
        assertEquals(0, Auth.bloqueio(chave, t0 + 16 * 60 * 1000L)) // passou a janela de 15 min
        Auth.limparFalhas(chave)
        assertEquals(0, Auth.bloqueio(chave, t0))
    }
}

class SenhasTest {
    @Test fun `o resumo nao guarda a senha e e sempre o mesmo`() {
        val h = Senhas.hash("doador123", "doador@hemare.com")
        assertEquals(64, h.length)
        assertFalse(h.contains("doador123"))
        assertEquals(h, Senhas.hash("doador123", " Doador@Hemare.com "))
    }

    @Test fun `senha ou email diferentes dao resumos diferentes`() {
        assertNotEquals(Senhas.hash("doador123", "a@hemare.com"), Senhas.hash("doador124", "a@hemare.com"))
        assertNotEquals(Senhas.hash("doador123", "a@hemare.com"), Senhas.hash("doador123", "b@hemare.com"))
    }
}

class PerfilTest {
    @Test fun `calcula a idade no dia do aniversario`() {
        assertEquals(25, Perfil.idadeEm(LocalDate.of(2000, 6, 15), LocalDate.of(2026, 6, 14)))
        assertEquals(26, Perfil.idadeEm(LocalDate.of(2000, 6, 15), LocalDate.of(2026, 6, 15)))
    }

    @Test fun `anonimo nao mostra nome nem contato ao hospital`() {
        val v = Perfil.visaoDoHospital("Maria Silva", null, "O+", "Recife", "anonimo", "(81) 99999-0001")
        assertFalse(v.identificado)
        assertTrue(v.ve.none { it.second.contains("Maria") || it.second.contains("99999") })
    }

    @Test fun `identificado mostra nome e telefone`() {
        val v = Perfil.visaoDoHospital("Maria Silva", null, "O+", "Recife", "identificado", "(81) 99999-0001")
        assertTrue(v.identificado)
        assertTrue(v.ve.any { it.second.contains("Maria") })
        assertTrue(v.ve.any { it.second.contains("99999") })
    }

    @Test fun `nome social aparece no lugar do nome civil`() {
        val v = Perfil.visaoDoHospital("Mario Silva", "Maria", "O+", "Recife", "identificado", null)
        assertTrue(v.ve.any { it.second == "Maria" })
    }
}

class TriagemTest {
    @Test fun `sem nenhum impedimento fica verde`() {
        assertEquals(NivelTriagem.VERDE, Triagem.avaliar(30, 70, emptyMap()).nivel)
    }

    @Test fun `doenca que impede deixa vermelho`() {
        val r = Triagem.avaliar(30, 70, mapOf("temHIV" to true))
        assertEquals(NivelTriagem.VERMELHO, r.nivel)
        assertTrue(r.motivos.isNotEmpty())
    }

    @Test fun `peso abaixo de 50 kg e idade abaixo de 16 deixam vermelho`() {
        assertEquals(NivelTriagem.VERMELHO, Triagem.avaliar(30, 45, emptyMap()).nivel)
        assertEquals(NivelTriagem.VERMELHO, Triagem.avaliar(15, 70, emptyMap()).nivel)
    }

    @Test fun `menor de 18 com autorizacao e situacao recente deixam amarelo`() {
        assertEquals(NivelTriagem.AMARELO, Triagem.avaliar(17, 70, emptyMap()).nivel)
        assertEquals(NivelTriagem.AMARELO, Triagem.avaliar(30, 70, mapOf("bebidaAlcoolica" to true)).nivel)
        assertEquals(NivelTriagem.AMARELO, Triagem.avaliar(30, 70, mapOf("usaMedicacaoContinua" to true)).nivel)
    }

    @Test fun `impedimento ganha de atencao`() {
        assertEquals(NivelTriagem.VERMELHO, Triagem.avaliar(30, 70, mapOf("temHepatiteC" to true, "temDiabetes" to true)).nivel)
    }

    @Test fun `avisos em tempo real dos campos`() {
        assertTrue(Triagem.avisoIdade(15).contains("16"))
        assertEquals("", Triagem.avisoIdade(30))
        assertEquals("", Triagem.avisoIdade(null))
        assertTrue(Triagem.avisoPeso(45).contains("50"))
        assertEquals("", Triagem.avisoPeso(60))
    }
}

class GamificacaoTest {
    @Test fun `placar esconde cidade com menos de tres doadores`() {
        val p = Gamificacao.montarPlacar(listOf(LinhaPlacar("Recife", 5, 10), LinhaPlacar("Olinda", 2, 20), LinhaPlacar("Caruaru", 3, 4)))
        assertEquals(listOf("Recife", "Caruaru"), p.cidades.map { it.cidade })
        assertEquals(listOf(1, 2), p.cidades.map { it.posicao })
        assertEquals(34, p.totalDoacoes)
        assertEquals(10, p.totalDoadores)
    }
}
