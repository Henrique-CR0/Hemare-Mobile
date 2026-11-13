package com.example.hemaremobile.data

import com.example.hemaremobile.domain.Datas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de integração: cada teste percorre uma jornada inteira, passando por vários componentes ao mesmo tempo
 * (contas, regras do domínio, loja, agenda, cadeia de confiança e privacidade), como uma pessoa usaria o app.
 */
class JornadasIntegracaoTest {
    private fun idDe(s: HemareStore, email: String) = s.atual().usuarios.first { it.email == email }.id

    /** A loja com a Maria logada, sem doações nem agendamentos dela (pode doar e agendar). */
    private fun lojaDaMaria(): HemareStore {
        val s = HemareStore()
        s.entrar("doador@hemare.com", "doador123")
        val e = s.atual()
        val id = idDe(s, "doador@hemare.com")
        s.definir(e.copy(doacoes = e.doacoes.filter { it.doadorId != id }, agendamentos = e.agendamentos.filter { it.doadorId != id }))
        return s
    }

    /** Um doador identificado, compatível com o tipo e que já pode doar hoje (escolhido nos dados, sem alterar a cadeia). */
    private fun doadorApto(s: HemareStore, receptor: String): Doador = s.atual().doadores.values.first { d ->
        d.visibilidade == "identificado" && com.example.hemaremobile.domain.Compatibilidade.podeDoar(d.tipoSanguineo, receptor) &&
            com.example.hemaremobile.domain.Elegibilidade.verificar(s.atual().ultimaDoacaoDe(d.id), d.sexo).apto
    }

    private fun vagasDoHospital1(s: HemareStore, horas: List<String>, aPartirDeDias: Long = 3): List<Vaga> {
        val dia = Datas.hoje().plusDays(aPartirDeDias)
        return s.atual().vagas.filter { it.hospitalId == 1 && !it.data.isBefore(dia) }
            .groupBy { it.data }.entries.sortedBy { it.key }
            .first { (_, v) -> horas.all { h -> v.any { it.hora == h } } }
            .value.filter { it.hora in horas }.sortedBy { it.hora }
    }

    // ---------------------------------------------------------------- cadeia
    @Test fun `a cadeia dos dados de exemplo esta integra`() {
        val r = HemareStore().atual().verificarCadeia()
        assertTrue(r.total > 0)
        assertTrue("primeiro inválido: ${r.primeiroInvalidoId}", r.integra)
    }

    @Test fun `adulterar uma doacao quebra a cadeia e aponta onde`() {
        val e = HemareStore().atual()
        val alvo = e.doacoes[3]
        val adulterado = e.copy(doacoes = e.doacoes.map { if (it.id == alvo.id) it.copy(hospitalId = alvo.hospitalId + 1) else it })
        val r = adulterado.verificarCadeia()
        assertFalse(r.integra)
        assertEquals(alvo.id, r.primeiroInvalidoId)
    }

    @Test fun `apagar uma doacao do meio tambem quebra a cadeia`() {
        val e = HemareStore().atual()
        val semUma = e.copy(doacoes = e.doacoes.filterIndexed { i, _ -> i != 2 })
        assertFalse(semUma.verificarCadeia().integra)
    }

