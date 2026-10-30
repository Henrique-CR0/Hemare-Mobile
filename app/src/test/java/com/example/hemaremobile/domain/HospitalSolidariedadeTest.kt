package com.example.hemaremobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.time.LocalDate

class CadeiaTest {
    @Test fun `o primeiro elo usa o genesis de 64 zeros`() {
        assertEquals("0".repeat(64), Cadeia.GENESIS)
    }

    @Test fun `o hash e sha256 de anterior doador hospital e data`() {
        val esperado = MessageDigest.getInstance("SHA-256").digest("${Cadeia.GENESIS}|1|2|2026-01-01".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertEquals(esperado, Cadeia.calcularHash(Cadeia.GENESIS, 1, 2, LocalDate.of(2026, 1, 1)))
    }

    @Test fun `mudar qualquer campo muda o hash`() {
        val base = Cadeia.calcularHash(Cadeia.GENESIS, 1, 2, LocalDate.of(2026, 1, 1))
        assertNotEquals(base, Cadeia.calcularHash(Cadeia.GENESIS, 9, 2, LocalDate.of(2026, 1, 1)))
        assertNotEquals(base, Cadeia.calcularHash(Cadeia.GENESIS, 1, 2, LocalDate.of(2026, 1, 2)))
        assertNotEquals(base, Cadeia.calcularHash(base, 1, 2, LocalDate.of(2026, 1, 1)))
    }
}

class EstoqueTest {
    @Test fun `o pior nivel decide o clima da cidade`() {
        assertEquals("Crítico", Estoque.climaDe(listOf("estavel", "critico")).rotulo)
        assertEquals("Emergência", Estoque.climaDe(listOf("alerta", "emergencia")).rotulo)
        assertEquals("Estável", Estoque.climaDe(emptyList()).rotulo)
    }

    @Test fun `emergencia pesa mais que estavel na ordenacao`() {
        assertTrue(Estoque.PESO.getValue("emergencia") < Estoque.PESO.getValue("estavel"))
    }
}

class AgendamentoTest {
    private val hoje = Datas.hoje()
    private val todosOsDias = listOf(0, 1, 2, 3, 4, 5, 6)

    @Test fun `abre vagas em lote para todos os dias e horarios`() {
        val r = Agendamento.validarLote(hoje.plusDays(1), hoje.plusDays(7), todosOsDias, listOf("08:00", "09:00"), 2)
        assertTrue(r is Validacao.Ok)
        assertEquals(14, (r as Validacao.Ok).valor.size)
    }

    @Test fun `recusa lote no passado sem dia sem horario ou capacidade invalida`() {
        assertTrue(Agendamento.validarLote(hoje.minusDays(1), hoje.plusDays(2), todosOsDias, listOf("08:00"), 2) is Validacao.Erro)
        assertTrue(Agendamento.validarLote(hoje.plusDays(1), hoje.plusDays(2), emptyList(), listOf("08:00"), 2) is Validacao.Erro)
        assertTrue(Agendamento.validarLote(hoje.plusDays(1), hoje.plusDays(2), todosOsDias, emptyList(), 2) is Validacao.Erro)
        assertTrue(Agendamento.validarLote(hoje.plusDays(1), hoje.plusDays(2), todosOsDias, listOf("25:00"), 2) is Validacao.Erro)
        assertTrue(Agendamento.validarLote(hoje.plusDays(1), hoje.plusDays(2), todosOsDias, listOf("08:00"), 0) is Validacao.Erro)
        assertTrue(Agendamento.validarLote(hoje.plusDays(1), hoje.plusDays(2), todosOsDias, listOf("08:00"), 21) is Validacao.Erro)
    }

    @Test fun `nao deixa abrir vagas alem de 60 dias`() {
        assertTrue(Agendamento.validarLote(hoje.plusDays(58), hoje.plusDays(65), todosOsDias, listOf("08:00"), 2) is Validacao.Erro)
    }

