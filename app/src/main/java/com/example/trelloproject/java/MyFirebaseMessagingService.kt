package com.example.trelloproject.java

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.trelloproject.R
import com.example.trelloproject.activities.MainActivity
import com.example.trelloproject.activities.SignInActivity
import com.example.trelloproject.firebase.FirestoreClass
import com.example.trelloproject.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remotemessage: RemoteMessage) {// from the firebase server
        super.onMessageReceived(remotemessage)
        Log.d(TAG ,"FROM: ${remotemessage.from}")
        remotemessage.data.isNotEmpty().let {
            Log.d(TAG,"Message data Payload: ${remotemessage.from}")
            val title = remotemessage.data[Constants.FCM_KEY_TITLE]!!
            val message = remotemessage.data[Constants.FCM_KEY_MESSAGE]!!
            sendNotification(title, message)// to the user
        }
        remotemessage.notification?.let {
            Log.d(TAG,"Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e(TAG,"Refreshing token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?){
        val sharedPreferences =
            this.getSharedPreferences(Constants.TRELLO_PROJECT_PREFERENCES, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(Constants.FCM_TOKEN, token)
        editor.apply()
    }
    private fun sendNotification(title : String,message : String){
        val intent = if (FirestoreClass().getCurrentUserId().isNotEmpty()){
            Intent(this,MainActivity::class.java)
        }else{
            Intent(this,SignInActivity::class.java)
        }
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val channelId = this.resources.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(
            this,channelId
        ).setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelId,"Channel TrelloProject Title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0,notificationBuilder.build())
    }
    companion object{
        private const val TAG = "MyFirebaseMsgService"
    }
}