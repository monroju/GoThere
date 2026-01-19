package com.example.gothere.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gothere.repository.EventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule notifications after phone reboot
            val repo = EventsRepository()
            CoroutineScope(Dispatchers.IO).launch {
                val events = repo.listUpcoming()
                events.forEach { event ->
                    NotificationHelper.scheduleNotification(context, event)
                }
            }
        }
    }
}
