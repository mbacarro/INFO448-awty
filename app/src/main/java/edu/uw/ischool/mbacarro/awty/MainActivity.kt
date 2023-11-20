package edu.uw.ischool.mbacarro.awty

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
                        sendMMS(phoneNumber, message)
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
        val handler = Handler()
        handler.removeCallbacksAndMessages(null)
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
                sendMMS(phoneNumber, message)
                try {
                    Thread.sleep(interval * 60 * 1000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error in MessageSenderService: ${e.message}")
                }
            }
        }.start()
    }

    private fun sendMMS(phoneNumber: String, message: String) {
        try {
            // Get the URI for the video and audio files in res/raw
            val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.samplemp4}")
            val audioUri = Uri.parse("android.resource://${packageName}/${R.raw.samplemp3}")

            // Optional location URL
            val locationUrl: String? = null

            // Carrier-specific configuration overrides (can be null)
            val configOverrides: Bundle? = null

            // PendingIntent for handling the result of sending the message
            val sentIntent: PendingIntent? = null

            // messageId (optional)
            val messageId: Long = 0

            // Use the sendMultimediaMessage function
            smsManager.sendMultimediaMessage(
                this,
                videoUri,
                locationUrl,
                configOverrides,
                sentIntent,
                messageId
            )

            // For audio, you can also call sendMultimediaMessage separately
            smsManager.sendMultimediaMessage(
                this,
                audioUri,
                locationUrl,
                configOverrides,
                sentIntent,
                messageId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending MMS directly: ${e.message}")
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
