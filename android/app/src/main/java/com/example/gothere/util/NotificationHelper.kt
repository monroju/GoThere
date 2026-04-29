package com.example.gothere.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.gothere.model.EventItem

object NotificationHelper {
    private const val CHANNEL_ID = "calendar_reminders"
    private const val CHANNEL_NAME = "Calendar Reminders"
    private const val TASK_CHANNEL_ID = "task_reminders"
    private const val TASK_CHANNEL_NAME = "Task Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val calendarChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications for calendar events and notes"
            }
            notificationManager.createNotificationChannel(calendarChannel)

            val taskChannel = NotificationChannel(TASK_CHANNEL_ID, TASK_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for upcoming relocation tasks"
            }
            notificationManager.createNotificationChannel(taskChannel)
        }
    }

    fun scheduleTaskReminder(context: Context, taskId: String, taskTitle: String, dueAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("event_title", taskTitle)
            putExtra("event_id", "task_$taskId")
            putExtra("channel_id", TASK_CHANNEL_ID)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "task_$taskId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Remind at 9 AM the day before the due date
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = dueAtMillis
            add(java.util.Calendar.DAY_OF_YEAR, -1)
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }

        val triggerTime = calendar.timeInMillis
        if (triggerTime > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.d("NotificationHelper", "Scheduled task reminder for '$taskTitle' at $triggerTime")
            } catch (e: SecurityException) {
                Log.e("NotificationHelper", "Could not schedule task alarm", e)
            }
        }
    }

    fun scheduleNotification(context: Context, event: EventItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("event_title", event.title)
            putExtra("event_id", event.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 9:00 AM on the day of the event
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = event.dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }

        val triggerTime = calendar.timeInMillis
        if (triggerTime > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.d("NotificationHelper", "Scheduled notification for ${event.title} at $triggerTime")
            } catch (e: SecurityException) {
                Log.e("NotificationHelper", "Could not schedule exact alarm", e)
            }
        }
    }

    fun cancelNotification(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
