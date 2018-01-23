package com.elutto.app.serv

import com.elutto.app.util.LogUtil.logI
import com.elutto.app.util.LogUtil.makeLogTag
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class EluttoFirebaseInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        val refreshedToken = FirebaseInstanceId.getInstance().token

        // we want to send messages to this application instance and manage this apps subscriptions on the server side
        // so now send the Instance ID token to the app server
        refreshedToken?.let {
            sendRegistrationToServer(it)
        }
    }

    private fun sendRegistrationToServer(refreshedToken: String) {
        logI(TAG, "Refreshed token: $refreshedToken")
    }

    companion object {
        private val TAG = makeLogTag(EluttoFirebaseInstanceIdService::class.java)
    }
}
