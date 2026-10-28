package com.adrian.minimallauncher

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.adrian.minimallauncher.data.ForcedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Forced mode for the whole phone without the draw-over-other-apps permission: while a payment (or salida's
 * registration) is pending, any app that comes to the front is sent back home, and home is this launcher,
 * which shows the registration flow. It reads only which app is in front (window changes), never screen content.
 * The user turns it on once in Ajustes → Accesibilidad → Apps instaladas → Minimal Launcher.
 */
class ForcedModeService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile private var active = false
    private var lastHomeAt = 0L
    private var keyboards: Set<String> = emptySet()

    override fun onServiceConnected() {
        keyboards = getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.map { it.packageName }?.toSet().orEmpty()
        scope.launch {
            ForcedRepository(applicationContext).active.distinctUntilChanged().collect { on ->
                active = on
                // A payment just became pending: go to the flow right away.
                if (on) goHome()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!active || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in ALLOWED || pkg in keyboards) return
        goHome()
    }

    private fun goHome() {
        val now = SystemClock.uptimeMillis()
        if (now - lastHomeAt < DEBOUNCE_MS) return
        lastHomeAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val DEBOUNCE_MS = 700L
        /**
         * Never blocked: the system UI (shade, lock screen, recents), calls and emergency dialing, and Settings,
         * which is also the way out if the service ever misbehaves (turning it off there).
         */
        val ALLOWED = setOf(
            "android", "com.android.systemui", "com.android.settings",
            "com.android.phone", "com.android.server.telecom", "com.samsung.android.dialer", "com.google.android.dialer",
            "com.samsung.android.incallui", "com.android.emergency", "com.sec.android.app.launcher",
        )
    }
}
