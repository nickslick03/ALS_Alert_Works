package com.samehf.alsalert

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.ContactsContract.CommonDataKinds.Website.URL
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SetupWiFi2 : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var countDownTimer: CountDownTimer
    private val db = FirebaseFirestore.getInstance()
    private lateinit var listener : ListenerRegistration

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wi_fi2)

        progressBar = findViewById(R.id.progressBar)
        var submit = findViewById<Button>(R.id.submitButton)
        var wifiname = findViewById<EditText>(R.id.wifiName)
        var wifipass = findViewById<EditText>(R.id.wifiPass)
        var success = false

        val preferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val savedEmail = preferences.getString("email", null)
        val savedPassword = preferences.getString("password", null)
        val uid = preferences.getString("uid", null)
        val step = preferences.getString("step", null)

        if(step=="complete") {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }


        submit.setOnClickListener {


            // Usage
//            val response: String = sendHttpRequest("https://192.168.4.1/?ssid=$wifiname&password=$wifipass")
//            Log.d("Response", response)
            // Usage
            submit.visibility = View.GONE
            wifiname.visibility = View.GONE
            wifipass.visibility = View.GONE
            progressBar.visibility = View.VISIBLE



            val url = "http://192.168.4.1/?ssid=${wifiname.text}&password=${wifipass.text}&email=$savedEmail&epassword=$savedPassword"
            sendHttpRequest(url,
                onSuccess = { response ->
                    // Handle successful response
                    Log.d("Response", response)
                },
                onError = { error ->
                    // Handle error
                    error.printStackTrace()
                }
            )
            handleProgressBar()
            GlobalScope.launch(Dispatchers.Main) {
                delay(20000) // Wait for 20 seconds
                // After 20 seconds, update the progress bar to 100%
                if(!success) {
                    submit.visibility = View.VISIBLE
                    wifiname.visibility = View.VISIBLE
                    wifipass.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE
                    val preferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                    val editor = preferences.edit()
                    editor.putString("wifistatus", "failed")
                    editor.apply()

                    //This is needed to avoid issues with closing and opening to wrong layout
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    goback()
                }
            }


            if (uid != null) {
                listener = db.collection("ALSAlert").document(uid)
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            // Handle errors
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val connected = snapshot.getString("connected")
                            if (connected == "1") {

                                // If "connected" is set to "1", stop the progress and handle the connection
                                progressBar.progress = progressBar.max

                                db.collection("ALSAlert").document(uid)
                                    .update("connected", "2")
                                    .addOnSuccessListener {
                                        // Handle success
                                        success = true
                                    }
                                    .addOnFailureListener { e ->
                                        // Handle failure
                                    }
                                // Handle the connection here
                                val preferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                                val editor = preferences.edit()
                                editor.putString("step", "complete")
                                editor.putString("wifistatus", null)
                                editor.apply()

                                success()

                            }
                        }
                    }

            }

        }



    }

    fun success() {
        //This is needed to avoid issues with closing and opening to wrong layout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        listener.remove()
        Log.d("removed!", "hereee")
    }

    fun goback() {
        val intent2 = Intent(this, SetupWifi::class.java)
        startActivity(intent2)
    }

    fun handleProgressBar() {
        progressBar = findViewById(R.id.progressBar)
        progressBar.max = 500 // Set the maximum value to 20 (20 seconds)

        countDownTimer = object : CountDownTimer(20000, 10) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the progress bar every second
                //Log.d("test1", millisUntilFinished.toString())
                val progress = 500 -(millisUntilFinished / 40).toInt()
                progressBar.progress = progress
            }

            override fun onFinish() {
                // Progress bar reached 100% (20 seconds)
                progressBar.progress = progressBar.max
            }
        }

        // Start the countdown timer
        countDownTimer.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Cancel the countdown timer to prevent memory leaks
        countDownTimer.cancel()
    }

    fun sendHttpRequest(url: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
               val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.use(BufferedReader::readText)

                onSuccess(response)

                reader.close()
                inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
