package com.example.hemaremobile

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hemaremobile.data.HemareStore
import com.example.hemaremobile.data.AgendamentoReg
import com.example.hemaremobile.data.Seed
import com.example.hemaremobile.data.TipoConta
import com.example.hemaremobile.data.Vaga
import com.example.hemaremobile.data.api.Conversores
import com.example.hemaremobile.data.local.BancoHemare
import com.example.hemaremobile.data.verificarCadeia
import com.example.hemaremobile.domain.Datas
import com.example.hemaremobile.services.AgendadorLembretes
import com.example.hemaremobile.services.LembreteReceiver
import com.example.hemaremobile.services.Notificacoes
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de integração no aparelho: usam o Android de verdade (SQLite, org.json, AlarmManager e notificações).
 * Cada banco de teste tem nome próprio, então não mexem nos dados do app.
 */
@RunWith(AndroidJUnit4::class)
class IntegracaoNoAparelhoTest {
    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nomeBanco = "teste-integracao.db"

    @Before fun limpar() { contexto.deleteDatabase(nomeBanco) }

    @After fun limparDepois() { contexto.deleteDatabase(nomeBanco) }

    private fun banco() = BancoHemare(contexto, nomeBanco)

    // ---------------------------------------------------------------- banco de dados
    @Test fun bancoNovoComecaVazio() {
        val b = banco()
        assertNull(b.carregar())
        b.close()
    }

    @Test fun guardaECarregaTudoQueOAppSabe() {
        val original = Seed.criar().let { it.copy(sessaoUsuarioId = it.usuarios.first { u -> u.email == "doador@hemare.com" }.id, servidor = "http://10.0.2.2:3000", notificacoes = false) }
        val b1 = banco()
        b1.salvar(original)
        b1.close()

        val b2 = banco()
        val carregado = try { b2.carregar() } finally { b2.close() }
        assertNotNull(carregado)
        carregado!!
        assertEquals(original.usuarios, carregado.usuarios)
        assertEquals(original.doadores.keys, carregado.doadores.keys)
        assertEquals(original.hospitais, carregado.hospitais)
        assertEquals(original.doacoes, carregado.doacoes)
        assertEquals(original.estoque, carregado.estoque)
        assertEquals(original.necessidades, carregado.necessidades)
        assertEquals(original.vagas, carregado.vagas)
        assertEquals(original.casos, carregado.casos)
        assertEquals(original.campanhas, carregado.campanhas)
        assertEquals(original.noticias.size, carregado.noticias.size)
        assertEquals(original.proximoId, carregado.proximoId)
        assertEquals(original.sessaoUsuarioId, carregado.sessaoUsuarioId)
        assertEquals("http://10.0.2.2:3000", carregado.servidor)
        assertFalse(carregado.notificacoes)
        assertTrue("a cadeia tem de continuar íntegra depois de ir e voltar do banco", carregado.verificarCadeia().integra)
    }

    @Test fun salvarDeNovoSubstituiEmVezDeDuplicar() {
        val e = Seed.criar()
        val b = banco()
        b.salvar(e); b.salvar(e); b.salvar(e)
        val carregado = b.carregar()!!
        assertEquals(e.usuarios.size, carregado.usuarios.size)
        assertEquals(e.doacoes.size, carregado.doacoes.size)
        b.close()
    }

    @Test fun fotoDoDoadorSobreviveAoBanco() {
        val e = Seed.criar()
        val id = e.usuarios.first { it.email == "doador@hemare.com" }.id
        val foto = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val comFoto = e.copy(doadores = e.doadores + (id to e.doadores.getValue(id).copy(foto = foto)))
        val b = banco()
        b.salvar(comFoto)
        val lida = b.carregar()!!.doadores.getValue(id).foto
        b.close()
        assertNotNull(lida)
        assertEquals(8, lida!!.width)
    }

    @Test fun senhaNuncaFicaEmTextoNoArquivoDoBanco() {
        val b = banco()
        b.salvar(Seed.criar())
        val cursor = b.readableDatabase.rawQuery("SELECT json FROM usuarios", null)
        var achouTexto = false
        var linhas = 0
        cursor.use { while (it.moveToNext()) { linhas++; val j = it.getString(0); if (j.contains("doador123") || j.contains("hospital123") || j.contains("admin1234")) achouTexto = true } }
        b.close()
        assertTrue(linhas > 0)
        assertFalse("uma senha apareceu em texto no banco", achouTexto)
    }

    @Test fun bancoPelaMetadeEhRecriadoEmVezDeTravarOApp() {
        val b = banco()
        b.salvar(Seed.criar())
        b.writableDatabase.execSQL("DROP TABLE meta") // simula o banco incompleto que já derrubou o app
        assertNull(b.carregar()) // não pode lançar exceção
        b.close()
        // depois disso o banco volta a funcionar normalmente
        val novo = banco()
        novo.salvar(Seed.criar())
        assertNotNull(novo.carregar())
        novo.close()
    }

