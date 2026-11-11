package com.adrian.minimallauncher.feature.finance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * The gear of Finanzas (2026-11-11): only what is set once and is not part of the month. The plan and the caps live
 * in Presupuesto, the accounts in Banco.
 * 1. Avisos del banco: whether the launcher can read the bank apps' notices.
 * 2. Meta de ahorro: the goal and what is already set aside.
 */
@Composable
internal fun FinanceGear(savingGoal: String, onSaveGoal: (String) -> Unit, today: LocalDate, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge, color = FinanceWhite)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead("Avisos del banco", "Con esto el launcher lee las notificaciones de tus bancos: cada compra o ingreso te pide registrarlo con la cuenta ya elegida. Solo se lee el monto, el comercio y la tarjeta.")
            Column(Modifier.fillMaxWidth().glass().padding(16.dp)) { BankNoticesRow() }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHead("Meta de ahorro", "Para qué ahorras y para cuándo. Lo que apartas desde Presupuesto (Para ahorro) se suma aquí.")
            Column(Modifier.fillMaxWidth().glass().padding(16.dp)) { SavingGoalView(savingGoal, today, onSaveGoal) }
        }
    }
}
