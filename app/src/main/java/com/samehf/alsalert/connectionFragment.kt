package com.samehf.alsalert

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale


class connectionFragment : Fragment(R.layout.fragment_connection) {

    private lateinit var listener : ListenerRegistration
    private lateinit var listener2 : ListenerRegistration
    private val db = FirebaseFirestore.getInstance()
    private var clickHandled = false
    private lateinit var uid : String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the button by its ID
        val wifiTestButton = view.findViewById<Button>(R.id.wifiTestButton)
        val wifiTestText = view.findViewById<TextView>(R.id.wifiTestText)
        val alarmTestButton = view.findViewById<Button>(R.id.alarmTestButton)
        val alarmStopButton = view.findViewById<Button>(R.id.alarmStopButton)


        val preferences = requireActivity().getSharedPreferences("LoginPrefs", AppCompatActivity.MODE_PRIVATE)
        uid = preferences.getString("uid", null).toString()

        alarmTestButton.setOnClickListener {
            if (uid != null) {
                db.collection("ALSAlert").document(uid)
                    .update("startAlarm", "1")
                checknow()
            }
        }

        alarmStopButton.setOnClickListener {
            if (uid != null) {
                db.collection("ALSAlert").document(uid)
                    .update("stopAlarm", "1")
            }
        }



        if (uid != null) {
            listener2 = db.collection("ALSAlert").document(uid)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        // Handle errors
                        return@addSnapshotListener
                    }


                    if (snapshot != null && snapshot.exists()) {
                        val lastConnectTime = snapshot.getTimestamp("lastConnection")
                        if (lastConnectTime != null) {
                            // Use the updated value of lastConnection here
                            // For example, set it to a TextView
                            val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault())
                            val lastConnectTimeFormatted = dateFormat.format(lastConnectTime.toDate())

                            wifiTestText.text = lastConnectTimeFormatted
                        } else {
                            // Handle the case when "lastConnection" field is null
                        }
                    } else {
                        // Handle the case when document doesn't exist
                    }
                }
        }
        // Set a click listener for the button
        wifiTestButton.setOnClickListener {
            if(!clickHandled){
                clickHandled = true
                GlobalScope.launch(Dispatchers.Main) {
                    delay(10000) // Wait for 10 seconds
                    if (clickHandled) {
                        wifiTestText.text = "Failed!"
                    }
                }
                wifiTestButton.setBackgroundColor(Color.RED)
                Log.d("connected", "here now1")
                // Handle button click here
                // For example, you can navigate to another fragment or perform an action
                if (uid != null) {
                    db.collection("ALSAlert").document(uid)
                        .update("connected", "3")
                        .addOnSuccessListener {
                            // Handle success
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                        }
                    checknow()

                    // Handle the connection here
                    listener = db.collection("ALSAlert").document(uid)
                    .addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            // Handle errors
                            return@addSnapshotListener
                        }

                        val connected = snapshot?.getString("connected")
                        if (connected == "4") {
                            // "connected" is set to 2, handle the condition here
                            // For example, perform an action or navigate to another fragment
                            wifiTestButton.setBackgroundColor(Color.GREEN)
                            clickHandled = false
                            listener.remove()
                        }
                    }

                }
            }
        }
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

    fun checknow() {
        var ipaddress : String
        db.collection("ALSAlert").document(uid)
            .get(Source.DEFAULT).addOnCompleteListener {

                ipaddress = it.result.data?.get("ipaddress").toString()
//                        Log.d("ipaddres", ipaddress)
                val url = "http://${ipaddress}:80/checknow"
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
            }
    }
}