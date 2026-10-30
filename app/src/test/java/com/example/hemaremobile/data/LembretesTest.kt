package com.example.hemaremobile.data

import com.example.hemaremobile.domain.Datas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class LembretesTest {
    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val mariaId = 6 // doador@hemare.com na Seed

    /** A loja com a Maria logada, sem doações nem agendamentos dela, e com o lembrete de retorno ligado. */
    private fun lojaDaMaria(querLembrete: Boolean = true): HemareStore {
        val s = HemareStore()
        s.entrar("doador@hemare.com", "doador123")
        val e = s.atual()
        s.definir(
            e.copy(
                doadores = e.doadores + (mariaId to e.doadores.getValue(mariaId).copy(querLembrete = querLembrete, sexo = 'F')),
                doacoes = e.doacoes.filter { it.doadorId != mariaId },
                agendamentos = e.agendamentos.filter { it.doadorId != mariaId }
            )
        )
        return s
    }

    @Test fun `visitante nao recebe aviso`() {
        assertTrue(HemareStore().atual().lembretes().isEmpty())
    }

    @Test fun `lembrete de retorno cai as 9h do dia em que ela volta a poder doar`() {
        val s = lojaDaMaria()
        s.definir(s.atual().registrarDoacao(mariaId, 1, Datas.hoje().minusDays(10)))
        val r = s.atual().lembretes().single { it.id == IdsLembrete.RETORNO }
        val esperado = Datas.hoje().plusDays(80).atTime(9, 0).atZone(fuso).toInstant().toEpochMilli() // mulher: 90 dias
        assertEquals(esperado, r.quandoMs)
        assertTrue(r.titulo.contains("doar de novo"))
    }

    @Test fun `sem o lembrete ligado nao ha aviso de retorno`() {
        val s = lojaDaMaria(querLembrete = false)
        s.definir(s.atual().registrarDoacao(mariaId, 1, Datas.hoje().minusDays(10)))
        assertTrue(s.atual().lembretes().none { it.id == IdsLembrete.RETORNO })
    }

    @Test fun `quem ja pode doar nao recebe aviso de retorno`() {
        val s = lojaDaMaria()
        s.definir(s.atual().registrarDoacao(mariaId, 1, Datas.hoje().minusDays(120)))
        assertTrue(s.atual().lembretes().isEmpty())
    }

    @Test fun `doacao agendada gera aviso na vespera as 18h e 2 horas antes`() {
        val s = lojaDaMaria()
        val e = s.atual()
        val dia = Datas.hoje().plusDays(3)
        s.definir(e.copy(vagas = e.vagas + Vaga(9001, 1, dia, "10:00", 2), agendamentos = e.agendamentos + AgendamentoReg(9002, 9001, mariaId, "agendado")))
        val lista = s.atual().lembretes()
        val vespera = lista.single { it.id == IdsLembrete.VESPERA }
        val duasHoras = lista.single { it.id == IdsLembrete.DUAS_HORAS }
        assertEquals(dia.minusDays(1).atTime(18, 0).atZone(fuso).toInstant().toEpochMilli(), vespera.quandoMs)
        assertEquals(dia.atTime(8, 0).atZone(fuso).toInstant().toEpochMilli(), duasHoras.quandoMs)
        assertTrue(vespera.texto.contains("10:00"))
        assertTrue(vespera.texto.contains(s.atual().hospitalNome(1)))
    }

    @Test fun `agendamento cancelado nao gera aviso`() {
        val s = lojaDaMaria()
        val e = s.atual()
        val dia = Datas.hoje().plusDays(3)
        s.definir(e.copy(vagas = e.vagas + Vaga(9001, 1, dia, "10:00", 2), agendamentos = e.agendamentos + AgendamentoReg(9002, 9001, mariaId, "cancelado")))
        assertTrue(s.atual().lembretes().isEmpty())
    }

    @Test fun `notificacoes desligadas nao agendam nada`() {
        val s = lojaDaMaria()
        s.definir(s.atual().registrarDoacao(mariaId, 1, Datas.hoje().minusDays(10)))
        s.definirNotificacoes(false)
        assertTrue(s.atual().lembretes().isEmpty())
        s.definirNotificacoes(true)
        assertEquals(1, s.atual().lembretes().size)
    }

    @Test fun `hospital e administrador nao recebem aviso de doador`() {
        val s = HemareStore()
        s.entrar("hospital@hemare.com", "hospital123")
        assertTrue(s.atual().lembretes().isEmpty())
        s.entrar("admin@hemare.com", "admin1234")
        assertTrue(s.atual().lembretes().isEmpty())
    }

    @Test fun `a loja avisa quem escuta a cada mudanca`() {
        var chamadas = 0
        val s = HemareStore(repositorio = null, aoMudar = { chamadas++ })
        assertEquals(1, chamadas) // ao abrir
    }
}