    @Test fun `capacidade nunca fica abaixo de quem ja agendou`() {
        assertNotNull(Agendamento.validarCapacidade(1, 2))
        assertNull(Agendamento.validarCapacidade(3, 2))
        assertNotNull(Agendamento.validarCapacidade(21, 0))
    }

    @Test fun `presenca so se confirma no dia e falta so depois do horario`() {
        assertNotNull(Agendamento.podeMudarStatus("agendado", "compareceu", hoje.plusDays(1), "08:00"))
        assertNull(Agendamento.podeMudarStatus("agendado", "compareceu", hoje, "08:00"))
        assertNotNull(Agendamento.podeMudarStatus("agendado", "faltou", hoje.plusDays(1), "08:00"))
        assertNull(Agendamento.podeMudarStatus("agendado", "faltou", hoje.minusDays(1), "08:00"))
        assertNotNull(Agendamento.podeMudarStatus("compareceu", "faltou", hoje.minusDays(1), "08:00"))
        assertNotNull(Agendamento.podeMudarStatus("agendado", "inventado", hoje, "08:00"))
    }

    @Test fun `so cancela agendamento ativo e que ainda nao passou`() {
        assertNull(Agendamento.podeCancelar("agendado", hoje.plusDays(2), "08:00"))
        assertNotNull(Agendamento.podeCancelar("agendado", hoje.minusDays(1), "08:00"))
        assertNotNull(Agendamento.podeCancelar("cancelado", hoje.plusDays(2), "08:00"))
    }

    @Test fun `regras para o doador agendar`() {
        val vaga = hoje.plusDays(3)
        assertNull(Agendamento.podeAgendar('F', null, emptyList(), vaga, "10:00", 2, 0, null))
        assertNotNull(Agendamento.podeAgendar(null, null, emptyList(), vaga, "10:00", 2, 0, null))
        assertTrue(Agendamento.podeAgendar('F', null, emptyList(), vaga, "10:00", 2, 2, null)!!.contains("lotado"))
        assertTrue(Agendamento.podeAgendar('F', null, emptyList(), vaga, "10:00", 2, 0, hoje.plusDays(1) to "09:00")!!.contains("agendamento"))
        // ainda no intervalo de 90 dias na data da vaga
        assertTrue(Agendamento.podeAgendar('F', hoje.minusDays(10), listOf(hoje.minusDays(10)), vaga, "10:00", 2, 0, null)!!.contains("apto"))
        // 3 doacoes no ano: limite da mulher
        val tres = listOf(hoje.minusDays(300), hoje.minusDays(200), hoje.minusDays(100))
        assertTrue(Agendamento.podeAgendar('F', null, tres, vaga, "10:00", 2, 0, null)!!.contains("limite"))
    }
}

class AlertaRadarTest {
    private val hoje = LocalDate.of(2026, 10, 30)
    private fun cand(id: Int, tipo: String = "O+", vis: String = "identificado", cidade: String = "Recife", ultima: LocalDate? = null, alerta: LocalDate? = null) =
        Candidato(id, tipo, 'M', cidade, "PE", vis, ultima, alerta)

    @Test fun `alerta chama so identificado compativel apto da cidade e nao chamado ha pouco`() {
        val doadores = listOf(
            cand(1),
            cand(2, vis = "anonimo"),
            cand(3, ultima = hoje.minusDays(10)),
            cand(4, cidade = "Olinda"),
            cand(5, alerta = hoje.minusDays(1)),
            cand(6, tipo = "A+")
        )
        val r = Alerta.selecionar(doadores, Compatibilidade.doadoresCompativeis("O+"), "recife", hoje)
        assertEquals(listOf(1), r.convocados.map { it.id })
        assertEquals(1, r.resumo.poupados)
    }

