package com.example.hemaremobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DatasTest {
    @Test fun `conta dias entre duas datas`() {
        assertEquals(30, Datas.diasEntre(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
        assertEquals(-1, Datas.diasEntre(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)))
    }

    @Test fun `le e formata dd mm aaaa`() {
        assertEquals(LocalDate.of(2026, 10, 9), Datas.lerDmy("09/10/2026"))
        assertEquals("09/10/2026", Datas.dmy(LocalDate.of(2026, 10, 9)))
        assertEquals("09/10", Datas.dm(LocalDate.of(2026, 10, 9)))
    }

    @Test fun `data inexistente ou incompleta nao vira data`() {
        assertNull(Datas.lerDmy("31/02/2026"))
        assertNull(Datas.lerDmy("09/10/26"))
    }

    @Test fun `mascara de digitacao coloca as barras`() {
        assertEquals("09/10/2026", Datas.mascaraDmy("09102026"))
        assertEquals("09/1", Datas.mascaraDmy("091"))
    }

    @Test fun `gera horarios do primeiro ao ultimo de hora em hora`() {
        assertEquals(listOf("08:00", "09:00", "10:00"), Datas.gerarHorarios("08:00", "10:00", 60))
        assertTrue(Datas.gerarHorarios("08:00", "10:00", 1).isEmpty())
        assertTrue(Datas.gerarHorarios("oito", "10:00", 60).isEmpty())
    }
}

class DocumentosTest {
    @Test fun `mascara cpf telefone cnpj e cep`() {
        assertEquals("529.982.247-25", Documentos.mascaraCpf("52998224725"))
        assertEquals("(81) 99999-0001", Documentos.mascaraTelefone("81999990001"))
        assertEquals("11.222.333/0001-81", Documentos.mascaraCnpj("11222333000181"))
        assertEquals("50000-000", Documentos.mascaraCep("50000000"))
    }

    @Test fun `valida o digito verificador do cpf`() {
        assertTrue(Documentos.cpfValido("529.982.247-25"))
        assertFalse(Documentos.cpfValido("529.982.247-24"))
        assertFalse(Documentos.cpfValido("111.111.111-11"))
        assertFalse(Documentos.cpfValido("123"))
    }

    @Test fun `valida o digito verificador do cnpj`() {
        assertTrue(Documentos.cnpjValido("11.222.333/0001-81"))
        assertFalse(Documentos.cnpjValido("11.222.333/0001-82"))
        assertFalse(Documentos.cnpjValido("00.000.000/0000-00"))
    }

    @Test fun `esconde o comeco e o fim do cpf`() {
        assertEquals("***.982.247-**", Documentos.mascararCpf("52998224725"))
        assertNull(Documentos.mascararCpf("123"))
    }
}

class CompatibilidadeTest {
    @Test fun `O negativo doa para todos`() {
        Compatibilidade.TIPOS_PEDIDO.forEach { assertTrue("O- para $it", Compatibilidade.podeDoar("O-", it)) }
    }

    @Test fun `AB positivo recebe de todos`() {
        assertEquals(8, Compatibilidade.doadoresCompativeis("AB+").size)
    }

    @Test fun `A positivo nao doa para O negativo`() {
        assertFalse(Compatibilidade.podeDoar("A+", "O-"))
        assertEquals(listOf("O-"), Compatibilidade.doadoresCompativeis("O-"))
    }

    @Test fun `tipo desconhecido nao e compativel com ninguem`() {
        assertFalse(Compatibilidade.podeDoar("O-", "XY"))
        assertTrue(Compatibilidade.doadoresCompativeis("XY").isEmpty())
    }
}

class ElegibilidadeTest {
    private val hoje = LocalDate.of(2026, 10, 30)

    @Test fun `primeira doacao esta apta`() {
        val r = Elegibilidade.verificar(null, 'F', hoje)
        assertTrue(r.apto)
        assertEquals(0, r.diasRestantes)
    }

    @Test fun `mulher espera 90 dias`() {
        val r = Elegibilidade.verificar(hoje.minusDays(30), 'F', hoje)
        assertFalse(r.apto)
        assertEquals(60, r.diasRestantes)
        assertTrue(Elegibilidade.verificar(hoje.minusDays(90), 'F', hoje).apto)
    }

    @Test fun `homem espera 60 dias`() {
        assertFalse(Elegibilidade.verificar(hoje.minusDays(59), 'M', hoje).apto)
        assertTrue(Elegibilidade.verificar(hoje.minusDays(60), 'M', hoje).apto)
    }

    @Test fun `sem sexo informado nao ha como calcular`() {
        val r = Elegibilidade.verificar(null, null, hoje)
        assertFalse(r.apto)
        assertNull(r.diasRestantes)
        assertNull(Elegibilidade.aptoEmDia(null, null, hoje))
    }

    @Test fun `diz em que dia a pessoa fica apta`() {
        assertEquals(LocalDate.of(2026, 3, 2), Elegibilidade.aptoEmDia(LocalDate.of(2026, 1, 1), 'M', LocalDate.of(2026, 2, 1)))
    }

    @Test fun `limites anuais`() {
        assertEquals(4, Elegibilidade.LIMITE_ANUAL['M'])
        assertEquals(3, Elegibilidade.LIMITE_ANUAL['F'])
    }
}

class DistanciaTest {
    @Test fun `recife a sao paulo fica perto de 2100 km`() {
        val km = Distancia.calcular(-8.0476, -34.8770, -23.5505, -46.6333)
        assertTrue("deu $km", km in 2000..2300)
    }

    @Test fun `mesmo ponto tem distancia zero`() {
        assertEquals(0, Distancia.calcular(-8.0, -34.0, -8.0, -34.0))
    }
}

class TextosTest {
    @Test fun `normaliza acentos e caixa`() {
        assertEquals("sao paulo", Textos.normalizar("São Paulo"))
        assertEquals(Textos.normalizarCidade("RECIFE"), Textos.normalizarCidade("  Récife "))
    }

    @Test fun `limpar junta espacos repetidos`() {
        assertEquals("a b c", Textos.limpar("  a   b \n c "))
    }
}
