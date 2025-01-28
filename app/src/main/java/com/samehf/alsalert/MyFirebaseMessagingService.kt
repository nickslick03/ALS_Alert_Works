package com.samehf.alsalert

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.util.TimeZone
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "MyFirebaseMsgService"
    private lateinit var firebaseAuth: FirebaseAuth
    private var ringtone: Ringtone? = null



    @SuppressLint("MissingPermission")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if the message contains data
//        if (remoteMessage.data.isNotEmpty()) {
//            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
//
//            // Handle the data payload here
//            val title = remoteMessage.data["title"]
//            val body = remoteMessage.data["body"]
//
//            // Show notification
//            showNotification(title, body)
//        }
        if(remoteMessage.notification != null) {
//            val title = remoteMessage.notification!!.title
//            val body = remoteMessage.notification!!.body
//
//            val largeicon = BitmapFactory.decodeResource(resources, R.drawable.warningsymbol)
//
//            val notificationBuilder = NotificationCompat.Builder(this, "myID123")
//                .setContentTitle(title)
//                .setContentText("test")
//                .setPriority(NotificationCompat.PRIORITY_HIGH)  // Set priority to high for heads-up notification
////            .setSound(soundUri)  // Set the sound for the notification
//                .setSmallIcon(R.drawable.warningsymbol)
//                .setLargeIcon(largeicon)
//
//            val manager = NotificationManagerCompat.from(this)
//            manager.notify(1002,notificationBuilder.build())
            // Show notification
//        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//            showNotification(this, title, body, "myID123")
//        playRingtone()
        }
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        val largeicon = BitmapFactory.decodeResource(resources, R.drawable.warningsymbol)
        val soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.alert2)
        val notificationBuilder = NotificationCompat.Builder(this,"myID123")
            .setContentTitle(title)
            .setContentText("test")
            .setPriority(NotificationCompat.PRIORITY_MAX)  // Set priority to high for heads-up notification
            .setSound(soundUri)  // Set the sound for the notification
            .setSmallIcon(R.drawable.warningsymbol)
            .setLargeIcon(largeicon)

        val manager = NotificationManagerCompat.from(this)
        manager.notify(0,notificationBuilder.build())
        // Check if the message contains a notification payload
//        remoteMessage.notification?.let {
//            Log.d(TAG, "Message Notification Body: ${it.body}")
//            val title = it.title
//            val body = it.body
//
//            val largeicon = BitmapFactory.decodeResource(resources, R.drawable.warningsymbol)
//
//            val notificationBuilder = NotificationCompat.Builder(this,"myID123")

//                .setContentTitle(title)
//                .setContentText("test")
//                .setPriority(NotificationCompat.PRIORITY_HIGH)  // Set priority to high for heads-up notification
////            .setSound(soundUri)  // Set the sound for the notification
//                .setSmallIcon(R.drawable.warningsymbol)
//                .setLargeIcon(largeicon)
//
//            val manager = NotificationManagerCompat.from(this)
//            manager.notify(0,notificationBuilder.build())
//        }
        showNotification(this,title,body,"myID123")

    }

    fun createChannel() {
        val sound =
            Uri.parse("android.resource://" + applicationContext.packageName + "/alert2.mp3")
        val mChannel: NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel =
                NotificationChannel("myID123", "VIDEO CALL", NotificationManager.IMPORTANCE_HIGH)
            mChannel.lightColor = Color.GRAY
            mChannel.enableLights(true)
            mChannel.description = "VIDEO CALL"
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            mChannel.setSound(sound, audioAttributes)
            val notificationManager =
                applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    override fun onNewToken(token: String) {
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser
        val uid = user?.uid.toString()

        if (token != null && uid != "null") {
            val firestore = FirebaseFirestore.getInstance()



            // Reference to the "ALSAlert" collection and the document with UID
            val espCollection = firestore.collection("ALSAlert")
            val userDocument = espCollection.document(uid)

            // Check if the document exists
            userDocument.get().addOnCompleteListener { documentSnapshotTask ->
                if (documentSnapshotTask.isSuccessful) {
                    val documentSnapshot = documentSnapshotTask.result

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Document exists, update the existing document with the FCM token
                        userDocument.update("token", token,
                            "timezone", TimeZone.getDefault().getID(),
                            "notification", "0")
                            .addOnSuccessListener {
                                // Successfully updated the document
                            }
                            .addOnFailureListener { e ->
                                // Handle the error
                                // e.printStackTrace()
                            }
                    } else {
                        // Document doesn't exist, create a new document with the FCM token
                        val data = hashMapOf(
                            "token" to token,
                            "timezone" to TimeZone.getDefault().getID()

                        )

                        userDocument.set(data)
                            .addOnSuccessListener {
                                // Successfully created the document
                            }
                            .addOnFailureListener { e ->
                                // Handle the error
                                // e.printStackTrace()
                            }
                    }
                } else {
                    // Handle the error
                    // documentSnapshotTask.exception?.printStackTrace()
                }
            }
        }
    }

    object ConstantFunction {

        fun createNotificationChannel(
            context: Context,
            channelId: String,
            channelName: String,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()


                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )

//                channel.setSound(soundUri, audioAttributes)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, title: String?, body: String?, channelId: String) {
        val notificationManager = NotificationManagerCompat.from(context)

        val largeicon = BitmapFactory.decodeResource(resources, R.drawable.warningsymbol)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)  // Set priority to high for heads-up notification
//            .setSound(soundUri)  // Set the sound for the notification
            .setSmallIcon(R.drawable.warningsymbol)
            .setLargeIcon(largeicon)

//        playRingtone()

        // Other notification settings...

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun playRingtone() {
        // Get the default ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        // Create a Ringtone object
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)

        // Start playing the ringtone
        ringtone?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the ringtone when the activity is destroyed
        ringtone?.stop()
    }
}
