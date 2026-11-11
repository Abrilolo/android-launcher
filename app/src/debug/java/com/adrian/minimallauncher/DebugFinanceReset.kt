package com.adrian.minimallauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug only: sets Finanzas back to zero (plan, saving %, daily limit, topic budgets, saving goal) and keeps every
 * recorded movement. adb shell am broadcast -a com.adrian.minimallauncher.DEBUG_FINANCE_RESET -p com.adrian.minimallauncher
 */
class DebugFinanceResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context)
                val before = settings.moduleSettings.first()
                Log.i("DebugFinanceReset", "before: plan=${before.financePlan} saving=${before.savingPct} daily=${before.dailyBudgetCents} goal=${before.savingGoal}")
                settings.setModuleSettings(before.copy(financePlan = "", savingPct = 0, dailyBudgetCents = 0, savingGoal = ""))
                val budgets = LauncherDatabase.get(context).budgets()
                val rows = budgets.all()
                rows.forEach { budgets.delete(it.id) }
                Log.i("DebugFinanceReset", "done: removed ${rows.size} budgets, movements kept")
            } finally { pending.finish() }
        }
    }
}
