package io.github.hronek.filipneriquotes.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtils {
    const val CHANNEL_ID = "daily_quotes"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = mgr.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Quotes",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                ch.description = "Daily notification with quote of the day"
                mgr.createNotificationChannel(ch)
            }
        }
    }
}
