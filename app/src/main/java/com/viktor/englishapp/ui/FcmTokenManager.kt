package com.viktor.englishapp.ui

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.viktor.englishapp.data.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    private const val TAG = "FCM_DEBUG"

    fun registerAfterLogin(context: Context, jwtToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Attempting to get FCM token...")
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM token obtained: $fcmToken")

                context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE).edit {
                    putString("fcm_token", fcmToken)
                }

                Log.d(TAG, "Sending FCM token to backend...")
                RetrofitClient.instance.updateFcmToken(
                    token = "Bearer $jwtToken",
                    body = mapOf("fcm_token" to fcmToken)
                )
                Log.d(TAG, "FCM token sent to backend successfully!")

            } catch (e: Exception) {
                Log.e(TAG, "FCM registration FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            }
        }
    }
}
