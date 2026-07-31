package com.example.gothere.notify

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GoThereMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d("FCM", "token=$token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "msg=${message.notification?.title}")
    }
}
