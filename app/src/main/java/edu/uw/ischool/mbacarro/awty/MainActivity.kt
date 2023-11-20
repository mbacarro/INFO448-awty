package edu.uw.ischool.mbacarro.awty

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false
    private lateinit var messageEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var intervalEditText: EditText
    private lateinit var startStopButton: Button
    private lateinit var smsManager: SmsManager

    private val TAG = "MainActivity"
    private val SMS_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smsManager = SmsManager.getDefault()

        messageEditText = findViewById(R.id.messageEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        intervalEditText = findViewById(R.id.intervalEditText)
        startStopButton = findViewById(R.id.startStopButton)

        startStopButton.setOnClickListener {
            if (!isServiceRunning) {
                checkAndRequestSMSPermission()
            } else {
                stopService()
            }
        }
    }

    private fun startService() {
        val message = messageEditText.text.toString()
        val phoneNumber = phoneNumberEditText.text.toString()
        val intervalStr = intervalEditText.text.toString()

        if (message.isNotEmpty() && intervalStr.isNotEmpty()) {
            val interval = intervalStr.toLong()

            if (interval > 0) {
                val handler = Handler()
                val runnable = object : Runnable {
                    override fun run() {
                        sendSMS(phoneNumber, message)
                        showToast("$phoneNumber: $message")
                        handler.postDelayed(this, interval * 60 * 1000)
                    }
                }

                handler.postDelayed(runnable, 0)

                startStopButton.text = "Stop"
                isServiceRunning = true

                // Start the background service
                startService(message, phoneNumber, interval)
                Log.i(TAG, "starting service")
            } else {
                showToast("Interval must be greater than 0")
            }
        } else {
            showToast("Please fill out all fields")
        }
    }

    private fun stopService() {
        startStopButton.text = "Start"
        isServiceRunning = false
        Log.i(TAG, "stopping service")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startService(message: String, phoneNumber: String, interval: Long) {
        Thread {
            while (isServiceRunning) {
                sendSMS(phoneNumber, message)
                try {
                    Thread.sleep(interval * 60 * 1000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error in MessageSenderService: ${e.message}")
                }
            }
        }.start()
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}")
        }
    }

    private fun checkAndRequestSMSPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        } else {
            startService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startService()
            } else {
                showToast("SMS permission denied. Cannot send messages.")
            }
        }
    }
}
