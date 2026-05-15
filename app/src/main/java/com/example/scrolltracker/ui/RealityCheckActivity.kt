package com.example.scrolltracker.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.scrolltracker.R
import com.example.scrolltracker.databinding.ActivityRealityCheckBinding

class RealityCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRealityCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRealityCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("scroll_tracker_prefs", MODE_PRIVATE)
        val dailyCount = prefs.getInt("daily_scroll_count", 0)

        binding.tvRealityCount.text = dailyCount.toString()

        val colorRes = when {
            dailyCount < 20 -> R.color.color_safe
            dailyCount < 50 -> R.color.color_warning
            dailyCount < 100 -> R.color.color_rising_danger
            else -> R.color.color_danger
        }

        binding.tvRealityCount.setTextColor(ContextCompat.getColor(this, colorRes))

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000) // 2 seconds delay
    }
}
