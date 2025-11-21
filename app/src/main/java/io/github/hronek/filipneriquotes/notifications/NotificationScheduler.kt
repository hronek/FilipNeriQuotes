package io.github.hronek.filipneriquotes.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import android.util.Log

object NotificationScheduler {
    private const val REQUEST_CODE = 10001

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        val triggerAt = nextTriggerTime(hour, minute)
        Log.d("FNQ", "Scheduling exact alarm at ${'$'}triggerAt for ${'$'}hour:${'$'}minute (ms=${'$'}triggerAt)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d("FNQ", "Cancelling exact alarm")
        am.cancel(pendingIntent(context))
    }

    fun rescheduleFromPrefs(context: Context) {
        val prefs = io.github.hronek.filipneriquotes.data.Prefs
        if (prefs.isNotifEnabled(context)) {
            val (h, m) = prefs.getNotifTime(context)
            Log.d("FNQ", "Rescheduling from prefs enabled=true time=${'$'}h:${'$'}m")
            scheduleDaily(context, h, m)
        } else {
            Log.d("FNQ", "Rescheduling from prefs enabled=false -> cancel")
            cancel(context)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuoteNotificationReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun nextTriggerTime(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
