package com.elutto.app.serv

import com.elutto.app.util.LogUtil.logI
import com.elutto.app.util.LogUtil.makeLogTag
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EluttoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        logI(TAG, "From: " + remoteMessage!!.from)
        logI(TAG, "Notification body: " + remoteMessage.notification?.body)
    }

    companion object {
        private val TAG = makeLogTag(EluttoFirebaseMessagingService::class.java)
    }
}
