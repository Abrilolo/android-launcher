package com.adrian.minimallauncher

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.adrian.minimallauncher.data.ForcedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only: a quick settings tile "Pago de prueba" that creates a fake pending payment ($85.00 at
 * OXXO, like a bank notification would) and opens the launcher, so the forced flow can be tried by hand.
 * Bank notification parsing comes later.
 */
class DebugPaymentTile : TileService() {
    override fun onClick() {
        runBlocking(Dispatchers.IO) { ForcedRepository(applicationContext).addTestPayment() }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, home, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(home)
        }
    }
}

/** Debug builds only: a quick settings tile "Rutina" that opens the routine test panel (placeholder). */
class DebugRoutineTile : TileService() {
    override fun onClick() {
        val open = Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_ROUTINE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 1, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(open)
        }
    }
}

/**
 * Debug builds only, from adb (the DUMP permission keeps other apps out):
 * `adb shell am broadcast -a com.adrian.minimallauncher.DEBUG_PAYMENT -n com.adrian.minimallauncher/.DebugPaymentReceiver --el amount 8500 --es merchant OXXO`
 */
class DebugPaymentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ForcedRepository(context).addTestPayment(intent.getLongExtra("amount", 8500), intent.getStringExtra("merchant") ?: "OXXO") }
            finally { pending.finish() }
        }
    }
}
