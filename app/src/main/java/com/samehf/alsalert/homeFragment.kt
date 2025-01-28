package com.samehf.alsalert
import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask

class homeFragment:Fragment(R.layout.fragment_home) {

    private var latestTimestamp: Timestamp? = null
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    //private var timer: Timer? = null
    private lateinit var agoText : TextView
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //recyclerView = view.findViewById(R.id.recyclerViewTimestamps)
        //recyclerView.layoutManager = LinearLayoutManager(context)


        val preferences = requireActivity().getSharedPreferences("LoginPrefs", AppCompatActivity.MODE_PRIVATE)
        var uid = preferences.getString("uid", null)

        val alarmStopButton = view.findViewById<Button>(R.id.stopAlarmButton2)
        agoText = view.findViewById<TextView>(R.id.agoText)

        alarmStopButton.setOnClickListener {
            if (uid != null) {
                db.collection("ALSAlert").document(uid)
                    .update("stopAlarm", "1")
            }
        }

        if(uid!=null) {
            val documentRef = db.collection("ALSAlert").document(uid)
            listenerRegistration = documentRef.addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        // Handle errors
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Retrieve the "LastNotification" field as an array of Timestamps
                        val timestampsList = snapshot.get("LastNotification") as? List<com.google.firebase.Timestamp>

                        // Initialize the RecyclerView adapter with the retrieved timestamps
                        if (timestampsList != null) {
                            // Reference to the LinearLayout container
                            val container: LinearLayout = view.findViewById(R.id.container)

                            container.removeAllViews()
                            // Format for displaying timestamps
                            val dateFormat = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault())

                            // Iterate through the list of timestamps and create TextViews
                            var isFirstItem = true
                            for (timestamp in timestampsList) {
                                val timestampText = dateFormat.format(timestamp.toDate())

                                val textView = TextView(context)
                                textView.text = timestampText
                                textView.textSize = 20f  // Set text size to 20sp
                                textView.setTypeface(null, Typeface.BOLD)  // Set text to bold
                                textView.gravity = Gravity.LEFT  // Center the text horizontally
                                textView.setLineSpacing(8f, 1.0f)  // Adjust the first parameter for line spacing in pixels

                                // Set padding (optional)
                                textView.setPadding(0, 8, 0, 8)

                                // Add the TextView to the LinearLayout container
                                container.addView(textView)

                                if(isFirstItem) {
                                    latestTimestamp = timestamp
                                    val currentTime = Timestamp.now().toDate().time
                                    val timestampTime = timestamp.toDate().time
                                    val timeDifference = currentTime - timestampTime

                                    val days = timeDifference / (1000 * 60 * 60 * 24)
                                    val hours = timeDifference / (1000 * 60 * 60) % 24
                                    val minutes = timeDifference / (1000 * 60) % 60
                                    val seconds = timeDifference / 1000 % 60

//                                    val sdf = SimpleDateFormat("dd HH:mm:ss a", Locale.getDefault())
//                                    sdf.timeZone = TimeZone.getTimeZone("GMT") // Set timezone to GMT to avoid offset
//                                    val timeAgoString = "${days}d ${String.format("%02d", hours)}:${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"

                                    if(days.toInt() ==0){
                                        if(hours.toInt() ==0){
                                            if(minutes.toInt()==0){
                                                if(seconds.toInt() == 1){
                                                    agoText.text = "${seconds} second ago"
                                                } else {
                                                    agoText.text = "${seconds} seconds ago"
                                                }

                                            } else if(minutes.toInt()==1) {
                                                agoText.text = "${minutes} minute ago"
                                            } else {
                                                agoText.text = "${minutes} minutes ago"
                                            }
                                        } else if(hours.toInt() ==1) {
                                            agoText.text = "${hours} hour ago"
                                        } else {
                                            agoText.text = "${hours} hours ago"
                                        }
                                    } else if (days.toInt() ==1){
                                        agoText.text = "${days} day ago"
                                    } else {
                                        agoText.text = "${days} days ago"
                                    }

                                    //agoText.text = timeAgoString
                                    Log.d("days", days.toString())
                                    Log.d("hours", hours.toString())
                                    Log.d("minutes", minutes.toString())

                                    val lastAlarmText =
                                        view.findViewById<TextView>(R.id.lastAlarmText)
                                    lastAlarmText.text = timestampText
                                    isFirstItem = false
                                    startPeriodicUpdates()
                                }

                            }
                        }
                    }
                }
        }
    }

    private fun startPeriodicUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateDisplay()
                // Schedule the next update after 1 second
                handler.postDelayed(this, 1000)
            }
        })
    }

    fun updateDisplay() {
        val currentTime = Timestamp.now().toDate().time
        val timestampTime = latestTimestamp?.toDate()?.time
        val timeDifference = currentTime - timestampTime!!

        val days = timeDifference / (1000 * 60 * 60 * 24)
        val hours = timeDifference / (1000 * 60 * 60) % 24
        val minutes = timeDifference / (1000 * 60) % 60
        val seconds = timeDifference / 1000 % 60

        if(days.toInt() ==0){
            if(hours.toInt() ==0){
                if(minutes.toInt()==0){
                    if(seconds.toInt() == 1){
                        agoText.text = "${seconds} second ago"
                    } else {
                        agoText.text = "${seconds} seconds ago"
                    }

                } else if(minutes.toInt()==1) {
                    agoText.text = "${minutes} minute ago"
                } else {
                    agoText.text = "${minutes} minutes ago"
                }
            } else if(hours.toInt() ==1) {
                agoText.text = "${hours} hour ago"
            } else {
                agoText.text = "${hours} hours ago"
            }
        } else if (days.toInt() ==1){
            agoText.text = "${days} day ago"
        } else {
            agoText.text = "${days} days ago"
        }

    }

    override fun onPause() {
        super.onPause()
        // Remove the snapshot listener when the fragment is paused
        listenerRegistration?.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the updateRunnable callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
}
