package com.example.hemaremobile.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.hemaremobile.MainActivity

/** Serviço de notificações do Android: canal, permissão e exibição do aviso. */
object Notificacoes {
    const val CANAL = "lembretes"
    const val PERMISSAO = Manifest.permission.POST_NOTIFICATIONS

    /** Android 13+ pede permissão para mostrar notificações; antes disso, basta o canal estar ligado. */
    fun permitido(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(contexto, PERMISSAO) == PackageManager.PERMISSION_GRANTED

    private fun criarCanal(contexto: Context) {
        val gerente = contexto.getSystemService(NotificationManager::class.java)
        if (gerente.getNotificationChannel(CANAL) == null) {
            gerente.createNotificationChannel(
                NotificationChannel(CANAL, "Lembretes de doação", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Aviso de quando você pode doar de novo e da sua doação agendada."
                }
            )
        }
    }

    /** Mostra o aviso. Devolve false se a pessoa ainda não deu permissão. */
    fun mostrar(contexto: Context, id: Int, titulo: String, texto: String): Boolean {
        if (!permitido(contexto)) return false
        criarCanal(contexto)
        val abrir = PendingIntent.getActivity(
            contexto, 0, Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        contexto.getSystemService(NotificationManager::class.java).notify(id, aviso)
        return true
    }
}
