package com.example.scrolltracker.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scrolltracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "daily_scroll_count" || key == "session_scroll_count" ||
            key == "scroll_history" || key == "daily_watch_time_ms") {
            viewModel.refreshData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        sharedPreferences = getSharedPreferences("scroll_tracker_prefs", MODE_PRIVATE)

        binding.rvHistory.layoutManager = LinearLayoutManager(this)

        viewModel.scrollCount.observe(this) { count ->
            binding.tvScrollCount.text = count.toString()
            updatePsychologicalImpact(count)
        }
        
        viewModel.sessionInfo.observe(this) { info ->
            binding.tvSessionInfo.text = info
        }
        
        viewModel.history.observe(this) { historyList ->
            binding.rvHistory.adapter = HistoryAdapter(historyList)
        }

        viewModel.watchTimeMs.observe(this) { ms ->
            binding.tvWatchTime.text = com.example.scrolltracker.utils.NotificationHelper.formatWatchTime(ms)
        }

        viewModel.isServiceEnabled.observe(this) { isEnabled ->
            if (isEnabled) {
                binding.tvServiceStatus.text = "ON"
                binding.tvServiceStatus.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                binding.btnEnableService.isEnabled = false
                binding.btnEnableService.text = "Service is Active"
            } else {
                binding.tvServiceStatus.text = "OFF"
                binding.tvServiceStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
                binding.btnEnableService.isEnabled = true
                binding.btnEnableService.text = "Enable Accessibility Service"
            }
        }

        binding.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        checkBatteryOptimizations()
    }

    private fun checkBatteryOptimizations() {
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Important Setup")
                .setMessage("To prevent the system from terminating the background service, please allow the app to ignore battery optimizations.\n\nNote: On some devices (Xiaomi, Vivo, Oppo) you may also need to manually enable 'Autostart' or 'Background Activity' in App Settings.")
                .setPositiveButton("Allow") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Settings")?.setIcon(android.R.drawable.ic_menu_preferences)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 2, 0, "My Goals")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            2 -> {
                startActivity(Intent(this, GoalsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private var isPulsing = false
    private var pulseAnimator: android.animation.ObjectAnimator? = null

    private fun updatePsychologicalImpact(count: Int) {
        val levelText: String
        val colorRes: Int

        when {
            count < 20 -> {
                levelText = "LEVEL: SAFE"
                colorRes = com.example.scrolltracker.R.color.color_safe
                stopPulseAnimation()
                binding.tvScrollCount.setShadowLayer(0f, 0f, 0f, 0)
            }
            count < 50 -> {
                levelText = "LEVEL: WARNING"
                colorRes = com.example.scrolltracker.R.color.color_warning
                stopPulseAnimation()
                binding.tvScrollCount.setShadowLayer(0f, 0f, 0f, 0)
            }
            count < 100 -> {
                levelText = "LEVEL: DANGER"
                colorRes = com.example.scrolltracker.R.color.color_rising_danger
                stopPulseAnimation()
                binding.tvScrollCount.setShadowLayer(0f, 0f, 0f, 0)
            }
            else -> {
                levelText = "LEVEL: SEVERE DANGER"
                colorRes = com.example.scrolltracker.R.color.color_danger
                startPulseAnimation()
                val resolvedColor = androidx.core.content.ContextCompat.getColor(this, colorRes)
                binding.tvScrollCount.setShadowLayer(30f, 0f, 0f, resolvedColor)
            }
        }

        val resolvedColor = androidx.core.content.ContextCompat.getColor(this, colorRes)
        binding.tvScrollCount.setTextColor(resolvedColor)
        binding.tvAddictionLevel.text = levelText
        binding.tvAddictionLevel.setTextColor(resolvedColor)
    }

    private fun startPulseAnimation() {
        if (isPulsing) return
        isPulsing = true
        pulseAnimator = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            binding.tvScrollCount,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.08f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.08f)
        ).apply {
            duration = 1200
            repeatCount = android.animation.ObjectAnimator.INFINITE
            repeatMode = android.animation.ObjectAnimator.REVERSE
            start()
        }
    }

    private fun stopPulseAnimation() {
        if (!isPulsing) return
        isPulsing = false
        pulseAnimator?.cancel()
        binding.tvScrollCount.scaleX = 1.0f
        binding.tvScrollCount.scaleY = 1.0f
    }
}
