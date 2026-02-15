package com.example.gothere.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.*

object ReminderScheduler {
    fun schedule(context: Context, whenMillis: Long, dateKey: String, note: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("dateKey", dateKey)
            putExtra("note", note)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            dateKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // If time is in the past, fire in ~3 seconds (useful during testing)
        val triggerAt = if (whenMillis <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 3_000
        } else whenMillis

        // Best effort exact alarm; on Android 12+ user may need to allow exact alarms.
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancel(context: Context, dateKey: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            dateKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }
}