    // ---------------------------------------------------------------- hospital novo
    @Test fun `jornada do hospital novo - cadastro, analise, aprovacao e termometro publico`() {
        val s = HemareStore()
        val cad = s.cadastrarHospital(
            HemareStore.DadosHospital(
                "Hospital Jornada Teste", "jornada.hospital@hemare.com", "Senha#Forte1", "12.345.678/0001-95", "1234567",
                "50000-000", "Rua das Flores", "10", "Centro", "", "Olinda", "PE"
            )
        )
        assertTrue(cad.msg, cad.ok)
        assertEquals(TipoConta.HOSPITAL, s.atual().papel)

        // pendente: não publica nem aparece no termômetro
        assertTrue(s.publicarNecessidade("O+", "alerta").msg.contains("verificação"))
        assertFalse(s.definirEstoque("O-", "emergencia").ok)
        assertTrue(s.atual().estoquePublico().none { it.hospital == "Hospital Jornada Teste" })

        // o administrador aprova
        s.entrar("admin@hemare.com", "admin1234")
        assertTrue(s.aprovarHospital(idDe(s, "jornada.hospital@hemare.com")).ok)

        // agora opera, e o termômetro público e a previsão do sangue mostram
        s.entrar("jornada.hospital@hemare.com", "Senha#Forte1")
        assertTrue(s.definirEstoque("O-", "emergencia").ok)
        assertTrue(s.publicarNecessidade("O-", "emergencia").ok)
        assertTrue(s.atual().estoquePublico().any { it.hospital == "Hospital Jornada Teste" && it.nivel == "emergencia" })
        assertTrue(s.atual().climaPorCidade().any { it.cidade == "Olinda" && it.clima.rotulo == "Emergência" })
    }

    @Test fun `recusar e depois corrigir o perfil volta o hospital para analise`() {
        val s = HemareStore()
        s.entrar("admin@hemare.com", "admin1234")
        val id = idDe(s, "pendente@hemare.com")
        assertTrue(s.recusarHospital(id, "CNPJ não confere com o CNES informado").ok)
        s.entrar("pendente@hemare.com", "hospital123")
        assertEquals("RECUSADO", s.atual().hospitalAtual!!.status.name)
        val h = s.atual().hospitalAtual!!
        val salvo = s.salvarPerfilHospital("Hospital Aguardando Aprovação (exemplo)", h.cnpj, h.cnes, h.cep, h.endereco, h.numero, h.bairro, "", h.cidade, h.estado)
        assertTrue(salvo.msg, salvo.ok)
        assertEquals("PENDENTE", s.atual().hospitalAtual!!.status.name)
    }

    // ---------------------------------------------------------------- doação
    @Test fun `jornada da doacao - necessidade, doador compativel, confirmacao e cadeia`() {
        val s = HemareStore()
        val brunoId = doadorApto(s, "O+").id // um doador que já pode doar
        s.entrar("hospital@hemare.com", "hospital123")

        val nec = s.publicarNecessidade("O+", "critico")
        assertTrue(nec.msg, nec.ok)
        val match = s.atual().matchDe(nec.id)!!
        assertTrue(match.doadores.any { it.doadorId == brunoId && it.identificado })

        val antes = s.atual().doacoesDe(brunoId).size
        assertTrue(s.confirmarDoacao(brunoId).ok)
        assertEquals(antes + 1, s.atual().doacoesDe(brunoId).size)
        assertTrue(s.atual().verificarCadeia().integra)

        // logo em seguida ele ainda está no intervalo e o hospital não pode confirmar de novo
        assertFalse(s.confirmarDoacao(brunoId).ok)
    }

    @Test fun `o alerta de emergencia chama so quem pode ajudar e marca a data do aviso`() {
        val s = HemareStore()
        val aptoId = s.atual().doadores.values.first { d ->
            d.visibilidade == "identificado" && d.cidade == "Recife" && com.example.hemaremobile.domain.Compatibilidade.podeDoar(d.tipoSanguineo, "O+") &&
                com.example.hemaremobile.domain.Elegibilidade.verificar(s.atual().ultimaDoacaoDe(d.id), d.sexo).apto
        }.id
        s.entrar("hospital@hemare.com", "hospital123")
        val r = s.publicarNecessidade("O+", "emergencia")
        assertTrue(r.msg, r.ok)
        assertEquals(Datas.hoje(), s.atual().doadores.getValue(aptoId).ultimoAlerta)
        // a Carla é anônima: nunca é chamada, mesmo sendo compatível (B+ não doa para O+, mas a regra vale para qualquer anônimo)
        assertNull(s.atual().doadores.getValue(idDe(s, "carla@exemplo.com")).ultimoAlerta)
    }

