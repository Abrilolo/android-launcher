package com.adrian.minimallauncher

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.adrian.minimallauncher.data.BankApps
import com.adrian.minimallauncher.data.BankParser
import com.adrian.minimallauncher.data.ForcedRepository
import com.adrian.minimallauncher.data.LauncherDatabase
import com.adrian.minimallauncher.feature.bank.Bank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads notifications from the bank apps in [BankApps.watched] and turns a payment or an income into a pending
 * row, which opens the forced registration flow. Text from other apps is never read. Nothing is stored but the
 * amount, direction and merchant. The user turns it on once in Ajustes → Notificaciones → Acceso a notificaciones.
 */
class BankNotificationService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in BankApps.watched || sbn.isOngoing) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val parsed = BankParser.parse(sbn.packageName, title, text) ?: return
        // `when` stays the same when the app updates its notice, so the id (and the row) do too.
        val at = n.`when`.takeIf { it > 0 } ?: sbn.postTime
        val id = BankParser.stableId(sbn.packageName, parsed.amountCents, at)
        scope.launch {
            val repo = ForcedRepository(applicationContext)
            val accounts = LauncherDatabase.get(applicationContext).bank().allAccounts()
            val account = Bank.resolve(accounts, BankApps.bankOf(sbn.packageName), parsed.digits, parsed.credit)?.id.orEmpty()
            when (parsed.type) {
                BankParser.Type.PURCHASE, BankParser.Type.INCOME ->
                    repo.addPayment(parsed.amountCents, sbn.packageName, parsed.merchant, parsed.direction, id, account)
                BankParser.Type.WITHDRAWAL -> if (account.isNotBlank()) repo.saveWithdrawal(parsed.amountCents, account, parsed.merchant, id)
                BankParser.Type.CARD_PAYMENT -> if (account.isNotBlank()) repo.payCard(account, parsed.amountCents, sbn.packageName, id)
                BankParser.Type.STATEMENT -> if (account.isNotBlank()) repo.setStatement(account, parsed.amountCents, parsed.dueDay)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Whether the user gave notification access to this launcher. */
        fun enabled(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        fun component(context: Context) = ComponentName(context, BankNotificationService::class.java)
    }
}