    @Test fun `radar conta quem esta apto agora e quem fica apto em 7 dias`() {
        val lista = listOf(cand(1), cand(2, ultima = hoje.minusDays(55)), cand(3, ultima = hoje.minusDays(40)), cand(4, ultima = hoje.minusDays(5)))
        val o = Radar.calcular(lista, hoje).first { it.tipo == "O+" }
        assertEquals(1, o.aptoAgora)
        assertEquals(1, o.em7Dias)
        assertEquals(1, o.em30Dias)
        assertEquals(1, o.maisDe30Dias)
    }
}

class ApadrinhamentoTest {
    private val hoje = LocalDate.of(2026, 10, 30)

    @Test fun `meta sugerida cobre a frequencia do paciente`() {
        assertEquals(6, Apadrinhamento.sugerirMeta(30))
        assertEquals(14, Apadrinhamento.sugerirMeta(14))
        assertEquals(2, Apadrinhamento.sugerirMeta(120))
    }

    @Test fun `apelido nunca pode ser o nome ou um documento`() {
        assertTrue(Apadrinhamento.validarApelido("Guerreiro Léo") is Validacao.Ok)
        assertTrue(Apadrinhamento.validarApelido("A") is Validacao.Erro)
        assertTrue(Apadrinhamento.validarApelido("João 12345") is Validacao.Erro)
        assertTrue(Apadrinhamento.validarApelido("Léo <b>") is Validacao.Erro)
    }

    @Test fun `quem pode apadrinhar`() {
        assertNull(Apadrinhamento.podeApadrinhar("O-", "A+", 0, false, true))
        assertNotNull(Apadrinhamento.podeApadrinhar("A+", "O-", 0, false, true))
        assertNotNull(Apadrinhamento.podeApadrinhar("O-", "A+", 3, false, true))
        assertNotNull(Apadrinhamento.podeApadrinhar("O-", "A+", 0, true, true))
        assertNotNull(Apadrinhamento.podeApadrinhar("O-", "A+", 0, false, false))
    }

    @Test fun `situacao da meta de padrinhos`() {
        val m = Apadrinhamento.situacaoMeta(3, 6)
        assertEquals(50, m.porcentagem)
        assertFalse(m.completo)
        assertTrue(Apadrinhamento.situacaoMeta(6, 6).completo)
    }

    @Test fun `hospital chama padrinhos no maximo uma vez por semana`() {
        assertTrue(Apadrinhamento.podeChamarAgora(null, hoje).pode)
        val cedo = Apadrinhamento.podeChamarAgora(hoje.minusDays(3), hoje)
        assertFalse(cedo.pode)
        assertEquals(4, cedo.diasRestantes)
        assertTrue(Apadrinhamento.podeChamarAgora(hoje.minusDays(7), hoje).pode)
    }
}

class CampanhaTest {
    private val hoje = LocalDate.of(2026, 10, 30)

    @Test fun `valida campanha com prazo padrao de 15 dias`() {
        val ok = Campanha.validar("Paciente Aurora", "5", "", true)
        assertEquals(15, (ok as Validacao.Ok).valor.prazoDias)
        assertEquals(5, ok.valor.metaBolsas)
    }

    @Test fun `exige autorizacao meta e prazo dentro do limite`() {
        assertTrue(Campanha.validar("Paciente Aurora", "5", "", false) is Validacao.Erro)
        assertTrue(Campanha.validar("Paciente Aurora", "0", "", true) is Validacao.Erro)
        assertTrue(Campanha.validar("Paciente Aurora", "31", "", true) is Validacao.Erro)
        assertTrue(Campanha.validar("Paciente Aurora", "5", "61", true) is Validacao.Erro)
    }

    @Test fun `situacao da campanha`() {
        val aberta = Campanha.situacao(10, 4, 2, hoje.plusDays(5), true, hoje)
        assertEquals("aberta", aberta.status)
        assertEquals(6, aberta.faltam)
        assertEquals(40, aberta.porcentagem)
        assertEquals("meta-atingida", Campanha.situacao(10, 10, 0, hoje.plusDays(5), true, hoje).status)
        assertEquals("expirada", Campanha.situacao(10, 1, 0, hoje.minusDays(1), true, hoje).status)
        assertEquals("encerrada", Campanha.situacao(10, 1, 0, hoje.plusDays(5), false, hoje).status)
    }

