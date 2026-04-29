package com.example.gothere.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import com.example.gothere.MainActivity
import com.example.gothere.R
import com.example.gothere.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object WeeklyDigestScheduler {
    private const val TAG = "WeeklyDigest"
    private const val REQUEST_CODE = 9999

    /** Schedule a weekly notification for Monday at 9am. */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WeeklyDigestReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
        Log.d(TAG, "Weekly digest scheduled for Monday 9am")
    }
}

class WeeklyDigestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = TaskRepository()
                // Count tasks due this week and overdue
                val now = System.currentTimeMillis()
                val weekEnd = now + 7L * 24 * 60 * 60 * 1000

                // We can't easily query Firestore here without auth,
                // so build a simple motivational message
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to", "tasks")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, "task_reminders")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle("GoThere Weekly Check-in")
                    .setContentText("How's your relocation progress? Tap to review your checklist.")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Start your week right! Check your relocation checklist and knock out a few tasks. Every step counts toward your move."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(9999, notification)
            } catch (e: Exception) {
                Log.e("WeeklyDigest", "Failed to send digest", e)
            }
        }
    }
}
