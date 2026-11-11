package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.BankParser
import com.adrian.minimallauncher.data.BankParser.Type
import com.adrian.minimallauncher.feature.bank.Bank
import com.adrian.minimallauncher.feature.bank.BankAccount
import com.adrian.minimallauncher.feature.bank.BankKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The five banks' notices as the user gave them (2026-11-11), and where each one goes. */
class BankNoticesTest {
    private val revolut = "com.revolut.revolut"
    private val nu = "com.nu.production"
    private val stori = "com.storicard.stori"
    private val plata = "card.plata.app"
    private val mp = "com.mercadopago.wallet"

    private fun parse(pkg: String, text: String) = BankParser.parse(pkg, "", text)

    @Test fun revolut() {
        parse(revolut, "💳 Revolut: Gastaste \$350.00 MXN en Starbucks. Balance actual: \$2,450.00 MXN.")!!.let {
            assertEquals(Type.PURCHASE, it.type); assertEquals(35000L, it.amountCents); assertEquals("Starbucks", it.merchant); assertEquals(false, it.credit)
        }
        parse(revolut, "💰 Revolut: ¡Dinero recibido! Juan Pérez te envió \$1,200.00 MXN.")!!.let {
            assertEquals(Type.INCOME, it.type); assertEquals(120000L, it.amountCents); assertEquals("Juan Pérez", it.merchant)
        }
        parse(revolut, "🏧 Revolut: Retiro exitoso de \$500.00 MXN en ATM BBVA.")!!.let {
            assertEquals(Type.WITHDRAWAL, it.type); assertEquals(50000L, it.amountCents)
        }
        parse(revolut, "📅 Revolut: Tu estado de cuenta está listo. Recuerda pagar \$3,400.00 MXN antes del 23 de este mes para evitar intereses.")!!.let {
            assertEquals(Type.STATEMENT, it.type); assertEquals(340000L, it.amountCents); assertEquals(23, it.dueDay)
        }
    }

    @Test fun nu() {
        parse(nu, "💜 Nu: Compra aprobada por \$890.00 MXN en Amazon México.")!!.let {
            assertEquals(Type.PURCHASE, it.type); assertEquals(89000L, it.amountCents); assertEquals("Amazon México", it.merchant); assertTrue(it.credit)
        }
        parse(nu, "💸 Nu: Recibiste una transferencia de \$2,000.00 MXN de Sofía Ramos. ¡Tu dinero ya está rindiendo!")!!.let {
            assertEquals(Type.INCOME, it.type); assertEquals("Sofía Ramos", it.merchant)
        }
        assertEquals(Type.WITHDRAWAL, parse(nu, "🏧 Nu: Retiro de efectivo de tu Cuenta Nu por \$400.00 MXN.")!!.type)
        parse(nu, "⏳ Nu: Tu fecha de pago es el 24. El pago para no generar intereses de tu tarjeta de crédito es de \$1,850.00 MXN.")!!.let {
            assertEquals(Type.STATEMENT, it.type); assertEquals(185000L, it.amountCents); assertEquals(24, it.dueDay)
        }
    }

    @Test fun stori() {
        parse(stori, "🛒 Stori: Compra exitosa por \$180.00 MXN en OXXO. ¡Sigue sumando historial!")!!.let {
            assertEquals(Type.PURCHASE, it.type); assertEquals("OXXO", it.merchant)
        }
        assertEquals(Type.INCOME, parse(stori, "📥 Stori: Tu transferencia por \$500.00 MXN ha sido recibida con éxito.")!!.type)
        parse(stori, "💵 Stori: Disposición de efectivo con tarjeta por \$300.00 MXN.")!!.let {
            assertEquals(Type.WITHDRAWAL, it.type); assertTrue(it.credit)
        }
        parse(stori, "⚠️ Stori: ¡Que no se te pase! Tu fecha de corte fue el 12. Tienes hasta el 2 de diciembre para pagar \$950.00 MXN.")!!.let {
            assertEquals(Type.STATEMENT, it.type); assertEquals(95000L, it.amountCents); assertEquals(2, it.dueDay)
        }
    }

