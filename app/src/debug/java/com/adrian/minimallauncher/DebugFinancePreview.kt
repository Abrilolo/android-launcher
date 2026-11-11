package com.adrian.minimallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adrian.minimallauncher.data.FinanceRecord
import com.adrian.minimallauncher.data.MoneyKind
import com.adrian.minimallauncher.feature.bank.*
import com.adrian.minimallauncher.feature.finance.*
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Native visual fixture. Every edit stays in memory; never opens the user's repositories. */
class DebugFinancePreview : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra("verifyPersistence", false)) lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { verifyFinancePersistence(this@DebugFinancePreview) }
                .getOrElse { "FAIL: ${it.javaClass.simpleName}: ${it.message}" }
            java.io.File(filesDir, "finance-verification.txt").writeText(result)
        }
        val empty = intent.getBooleanExtra("empty", false)
        val large = intent.getBooleanExtra("large", false)
        val today = LocalDate.of(2026, 11, 6)
        val timestamp = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setContent {
            LauncherTheme {
                val money = remember { mutableStateListOf<FinanceRecord>().apply {
                    if (!empty) addAll(listOf(
                        FinanceRecord("meal", 7000, "Comida", timestamp, category = "comida"),
                        FinanceRecord("ride", 11000, "Transporte", timestamp - 1000, category = "transporte"),
                        FinanceRecord("coffee", 6500, "Café", timestamp - 2000, category = "comida"),
                    ))
                } }
                val accounts = remember { mutableStateListOf<BankAccount>().apply {
                    if (!empty) addAll(listOf(
                        BankAccount("demo-debit", "Cuenta de ejemplo", BankKind.DEBIT, if (large) 12345678900 else 1800000),
                        BankAccount("demo-card", "Tarjeta de ejemplo", BankKind.CARD, 250000, cutDay = 10, payDay = 20),
                    ))
                } }
                val receivables = remember { mutableStateListOf<BankReceivable>() }
                val limits = remember { mutableStateListOf<BudgetLimit>().apply {
                    if (!empty) addAll(listOf(
                        BudgetLimit("food", "comida", BudgetPeriod.DAY, 15000),
                        BudgetLimit("transport", "transporte", BudgetPeriod.DAY, 10000),
                    ))
                } }
                var plan by remember { mutableStateOf(if (empty) "" else FinancePlan(2400000, listOf(PlanCost("Renta", 600000, BudgetPeriod.MONTH)), listOf(ScheduledPayment("music", "Música", 9900, "demo-card", 10))).encode()) }
                var saving by remember { mutableIntStateOf(10) }
                var daily by remember { mutableLongStateOf(if (empty) 0 else 50000) }
                var goal by remember { mutableStateOf(if (empty) "" else SavingGoal(300000, today.minusMonths(1), today.plusMonths(5), 80000).encode()) }
                var widget by remember { mutableStateOf(intent.getBooleanExtra("widget", false)) }
                val summary = Budget.summary(money.toList(), limits.toList(), saving, daily, today, FinancePlan.decode(plan))
                Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
                    Text("Datos de ejemplo · vista de desarrollo", Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = FinanceMuted)
                    if (widget) {
                        FinanceWidget(FinanceWidgets.TODAY, FinanceWidgetData(summary, money.toList(), FinancePlan.decode(plan), goal, emptyList(), today), Modifier.width(200.dp).height(220.dp), { widget = false }, {})
                    } else FinanceScreen(
                        summary = summary, money = money.toList(), dailyLimitCents = daily,
                        financePlan = plan, onSavePlan = { plan = it }, savingGoal = goal, onSaveGoal = { goal = it },
                        onAdd = { kind, cents, description, category, pending, account ->
                            val duplicate = category.startsWith("recurring:") && money.any { it.category == category && java.time.YearMonth.from(Budget.dateOf(it)) == java.time.YearMonth.from(today) }
                            if (!duplicate) {
                                val record = FinanceRecord(UUID.randomUUID().toString(), cents, description, timestamp + money.size, kind, category, pending, account = account)
                                Bank.validateMovement(accounts.toList(), record)
                                if ((kind == MoneyKind.WITHDRAWAL || account == Bank.CASH_ACCOUNT_ID) && accounts.none { it.id == Bank.CASH_ACCOUNT_ID }) accounts.add(Bank.cashAccount())
                                money.add(record)
                                accounts.replaceAll { row -> Bank.applied(row, record, 1) ?: row }
                            }
                        },
                        onUpdate = { record ->
                            Bank.validateMovement(accounts.toList(), record)
                            if ((record.kind == MoneyKind.WITHDRAWAL || record.account == Bank.CASH_ACCOUNT_ID) && accounts.none { it.id == Bank.CASH_ACCOUNT_ID }) accounts.add(Bank.cashAccount())
                            money.firstOrNull { it.id == record.id }?.let { old -> accounts.replaceAll { Bank.applied(it, old, -1) ?: it } }
                            accounts.replaceAll { Bank.applied(it, record, 1) ?: it }
                            money.replaceAll { if (it.id == record.id) record else it }
                        },
                        accounts = accounts.toList(), receivables = receivables.toList(),
                        onSaveAccount = { account -> accounts.removeAll { it.id == account.id }; accounts.add(account) },
                        onDeleteAccount = { id -> accounts.removeAll { it.id == id } },
                        onSaveReceivable = { value -> receivables.removeAll { it.id == value.id }; receivables.add(value) },
                        onDeleteReceivable = { id -> receivables.removeAll { it.id == id } },
                        onCollectReceivable = { value -> receivables.removeAll { it.id == value.id } },
                        onSettle = { id, cents -> money.replaceAll { if (it.id == id) it.copy(pendingFromOthersCents = (it.pendingFromOthersCents - cents).coerceAtLeast(0)) else it } },
                        onSetBudget = { topic, period, cents -> limits.removeAll { it.topic == topic && it.period == period }; limits.add(BudgetLimit(BudgetLimit.idOf(topic, period), topic, period, cents)) },
                        onSavingPct = { saving = it }, onDailyLimit = { daily = it },
                        onDelete = { id -> money.firstOrNull { it.id == id }?.let { old -> accounts.replaceAll { Bank.applied(it, old, -1) ?: it } }; money.removeAll { it.id == id } }, onBack = { widget = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