    // ---------------------------------------------------------------- privacidade
    @Test fun `jornada de privacidade - anonimo nao vaza no match nem na agenda do hospital`() {
        val s = HemareStore()
        val carlaId = idDe(s, "carla@exemplo.com") // anônima
        s.entrar("hospital@hemare.com", "hospital123")
        val dia = Datas.hoje().plusDays(3)
        val e = s.atual()
        s.definir(e.copy(vagas = e.vagas + Vaga(9001, 1, dia, "10:00", 2), agendamentos = e.agendamentos + AgendamentoReg(9002, 9001, carlaId, "agendado")))

        val agendados = s.atual().agendadosDoDia(1, dia)
        val dela = agendados.single { it.id == 9002 }
        assertNull(dela.telefone)

        val nec = s.publicarNecessidade("B+", "alerta")
        val match = s.atual().matchDe(nec.id)!!
        val naMatch = match.doadores.first { it.tipo == "B+" && !it.identificado }
        assertNull(naMatch.telefone)
        assertNull(naMatch.doadorId)
    }

    // ---------------------------------------------------------------- agenda (agendar, conflito, reagendar, cancelar, lotação)
    @Test fun `agendar bloqueia segundo agendamento ativo e libera depois do cancelamento`() {
        val s = lojaDaMaria()
        val (a, b) = vagasDoHospital1(s, listOf("09:00", "10:00"))

        val primeiro = s.agendar(a.id)
        assertTrue(primeiro.msg, primeiro.ok)
        assertNotNull(s.atual().agendamentoAtivoDe(s.atual().sessaoUsuarioId!!))

        val segundo = s.agendar(b.id)
        assertFalse(segundo.ok)
        assertTrue(segundo.msg.contains("agendamento"))

        // reagendar troca de horário sem ficar com dois
        val agId = s.atual().agendamentos.last { it.vagaId == a.id }.id
        val troca = s.reagendar(agId, b.id)
        assertTrue(troca.msg, troca.ok)
        assertEquals(b.id, s.atual().agendamentoAtivoDe(s.atual().sessaoUsuarioId!!)!!.vagaId)

        // cancelar libera a vaga e a pessoa pode marcar de novo
        val agora = s.atual().agendamentoAtivoDe(s.atual().sessaoUsuarioId!!)!!
        assertTrue(s.cancelarAgendamento(agora.id).ok)
        assertNull(s.atual().agendamentoAtivoDe(s.atual().sessaoUsuarioId!!))
        assertTrue(s.agendar(a.id).ok)
    }

    @Test fun `horario lotado nao aceita novo agendamento`() {
        val s = lojaDaMaria()
        val (a, _) = vagasDoHospital1(s, listOf("09:00", "10:00"))
        val e = s.atual()
        val outroId = idDe(s, "ana@exemplo.com")
        s.definir(
            e.copy(
                vagas = e.vagas.map { if (it.id == a.id) it.copy(capacidade = 1) else it },
                agendamentos = e.agendamentos + AgendamentoReg(9100, a.id, outroId, "agendado")
            )
        )
        val r = s.agendar(a.id)
        assertFalse(r.ok)
        assertTrue(r.msg, r.msg.contains("lotado"))
    }

    // ---------------------------------------------------------------- LGPD
    @Test fun `excluir a conta de quem ja doou mantem a cadeia de confianca integra`() {
        val s = HemareStore()
        s.entrar("doador@hemare.com", "doador123")
        val id = s.atual().sessaoUsuarioId!!
        assertTrue("a Maria precisa ter doações na Seed para este teste", s.atual().doacoesDe(id).isNotEmpty())
        assertTrue(s.atual().verificarCadeia().integra)

        assertTrue(s.excluirConta("doador123").ok)

        assertNull(s.atual().usuarioPorId(id))
        assertNull(s.atual().doadores[id])
        assertTrue("a exclusão da conta não pode quebrar a cadeia de auditoria", s.atual().verificarCadeia().integra)
    }
}