    @Test fun plata() {
        parse(plata, "🛍️ Plata: Compra de \$1,450.00 MXN en Walmart aprobada. Recibirás 1% de Cashback.")!!.let {
            assertEquals(Type.PURCHASE, it.type); assertEquals(145000L, it.amountCents); assertEquals("Walmart", it.merchant)
        }
        assertEquals(Type.CARD_PAYMENT, parse(plata, "✅ Plata: Recibimos tu pago por \$2,100.00 MXN. Tu saldo disponible se ha actualizado.")!!.type)
        parse(plata, "🏧 Plata: Retiro en cajero por \$1,000.00 MXN. Recuerda revisar las comisiones aplicables.")!!.let {
            assertEquals(Type.WITHDRAWAL, it.type); assertTrue(it.credit)
        }
        parse(plata, "🔔 Plata: Tu pago de \$4,200.00 MXN vence el 26 de noviembre. Aprovecha tus 30 días de gracia.")!!.let {
            assertEquals(Type.STATEMENT, it.type); assertEquals(26, it.dueDay)
        }
    }

    @Test fun mercadoPago() {
        parse(mp, "💳 Mercado Pago: Pagaste \$220.00 MXN en Uber.")!!.let {
            assertEquals(Type.PURCHASE, it.type); assertEquals("Uber", it.merchant)
        }
        assertEquals(Type.INCOME, parse(mp, "📥 Mercado Pago: ¡Te ingresaron dinero! Recibiste \$750.00 MXN a tu cuenta digital.")!!.type)
        assertEquals(Type.WITHDRAWAL, parse(mp, "🏪 Mercado Pago: Retiro de efectivo por \$600.00 MXN en 7-Eleven.")!!.type)
        // The minimum payment is not what the card owes.
        assertNull(parse(mp, "📝 Mercado Pago: Tu tarjeta de crédito cortó el 13 de noviembre. Tu pago mínimo es de \$300.00 MXN y vence el 3 de diciembre."))
    }

    @Test fun cardDigitsChooseTheAccount() {
        val p = parse(nu, "Compra aprobada por \$100.00 con tu tarjeta terminación 4321")!!
        assertEquals("4321", p.digits)
        val accounts = listOf(
            BankAccount("nu", "Nu", BankKind.CARD, 0),
            BankAccount("nu-debito", "Nu", BankKind.DEBIT, 0, cards = "4321"),
        )
        assertEquals("nu-debito", Bank.resolve(accounts, "nu", p.digits, p.credit)!!.id)
        assertEquals("nu", Bank.resolve(accounts, "nu", null, true)!!.id)
        assertEquals("nu-debito", Bank.resolve(accounts, "nu", null, false)!!.id)
        assertEquals("mercadopago", Bank.resolve(listOf(BankAccount("mercadopago", "Mercado Pago", BankKind.CARD, 0)), "mercadopago", null, true)!!.id)
    }

    private val today = LocalDate.of(2026, 11, 11)

    @Test fun purchaseLandsInTheRightPayment() {
        val nuCard = BankAccount("nu", "Nu", BankKind.CARD, 987741, cutDay = 14, payDay = 24, dueCents = 987741, dueDate = "2026-11-24")
        Bank.charged(nuCard, 10000, today).let { assertEquals(997741L, it.dueCents); assertEquals(0L, it.nextDueCents) }
        val revolut = BankAccount("revolut", "Revolut", BankKind.CARD, 1414067, cutDay = 3, payDay = 23, dueCents = 988819, dueDate = "2026-11-23", nextDueCents = 382209)
        Bank.charged(revolut, 10000, today).let { assertEquals(988819L, it.dueCents); assertEquals(392209L, it.nextDueCents) }
        val plata = BankAccount("plata", "Plata", BankKind.CARD, 134479, cutDay = 27, payDay = 26, dueCents = 26286, dueDate = "2026-11-26", nextDueCents = 108193)
        Bank.charged(plata, 10000, today).let { assertEquals(26286L, it.dueCents); assertEquals(118193L, it.nextDueCents) }
        val stori = BankAccount("stori", "Stori", BankKind.CARD, 211096, cutDay = 12, payDay = 2, dueCents = 211096, dueDate = "2026-12-02")
        Bank.charged(stori, 10000, today).let { assertEquals(221096L, it.dueCents) }
    }

    @Test fun paymentDayRollsAndPaymentsLower() {
        val revolut = BankAccount("revolut", "Revolut", BankKind.CARD, 1414067, cutDay = 3, payDay = 23, dueCents = 988819, dueDate = "2026-11-23", nextDueCents = 382209)
        val paid = Bank.paid(revolut, 988819)
        assertEquals(0L, paid.dueCents)
        val rolled = Bank.rolled(paid, LocalDate.of(2026, 11, 24))
        assertEquals(382209L, rolled.dueCents); assertEquals(0L, rolled.nextDueCents); assertEquals("2026-12-23", rolled.dueDate)
    }
}