    // ---------------------------------------------------------------- loja + banco (o app "reaberto")
    @Test fun sessaoContinuaDepoisDeReabrirOApp() {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        lateinit var primeira: HemareStore
        instrumentacao.runOnMainSync { primeira = HemareStore(banco()) }
        assertTrue(primeira.entrar("doador@hemare.com", "doador123").ok)
        Thread.sleep(1500) // a gravação acontece 300 ms depois da última mudança

        // "reabre" o app: uma loja nova, lendo o mesmo banco
        lateinit var reaberta: HemareStore
        instrumentacao.runOnMainSync { reaberta = HemareStore(banco()) }
        assertEquals(TipoConta.DOADOR, reaberta.atual().papel)
        assertEquals("doador@hemare.com", reaberta.atual().usuario!!.email)
    }

    @Test fun contaCriadaNoAppSobreviveEPermiteEntrarDepois() {
        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        lateinit var primeira: HemareStore
        instrumentacao.runOnMainSync { primeira = HemareStore(banco()) }
        assertTrue(primeira.cadastrarDoador("Pessoa Persistente", "persistente@hemare.com", "Senha#Forte1").ok)
        primeira.sair()
        Thread.sleep(1500)

        lateinit var reaberta: HemareStore
        instrumentacao.runOnMainSync { reaberta = HemareStore(banco()) }
        assertNull(reaberta.atual().papel) // saiu antes de fechar
        assertTrue(reaberta.entrar("persistente@hemare.com", "Senha#Forte1").ok)
    }

    // ---------------------------------------------------------------- API (leitura das respostas)
    @Test fun conversoresLeemOFormatoDaApiDoSite() {
        val locais = Conversores.locais(
            JSONArray("""[{"id":1,"nome":"Hemope","cidade":"Recife","estado":"PE","endereco":"Rua A, 100","telefone":null,"tipo":"hemocentro"}]""")
        )
        assertEquals("Hemope", locais.single().nome)
        assertNull(locais.single().telefone)

        val estoque = Conversores.estoque(
            JSONArray("""[{"hospital":"Hospital X","cidade":"Natal","estado":"RN","tipo_sanguineo":"O-","nivel":"critico","atualizado_em":"2026-10-30T10:00:00.000Z"}]""")
        )
        assertEquals("O-", estoque.single().tipo)
        assertEquals("critico", estoque.single().nivel)

        val noticias = Conversores.noticias(
            JSONObject("""{"itens":[{"id":7,"titulo":"Hemocentro precisa de doadores de sangue","resumo":"Estoque baixo","fonte":"Agência","url":"https://exemplo.gov.br/a","imagem":null,"categoria":"estoque","uf":"PE","publicadaEm":"2026-10-30T12:00:00.000Z"}]}""").getJSONArray("itens"),
            1
        )
        assertEquals("estoque", noticias.single().categoria)
        assertEquals("PE", noticias.single().uf)
        assertNull(noticias.single().imagem)
        assertTrue(noticias.single().publicadaEm > 0)
    }

    @Test fun erroDaApiViraFraseParaAPessoa() {
        assertEquals("Token inválido", Conversores.mensagemDeErro(401, """{"erro":"Token inválido"}"""))
        assertTrue(Conversores.mensagemDeErro(401, "<html>isto não é json</html>").contains("Sessão expirada"))
        assertTrue(Conversores.mensagemDeErro(500, "").contains("500"))
    }

    // ---------------------------------------------------------------- serviços do Android
    @Test fun agendadorRegistraOsAlarmesDaDoacaoEOsRemoveAoCancelar() {
        val base = Seed.criar()
        val mariaId = base.usuarios.first { it.email == "doador@hemare.com" }.id
        val dia = Datas.hoje().plusDays(5)
        val comAgendamento = base.copy(
            sessaoUsuarioId = mariaId,
            vagas = base.vagas + Vaga(9001, 1, dia, "10:00", 2),
            agendamentos = base.agendamentos.filter { it.doadorId != mariaId } + AgendamentoReg(9002, 9001, mariaId, "agendado")
        )
        val agendador = AgendadorLembretes(contexto)

        val ids = agendador.reagendar(comAgendamento)
        assertTrue("véspera e 2 horas antes", ids.containsAll(listOf(2, 3)))
        assertNotNull(alarme(2)); assertNotNull(alarme(3))

        agendador.reagendar(comAgendamento.copy(agendamentos = comAgendamento.agendamentos.filter { it.id != 9002 }))
        assertNull("cancelou o agendamento: o alarme da véspera tem de sumir", alarme(2))
        assertNull(alarme(3))
    }

    @Test fun notificacaoApareceNaBandejaQuandoHaPermissao() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(contexto.packageName, Notificacoes.PERMISSAO)
        assertTrue(Notificacoes.mostrar(contexto, 98, "Teste de integração", "Notificação enviada pelo teste automatizado."))
        val gerente = contexto.getSystemService(NotificationManager::class.java)
        // O Android publica a notificação de forma assíncrona: espera até 5 segundos em vez de olhar só uma vez.
        var achada = false
        val limite = System.currentTimeMillis() + 5000
        while (!achada && System.currentTimeMillis() < limite) {
            achada = gerente.activeNotifications.any { it.id == 98 }
            if (!achada) Thread.sleep(100)
        }
        gerente.cancel(98)
        assertTrue("a notificação não apareceu na bandeja em 5 segundos", achada)
    }

    private fun alarme(id: Int): PendingIntent? = PendingIntent.getBroadcast(
        contexto, id, Intent(contexto, LembreteReceiver::class.java).setAction("hemare.lembrete.$id"),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
}
