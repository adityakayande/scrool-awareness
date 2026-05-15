package com.example.scrolltracker.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.scrolltracker.data.PreferenceManager
import com.example.scrolltracker.utils.NotificationHelper
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class ScrollAccessibilityService : AccessibilityService() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var notificationHelper: NotificationHelper

    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null

    private var lastScrollTimestamp = 0L
    private var lastEventSignature = ""
    private var isTracking = false

    // ── Watch Time Tracking ───────────────────────────────────────────────────
    /** Timestamp when the current active watch segment started. -1 = not active. */
    private var segmentStartTime = -1L

    /** Timestamp of the last scroll event — used to detect idle. */
    private var lastScrollForTimeout = 0L

    /** Idle timeout: 60 seconds of no scrolling pauses the watch timer. */
    private val IDLE_TIMEOUT_MS = 60_000L

    /** Flush interval: persist accumulated time every 30 seconds. */
    private val FLUSH_INTERVAL_MS = 30_000L

    private val handler = Handler(Looper.getMainLooper())

    private val flushRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (segmentStartTime > 0L) {
                val idleDuration = now - lastScrollForTimeout
                if (idleDuration >= IDLE_TIMEOUT_MS) {
                    // User has been idle — end the segment at the last scroll time
                    val elapsed = lastScrollForTimeout - segmentStartTime
                    if (elapsed > 0L) preferenceManager.addWatchTime(elapsed)
                    segmentStartTime = -1L
                } else {
                    // Still active — flush elapsed time so far
                    val elapsed = now - segmentStartTime
                    preferenceManager.addWatchTime(elapsed)
                    segmentStartTime = now // reset so we don't double-count
                }
                // Update notification with latest watch time
                val scrollCount = preferenceManager.getDailyScrollCount()
                val watchTimeMs = preferenceManager.getDailyWatchTimeMs()
                notificationHelper.updateNotification(scrollCount, watchTimeMs)
            }
            handler.postDelayed(this, FLUSH_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferenceManager = PreferenceManager(this)
        notificationHelper = NotificationHelper(this)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlayView()

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            notificationHelper.getNotification(
                preferenceManager.getDailyScrollCount(),
                preferenceManager.getDailyWatchTimeMs()
            )
        )

        handler.postDelayed(flushRunnable, FLUSH_INTERVAL_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Check if Instagram is in foreground
        if (packageName == "com.instagram.android") {
            if (!isTracking) {
                isTracking = true
                showOverlay()
                // Start a new watch segment if not already active
                if (segmentStartTime < 0L) {
                    segmentStartTime = System.currentTimeMillis()
                    lastScrollForTimeout = segmentStartTime
                }
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isTracking) {
                isTracking = false
                hideOverlay()
                // Flush remaining watch time when leaving Instagram
                flushWatchTime()
            }
        }

        if (!isTracking || packageName != "com.instagram.android") return

        if (!preferenceManager.isWithinTrackingWindow()) return

        val eventType = event.eventType
        val className = event.className?.toString() ?: ""
        val currentTime = System.currentTimeMillis()

        // Ignore Noise (clicks, keyboard etc.)
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return
        }

        // Reel-Specific Check
        var isReel = false
        val sourceNode = event.source
        if (sourceNode != null) {
            val viewId = sourceNode.viewIdResourceName ?: ""
            if (viewId.contains("reel", ignoreCase = true) ||
                viewId.contains("clips", ignoreCase = true) ||
                viewId.contains("view_pager", ignoreCase = true)) {
                isReel = true
            }
            sourceNode.recycle()
        }

        val isScrollableClass = className.contains("RecyclerView") ||
                                className.contains("ViewPager") ||
                                className.contains("ListView")

        if (!isScrollableClass || (!isReel && !className.contains("ViewPager"))) {
            return
        }

        if (eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val currentSignature = "${event.fromIndex}_${event.toIndex}"
        val hasValidIndices = event.fromIndex != -1 || event.toIndex != -1

        // Ignore events where the visible items haven't changed (e.g. small movements, snapping back)
        if (hasValidIndices && currentSignature == lastEventSignature) {
            return
        }

        // Debounce: 1500ms window absorbs all repeated events from a single swipe gesture
        if (currentTime - lastScrollTimestamp > 1500) {
            if (hasValidIndices) {
                lastEventSignature = currentSignature
            }
            lastScrollTimestamp = currentTime
            lastScrollForTimeout = currentTime

            // If we were idle, resume the watch timer
            if (segmentStartTime < 0L) {
                segmentStartTime = currentTime
            }

            // Scroll Count Update
            preferenceManager.incrementScrollCount()

            // Update Notification
            val currentCount = preferenceManager.getDailyScrollCount()
            val watchTimeMs = preferenceManager.getDailyWatchTimeMs()
            notificationHelper.updateNotification(currentCount, watchTimeMs)
            
            triggerOverlayAnimation(currentCount)
            checkAndTriggerHeadsUpReminders(currentCount)
        }
    }

    /** Flush the current watch segment to storage and reset the timer. */
    private fun flushWatchTime() {
        if (segmentStartTime > 0L) {
            val now = System.currentTimeMillis()
            val elapsed = now - segmentStartTime
            if (elapsed > 0L) preferenceManager.addWatchTime(elapsed)
            segmentStartTime = -1L
        }
    }

    override fun onInterrupt() {
        // Called when accessibility service is interrupted (e.g., incoming call).
        flushWatchTime()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        flushWatchTime()
        handler.removeCallbacks(flushRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        hideOverlay()
        return super.onUnbind(intent)
    }

    private fun setupOverlayView() {
        overlayView = TextView(this).apply {
            val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
            setPadding(padding, padding, padding, padding)
            setTextColor(Color.WHITE)
            textSize = 64f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            // Setting a strong text shadow so it remains legible against light backgrounds in reels
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
            
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
        }
    }

    private fun showOverlay() {
        if (overlayView?.parent == null) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                y = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
                x = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
            }
            try {
                windowManager?.addView(overlayView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideOverlay() {
        try {
            if (overlayView?.parent != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerOverlayAnimation(count: Int) {
        overlayView?.apply {
            text = "$count"
            visibility = View.VISIBLE
            alpha = 1.0f
            animate().cancel()
            
            animate()
                .alpha(0f)
                .setStartDelay(1000)
                .setDuration(800)
                .withEndAction {
                    visibility = View.GONE
                }
                .start()
        }
    }

    private fun checkAndTriggerHeadsUpReminders(currentCount: Int) {
        val lastNotified = preferenceManager.lastNotifiedScrollCount
        
        val message = when {
            currentCount == 5 -> "Just 5 reels in. Be intentional."
            currentCount == 10 -> "Is this what you want to do today?"
            currentCount == 15 -> "15 reels down. Stay mindful."
            currentCount == 20 -> "20 reels. Time is slipping away."
            currentCount == 30 -> "Is this how you are accomplishing things today?"
            currentCount == 50 -> "You've scrolled 50 times. Time to take a break."
            currentCount == 100 -> "100 reels consumed. Stop scrolling immediately."
            currentCount > 100 && currentCount % 5 == 0 -> "WARNING: $currentCount reels! You are way past any reasonable limit. Close the app."
            currentCount % 10 == 0 -> "$currentCount reels consumed. Break the cycle."
            currentCount % 5 == 0 -> "$currentCount reels... STILL scrolling?"
            else -> null
        }

        if (message != null && currentCount > lastNotified) {
            var finalMessage = message
            val goals = preferenceManager.getGoals()
            if (goals.isNotEmpty()) {
                val randomGoal = goals.random()
                finalMessage = "$message\n\nRemember your goal: $randomGoal. Break the cycle."
            }
            
            // Cycle through 3 different sounds every 10 reels (e.g. 10->0, 20->1, 30->2, 40->0)
            val soundIndex = if (currentCount % 10 == 0) {
                ((currentCount / 10) - 1) % 3
            } else {
                null
            }
            
            preferenceManager.lastNotifiedScrollCount = currentCount
            notificationHelper.sendHeadsUpReminder("Scroll Warning", finalMessage, currentCount, soundIndex)
        }
    }
}
