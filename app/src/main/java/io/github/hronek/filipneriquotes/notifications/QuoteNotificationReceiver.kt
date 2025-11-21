package io.github.hronek.filipneriquotes.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.hronek.filipneriquotes.R
import io.github.hronek.filipneriquotes.data.Prefs
import io.github.hronek.filipneriquotes.data.QuoteRepository
import java.time.LocalDate
import android.util.Log

class QuoteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("FNQ", "QuoteNotificationReceiver.onReceive action=" + (intent?.action ?: "<null>") )
        if (!Prefs.isNotifEnabled(context)) {
            Log.d("FNQ", "Notifications disabled in Prefs -> returning")
            return
        }
        NotificationUtils.ensureChannel(context)
        val repo = QuoteRepository(context)
        val lang = Prefs.getLanguage(context).let { if (it == "auto") null else it }
        val text = repo.getQuoteFor(LocalDate.now(), lang)
        Log.d("FNQ", "Building notification with text length=" + text.length)

        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            Log.d("FNQ", "Notifying id=20001")
            notify(NOTIF_ID, notification)
        }

        // schedule next day
        val (h, m) = Prefs.getNotifTime(context)
        Log.d("FNQ", "Rescheduling next day at ${'$'}h:${'$'}m from receiver")
        NotificationScheduler.scheduleDaily(context, h, m)
    }

    companion object {
        private const val NOTIF_ID = 20001
    }
}
