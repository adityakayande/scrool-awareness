package com.example.scrolltracker.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyRecord(val date: String, val count: Int, val watchTimeMs: Long = 0L)

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "scroll_tracker_prefs"
        private const val KEY_DAILY_SCROLL_COUNT = "daily_scroll_count"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"

        private const val KEY_TRACKING_START_HOUR = "tracking_start_hour"
        private const val KEY_TRACKING_START_MINUTE = "tracking_start_minute"
        private const val KEY_TRACKING_END_HOUR = "tracking_end_hour"
        private const val KEY_TRACKING_END_MINUTE = "tracking_end_minute"

        private const val KEY_HISTORY = "scroll_history"

        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_SESSION_SCROLL_COUNT = "session_scroll_count"
        private const val KEY_LAST_SCROLL_TIME = "last_scroll_time"

        // Watch time keys
        private const val KEY_DAILY_WATCH_TIME_MS = "daily_watch_time_ms"

        // Notification keys
        private const val KEY_LAST_NOTIFIED_SCROLL_COUNT = "last_notified_scroll_count"

        // Goals keys
        private const val KEY_GOALS = "user_goals"
    }

    var trackingStartHour: Int
        get() = prefs.getInt(KEY_TRACKING_START_HOUR, 5)
        set(value) = prefs.edit().putInt(KEY_TRACKING_START_HOUR, value).apply()

    var trackingStartMinute: Int
        get() = prefs.getInt(KEY_TRACKING_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_TRACKING_START_MINUTE, value).apply()

    var trackingEndHour: Int
        get() = prefs.getInt(KEY_TRACKING_END_HOUR, 1) // 1 AM
        set(value) = prefs.edit().putInt(KEY_TRACKING_END_HOUR, value).apply()

    var trackingEndMinute: Int
        get() = prefs.getInt(KEY_TRACKING_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_TRACKING_END_MINUTE, value).apply()

    // Session Logic
    var sessionStartTime: Long
        get() = prefs.getLong(KEY_SESSION_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_SESSION_START_TIME, value).apply()

    var sessionScrollCount: Int
        get() = prefs.getInt(KEY_SESSION_SCROLL_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_SESSION_SCROLL_COUNT, value).apply()

    var lastScrollTime: Long
        get() = prefs.getLong(KEY_LAST_SCROLL_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCROLL_TIME, value).apply()

    var lastNotifiedScrollCount: Int
        get() = prefs.getInt(KEY_LAST_NOTIFIED_SCROLL_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_NOTIFIED_SCROLL_COUNT, value).apply()

    fun getDailyScrollCount(): Int {
        checkAndResetDaily()
        return prefs.getInt(KEY_DAILY_SCROLL_COUNT, 0)
    }

    fun incrementScrollCount() {
        checkAndResetDaily()
        val current = prefs.getInt(KEY_DAILY_SCROLL_COUNT, 0)
        prefs.edit().putInt(KEY_DAILY_SCROLL_COUNT, current + 1).apply()

        // Update session
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScrollTime > 5 * 60 * 1000) { // 5 minutes without scroll resets session
            sessionStartTime = currentTime
            sessionScrollCount = 1
        } else {
            sessionScrollCount += 1
        }
        lastScrollTime = currentTime
    }

    // Watch Time

    fun getDailyWatchTimeMs(): Long {
        checkAndResetDaily()
        return prefs.getLong(KEY_DAILY_WATCH_TIME_MS, 0L)
    }

    /** Add the given milliseconds to today's accumulated watch time. */
    fun addWatchTime(ms: Long) {
        if (ms <= 0L) return
        checkAndResetDaily()
        val current = prefs.getLong(KEY_DAILY_WATCH_TIME_MS, 0L)
        prefs.edit().putLong(KEY_DAILY_WATCH_TIME_MS, current + ms).apply()
    }

    fun isWithinTrackingWindow(): Boolean {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = trackingStartHour * 60 + trackingStartMinute
        val endMinutes = trackingEndHour * 60 + trackingEndMinute

        if (startMinutes <= endMinutes) {
            return currentMinutes in startMinutes..endMinutes
        } else {
            // Crosses midnight
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    private fun checkAndResetDaily() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        var effectiveDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        var effectiveYear = calendar.get(Calendar.YEAR)

        if (currentHour < trackingStartHour || (currentHour == trackingStartHour && currentMinute < trackingStartMinute)) {
            effectiveDayOfYear -= 1
            if (effectiveDayOfYear <= 0) {
                effectiveYear -= 1
                effectiveDayOfYear = 365
            }
        }

        val currentEffectiveDay = effectiveYear * 1000 + effectiveDayOfYear
        val lastResetDay = prefs.getInt(KEY_LAST_RESET_DAY, -1)

        if (currentEffectiveDay != lastResetDay && lastResetDay != -1) {
            val lastCount = prefs.getInt(KEY_DAILY_SCROLL_COUNT, 0)
            val lastWatchTime = prefs.getLong(KEY_DAILY_WATCH_TIME_MS, 0L)
            if (lastCount > 0 || lastWatchTime > 0L) {
                saveToHistory(lastCount, lastWatchTime)
            }
            prefs.edit()
                .putInt(KEY_DAILY_SCROLL_COUNT, 0)
                .putLong(KEY_DAILY_WATCH_TIME_MS, 0L)
                .putInt(KEY_LAST_NOTIFIED_SCROLL_COUNT, 0)
                .putInt(KEY_LAST_RESET_DAY, currentEffectiveDay)
                .apply()
        } else if (lastResetDay == -1) {
            prefs.edit().putInt(KEY_LAST_RESET_DAY, currentEffectiveDay).apply()
        }
    }

    private fun saveToHistory(count: Int, watchTimeMs: Long = 0L) {
        val history = getHistory().toMutableList()
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dateString = sdf.format(cal.time)

        history.add(0, DailyRecord(dateString, count, watchTimeMs))
        if (history.size > 30) {
            history.removeAt(history.size - 1)
        }

        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun getHistory(): List<DailyRecord> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<DailyRecord>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Goals

    fun getGoals(): List<String> {
        val json = prefs.getString(KEY_GOALS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addGoal(goal: String) {
        val currentGoals = getGoals().toMutableList()
        currentGoals.add(goal)
        val json = gson.toJson(currentGoals)
        prefs.edit().putString(KEY_GOALS, json).apply()
    }

    fun removeGoal(goal: String) {
        val currentGoals = getGoals().toMutableList()
        currentGoals.remove(goal)
        val json = gson.toJson(currentGoals)
        prefs.edit().putString(KEY_GOALS, json).apply()
    }
}
