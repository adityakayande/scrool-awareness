package com.example.scrolltracker.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefManager = com.example.scrolltracker.data.PreferenceManager(application)

    private val _scrollCount = MutableLiveData<Int>()
    val scrollCount: LiveData<Int> = _scrollCount

    private val _watchTimeMs = MutableLiveData<Long>()
    val watchTimeMs: LiveData<Long> = _watchTimeMs

    private val _history = MutableLiveData<List<com.example.scrolltracker.data.DailyRecord>>()
    val history: LiveData<List<com.example.scrolltracker.data.DailyRecord>> = _history

    private val _sessionInfo = MutableLiveData<String>()
    val sessionInfo: LiveData<String> = _sessionInfo

    private val _isServiceEnabled = MutableLiveData<Boolean>()
    val isServiceEnabled: LiveData<Boolean> = _isServiceEnabled

    fun refreshData() {
        _scrollCount.value = prefManager.getDailyScrollCount()
        _watchTimeMs.value = prefManager.getDailyWatchTimeMs()
        _history.value = prefManager.getHistory()

        val lastScroll = prefManager.lastScrollTime
        val currentTime = System.currentTimeMillis()
        if (lastScroll > 0 && currentTime - lastScroll < 5 * 60 * 1000) {
            val sessionStart = prefManager.sessionStartTime
            val dateFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val timeString = dateFormat.format(java.util.Date(sessionStart))
            _sessionInfo.value = "Current Session: Started at $timeString (${prefManager.sessionScrollCount} scrolls)"
        } else {
            _sessionInfo.value = "No active session"
        }

        _isServiceEnabled.value = isAccessibilityServiceEnabled(getApplication(), com.example.scrolltracker.service.ScrollAccessibilityService::class.java)
    }

    private fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
        val expectedComponentName = context.packageName + "/" + accessibilityService.name
        var enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServicesSetting == null) enabledServicesSetting = ""
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
