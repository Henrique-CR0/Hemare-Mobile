package com.example.hemaremobile.data

import com.example.hemaremobile.domain.Agendamento
import com.example.hemaremobile.domain.Elegibilidade
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Um aviso que o Android deve mostrar numa data futura, mesmo com o app fechado. */
data class Lembrete(val id: Int, val quandoMs: Long, val titulo: String, val texto: String)

object IdsLembrete {
    const val RETORNO = 1 // "você já pode doar de novo"
    const val VESPERA = 2 // véspera da doação agendada
    const val DUAS_HORAS = 3 // 2 horas antes da doação agendada
    val TODOS = listOf(RETORNO, VESPERA, DUAS_HORAS)
}

private val FUSO = ZoneId.of("America/Sao_Paulo")
private const val DUAS_HORAS_MS = 2 * 3600 * 1000L

/**
 * Os avisos que a pessoa logada deve receber (só doador, com as notificações ligadas):
 * - "lembrete de retorno" (opt-in, igual ao e-mail do site): às 9h do dia em que ela volta a poder doar;
 * - doação agendada: na véspera às 18h e 2 horas antes. Só entram avisos que ainda estão no futuro.
 */
fun Estado.lembretes(agoraMs: Long = System.currentTimeMillis()): List<Lembrete> {
    if (!notificacoes) return emptyList()
    val d = doadorAtual ?: return emptyList()
    val hoje: LocalDate = Instant.ofEpochMilli(agoraMs).atZone(FUSO).toLocalDate()
    val lista = mutableListOf<Lembrete>()

    if (d.querLembrete) {
        val aptoEm = ultimaDoacaoDe(d.id)?.let { Elegibilidade.aptoEmDia(it, d.sexo, hoje) }
        if (aptoEm != null && aptoEm.isAfter(hoje)) {
            val quando = aptoEm.atTime(9, 0).atZone(FUSO).toInstant().toEpochMilli()
            lista.add(Lembrete(IdsLembrete.RETORNO, quando, "Você já pode doar de novo 🩸", "Já passou o intervalo desde a sua última doação. Que tal agendar a próxima?"))
        }
    }

    agendamentoAtivoDe(d.id)?.let { ag ->
        val v = vagaPorId(ag.vagaId) ?: return@let
        val onde = hospitalNome(v.hospitalId)
        val momento = Agendamento.momentoDaVaga(v.data, v.hora)
        val vespera = v.data.minusDays(1).atTime(18, 0).atZone(FUSO).toInstant().toEpochMilli()
        if (vespera > agoraMs) lista.add(Lembrete(IdsLembrete.VESPERA, vespera, "Sua doação é amanhã 🩸", "Amanhã às ${v.hora} em $onde. Durma bem e se alimente."))
        val duasHoras = momento - DUAS_HORAS_MS
        if (duasHoras > agoraMs) lista.add(Lembrete(IdsLembrete.DUAS_HORAS, duasHoras, "Sua doação é daqui a 2 horas", "Hoje às ${v.hora} em $onde. Leve um documento com foto."))
    }
    return lista
}
