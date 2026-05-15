package com.example.scrolltracker.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.scrolltracker.R
import com.example.scrolltracker.data.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefManager = PreferenceManager(this)

        val btnStartTime: Button = findViewById(R.id.btnStartTime)
        val btnEndTime: Button = findViewById(R.id.btnEndTime)

        updateButtonText(btnStartTime, "Start Time", prefManager.trackingStartHour, prefManager.trackingStartMinute)
        updateButtonText(btnEndTime, "End Time", prefManager.trackingEndHour, prefManager.trackingEndMinute)

        btnStartTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    prefManager.trackingStartHour = hourOfDay
                    prefManager.trackingStartMinute = minute
                    updateButtonText(btnStartTime, "Start Time", hourOfDay, minute)
                },
                prefManager.trackingStartHour,
                prefManager.trackingStartMinute,
                true
            ).show()
        }

        btnEndTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    prefManager.trackingEndHour = hourOfDay
                    prefManager.trackingEndMinute = minute
                    updateButtonText(btnEndTime, "End Time", hourOfDay, minute)
                },
                prefManager.trackingEndHour,
                prefManager.trackingEndMinute,
                true
            ).show()
        }
    }

    private fun updateButtonText(button: Button, prefix: String, hour: Int, minute: Int) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        button.text = "Set $prefix ($formattedTime)"
    }
}
