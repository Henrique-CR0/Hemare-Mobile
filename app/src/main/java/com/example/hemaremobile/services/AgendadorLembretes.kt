package com.example.hemaremobile.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hemaremobile.data.Estado
import com.example.hemaremobile.data.IdsLembrete
import com.example.hemaremobile.data.lembretes
import com.example.hemaremobile.data.local.BancoHemare

/**
 * Agenda no AlarmManager do Android os avisos calculados por `Estado.lembretes()`.
 * Usa alarme não exato (`setAndAllowWhileIdle`): o aviso pode atrasar alguns minutos, mas não pede permissão especial.
 * Sempre apaga os alarmes antigos antes de criar os novos, então chamar de novo é seguro.
 */
class AgendadorLembretes(private val contexto: Context) {

    fun reagendar(estado: Estado): List<Int> {
        val alarmes = contexto.getSystemService(AlarmManager::class.java)
        IdsLembrete.TODOS.forEach { id ->
            PendingIntent.getBroadcast(contexto, id, intentDe(id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                alarmes.cancel(it)
                it.cancel()
            }
        }
        val novos = estado.lembretes()
        novos.forEach { l ->
            val intent = intentDe(l.id).putExtra(EXTRA_TITULO, l.titulo).putExtra(EXTRA_TEXTO, l.texto)
            val pendente = PendingIntent.getBroadcast(contexto, l.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, l.quandoMs, pendente)
        }
        return novos.map { it.id }
    }

    private fun intentDe(id: Int) = Intent(contexto, LembreteReceiver::class.java).setAction("hemare.lembrete.$id").putExtra(EXTRA_ID, id)

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_TEXTO = "texto"
    }
}

/** Recebe o alarme na hora marcada e mostra a notificação (funciona com o app fechado). */
class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        val id = intent.getIntExtra(AgendadorLembretes.EXTRA_ID, 0)
        val titulo = intent.getStringExtra(AgendadorLembretes.EXTRA_TITULO) ?: return
        val texto = intent.getStringExtra(AgendadorLembretes.EXTRA_TEXTO) ?: return
        Notificacoes.mostrar(contexto, id, titulo, texto)
    }
}

/** Depois que o aparelho reinicia os alarmes somem: lê o banco e agenda tudo de novo. */
class ReinicioReceiver : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val estado = BancoHemare(contexto).use { it.carregar() } ?: return
        AgendadorLembretes(contexto).reagendar(estado)
    }
}
