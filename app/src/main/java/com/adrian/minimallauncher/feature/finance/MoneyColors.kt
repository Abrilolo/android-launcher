package com.adrian.minimallauncher.feature.finance

import androidx.compose.ui.graphics.Color

/** Muted, cool data colors, independent of the launcher's neutral surfaces and red actions. */
internal val FinanceTeal = Color(0xFF62CDBD)
internal val FinanceBlue = Color(0xFF72B5EA)
internal val FinanceLavender = Color(0xFFB29AE8)
internal val FinanceSlate = Color(0xFF809FDD)
internal val FinanceSage = Color(0xFF8AC6A3)
internal val FinanceMist = Color(0xFF97CFE0)
internal val FinanceChartColors = listOf(FinanceTeal, FinanceBlue, FinanceLavender, FinanceSage, FinanceSlate, FinanceMist)

/** Keep a category's hue stable across periods, filters and reordered budget rows. */
internal fun financeCategoryColor(name: String): Color {
    val key = name.trim().lowercase(java.util.Locale.ROOT)
    val base = BASE_CATEGORIES.indexOf(key)
    val index = if (base >= 0) base + 1 else Math.floorMod(key.hashCode(), FinanceChartColors.size)
    return FinanceChartColors[index % FinanceChartColors.size]
}

/** Diseño (2026-11-09): verde éxito: sobra; azul principal: en uso; ámbar aviso: poco; rosa peligro: pasado. */
internal val MoneyPlenty = Color(0xFF4DD402)
internal val MoneyGoing = Color(0xFF1A77B5)
internal val MoneyLow = Color(0xFFFFCF7D)
internal val MoneyOver = Color(0xFFFF2962)

/** Empty data retains its hue, instead of falling back to the neutral panel gray. */
internal fun inactiveFinanceColor(color: Color) = color.copy(alpha = 0.42f)

/** The color for having spent [spentCents] of [budgetCents]. */
internal fun usageColor(spentCents: Long, budgetCents: Long): Color = when {
    spentCents <= 0 -> MoneyPlenty
    budgetCents <= 0 || spentCents > budgetCents -> MoneyOver
    spentCents * 10 < budgetCents * 5 -> MoneyPlenty
    spentCents * 10 < budgetCents * 8 -> MoneyGoing
    else -> MoneyLow
}

/** The same scale read from what is left of a total. */
internal fun leftColor(leftCents: Long, totalCents: Long): Color = usageColor(totalCents - leftCents, totalCents)

/** The state's name for [usageColor], for tests and descriptions. */
internal object FinanceColorsProbe {
    fun state(spentCents: Long, budgetCents: Long): String = when (usageColor(spentCents, budgetCents)) {
        MoneyPlenty -> "plenty"
        MoneyGoing -> "going"
        MoneyLow -> "low"
        else -> "over"
    }
}
