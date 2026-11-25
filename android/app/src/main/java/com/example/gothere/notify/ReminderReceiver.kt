package com.example.gothere.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gothere.MainActivity
import com.example.gothere.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dateKey = intent.getStringExtra("dateKey") ?: "Calendar"
        val note = intent.getStringExtra("note") ?: "Reminder"

        val nm = NotificationManagerCompat.from(context)
        ensureChannel(context)

        // Tapping opens the app (it did nothing before) and logs notification_opened.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationOpen.EXTRA_TYPE, NotificationOpen.TYPE_CALENDAR_NOTE)
            putExtra(NotificationOpen.EXTRA_ID, dateKey)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            dateKey.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name) // fallback; see Note below
            .setContentTitle("GoThere — $dateKey")
            .setContentText(note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(dateKey.hashCode(), notif)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID, "Calendar Notes", NotificationManager.IMPORTANCE_DEFAULT
            )
            mgr.createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL_ID = "gothere_calendar_notes"
    }
}