    @Test fun `regras da promessa de doacao`() {
        val sit = Campanha.situacao(10, 0, 0, hoje.plusDays(10), true, hoje)
        val expira = hoje.plusDays(10)
        assertNull(Campanha.validarPromessa(hoje.plusDays(2), sit, expira, false, null, 'F', hoje))
        assertNotNull(Campanha.validarPromessa(hoje.plusDays(2), sit, expira, true, null, 'F', hoje))
        assertNotNull(Campanha.validarPromessa(hoje.minusDays(1), sit, expira, false, null, 'F', hoje))
        assertNotNull(Campanha.validarPromessa(hoje.plusDays(31), sit, expira, false, null, 'F', hoje))
        assertNotNull(Campanha.validarPromessa(hoje.plusDays(11), sit, expira, false, null, 'F', hoje))
        assertNotNull(Campanha.validarPromessa(null, sit, expira, false, null, 'F', hoje))
        assertNotNull(Campanha.validarPromessa(hoje.plusDays(2), sit, expira, false, hoje.minusDays(10), 'F', hoje))
    }

    @Test fun `mensagem de compartilhar traz o link e quanto falta`() {
        val m = Campanha.mensagemCompartilhar("Aurora", "Hospital X", "Recife", 3, "https://x/y")
        assertTrue(m.contains("Faltam 3"))
        assertTrue(m.contains("https://x/y"))
    }
}

class RarosTest {
    private val hoje = LocalDate.of(2026, 10, 30)
    private fun raro(id: Int, status: String? = "confirmado", consent: Boolean = true, alcance: String = "cidade", cidade: String = "Recife", ultima: LocalDate? = null, notif: LocalDate? = null) =
        Raros.DoadorRaro(id, "rh-nulo", status, consent, alcance, cidade, "PE", 'M', ultima, notif)

    @Test fun `pedido exige fenotipo conhecido e motivo de 20 a 300 caracteres`() {
        assertTrue(Raros.validarPedido("rh-nulo", "Cirurgia de emergência, sem outra bolsa disponível", "") is Validacao.Ok)
        assertTrue(Raros.validarPedido("outro", "Cirurgia de emergência, sem outra bolsa disponível", "") is Validacao.Erro)
        assertTrue(Raros.validarPedido("rh-nulo", "curto", "") is Validacao.Erro)
        assertTrue(Raros.validarPedido("rh-nulo", "Cirurgia de emergência, sem outra bolsa disponível", "João 12345") is Validacao.Erro)
    }

    @Test fun `so chama confirmado com consentimento no alcance apto e nao chamado ha pouco`() {
        val lista = listOf(
            raro(1),
            raro(2, status = "declarado"),
            raro(3, consent = false),
            raro(4, cidade = "Olinda"),
            raro(5, ultima = hoje.minusDays(10)),
            raro(6, notif = hoje.minusDays(2)),
            raro(7, cidade = "Olinda", alcance = "estado")
        )
        val (convocados, resumo) = Raros.selecionar(lista, "rh-nulo", "Recife", "PE", hoje)
        assertEquals(listOf(1, 7), convocados.map { it.id }.sorted())
        assertEquals(5, resumo.naRede)
    }

    @Test fun `codigo tem 8 caracteres sem 0 O 1 e I`() {
        val c = Raros.gerarCodigo()
        assertEquals(8, c.length)
        assertTrue(c.all { it in Raros.ALFABETO })
        assertTrue(c.none { it in "0O1I" })
    }
}

class PcdTest {
    @Test fun `declarar exige tipo e consentimento`() {
        assertNull(Pcd.validarDeclaracao(false, emptyList(), false))
        assertNotNull(Pcd.validarDeclaracao(true, emptyList(), true))
        assertNotNull(Pcd.validarDeclaracao(true, listOf(Pcd.TIPOS[0].first), false))
        assertNull(Pcd.validarDeclaracao(true, listOf(Pcd.TIPOS[0].first), true))
    }
}
