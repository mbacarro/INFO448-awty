package edu.uw.ischool.mbacarro.awty

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false
    private lateinit var messageEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var intervalEditText: EditText
    private lateinit var startStopButton: Button

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        intervalEditText = findViewById(R.id.intervalEditText)
        startStopButton = findViewById(R.id.startStopButton)

        startStopButton.setOnClickListener {
            if (!isServiceRunning) {
                startService()
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
                sendToastMessage(message, phoneNumber)
                try {
                    Thread.sleep(interval * 60 * 1000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error in MessageSenderService: ${e.message}")
                }
            }
        }.start()
    }

    private fun sendToastMessage(message: String, phoneNumber: String) {
        runOnUiThread {
            showToast("$phoneNumber: $message")
        }
    }
}

