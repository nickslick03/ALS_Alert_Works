package com.samehf.alsalert

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class SetupWifi : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wifi)

        val preferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val wifiStatus = preferences.getString("wifistatus", null)
        var wifiErrorText = findViewById<TextView>(R.id.wifiErrorText)

        if(wifiStatus=="failed") {
            wifiErrorText.visibility = View.VISIBLE
        } else {
            wifiErrorText.visibility = View.INVISIBLE
        }

        var continueButton = findViewById<Button>(R.id.wificontinuebutton)

        continueButton.setOnClickListener {
            val intent = Intent(this, SetupWiFi2::class.java)
            startActivity(intent)
        }
    }



//    fun connectToWifi(context: Context, ssid: String, password: String) {
//        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // For Android 10 and above, use WifiNetworkSuggestion
//            val suggestion = WifiNetworkSuggestion.Builder()
//                .setSsid("ALS Alert")
//                .setWpa2Passphrase("")
//                .build()
//
//            // Register the suggestion
//            val suggestionsList = mutableListOf(suggestion)
//            val status = wifiManager.addNetworkSuggestions(suggestionsList)
//
//            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
//                // Use ConnectivityManager to request a connection
//                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//                val networkRequest = NetworkRequest.Builder()
//                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                    .build()
//                Toast.makeText(this,"new method here",Toast.LENGTH_LONG)
//
//                connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
//                    override fun onAvailable(network: Network) {
//                        // Network is available, now reconnect
//                        connectivityManager.bindProcessToNetwork(network)
//                        connectivityManager.unregisterNetworkCallback(this)
//                    }
//                })
//                return
//            }
//        } else {
//            // For versions below Android 10, use WifiConfiguration
//            // Disconnect from the current network
//            wifiManager.disconnect()
//            Toast.makeText(this,"old method here",Toast.LENGTH_LONG)
//
//            val conf = WifiConfiguration()
//            conf.SSID = "\"" + "ALS Alert" + "\""
//            conf.preSharedKey = "\"" + "" + "\""
//
//            val networkId = wifiManager.addNetwork(conf)
//            wifiManager.enableNetwork(networkId, true)
//            wifiManager.reconnect()
//            return
//        }
//
//        // Handle failure if reaching this point
//        // This could include cases where Wi-Fi suggestions were not accepted or the Android version is not supported.
//        // You may want to implement additional handling here.
//    }

}