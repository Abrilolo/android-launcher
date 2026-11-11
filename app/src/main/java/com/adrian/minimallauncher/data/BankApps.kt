package com.adrian.minimallauncher.data

/**
 * The money apps the launcher knows. [notifies] marks the ones whose notifications are read for payments and income
 * (investment and crypto apps are left out). [accountId] is the bank account their movements belong to, the id of a
 * card or account in Banco; empty when the app is not tied to one.
 */
object BankApps {
    data class BankApp(val packageName: String, val notifies: Boolean, val accountId: String = "")

    val all = listOf(
        BankApp("com.nu.production", true, "nu"),
        BankApp("com.bancomer.mbanking", true, "bbva"),
        BankApp("com.revolut.revolut", true, "revolut"),
        BankApp("com.storicard.stori", true, "stori"),
        BankApp("card.plata.app", true, "plata"),
        BankApp("com.mercadopago.wallet", true, "mercadopago"),
        BankApp("com.google.android.apps.walletnfcrel", true),
        BankApp("com.dolarapp", false),
        BankApp("com.gbm.app", false),
        BankApp("com.gbm.homebroker", false),
        BankApp("com.bitso.exchange", false),
        BankApp("com.bitso.wallet", false),
        BankApp("com.binance.dev", false),
    )

    /** Packages whose notifications [BankParser] reads. */
    val watched: Set<String> = all.filter { it.notifies }.map { it.packageName }.toSet()

    /** The bank a package belongs to, as [com.adrian.minimallauncher.feature.bank.Bank.bankKey] spells it ("mercadopago"). */
    fun bankOf(packageName: String): String = when (packageName) {
        "com.nu.production" -> "nu"
        "com.bancomer.mbanking" -> "bbva"
        "com.revolut.revolut" -> "revolut"
        "com.storicard.stori" -> "stori"
        "card.plata.app" -> "plata"
        "com.mercadopago.wallet" -> "mercadopago"
        else -> ""
    }

    /** The bank account a package's movements go to, empty when none. */
    fun accountOf(packageName: String): String = all.firstOrNull { it.packageName == packageName }?.accountId.orEmpty()
}
