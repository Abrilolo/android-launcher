package com.adrian.minimallauncher

import com.adrian.minimallauncher.data.BankParser
import com.adrian.minimallauncher.data.PendingPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankParserTest {
    private val pkg = "com.nu.production"
    private val OUT = PendingPayment.OUT
    private val IN = PendingPayment.IN

    @Test fun purchaseIsPaymentWithMerchant() {
        val p = BankParser.parse(pkg, "Compra aprobada", "Compra en OXXO por $85.00")!!
        assertEquals(8500L, p.amountCents)
        assertEquals(OUT, p.direction)
        assertEquals("OXXO", p.merchant)
    }

    @Test fun thousandsAndNoCents() {
        assertEquals(123450L, BankParser.parse(pkg, "", "Pagaste $1,234.5 en Amazon")!!.amountCents)
        assertEquals(20000L, BankParser.parse(pkg, "", "Retiro de $200 en cajero")!!.amountCents)
        assertEquals(9900L, BankParser.parse(pkg, "", "Cargo de 99.00 MXN")!!.amountCents)
    }

    @Test fun receivedTransferIsIncome() {
        val p = BankParser.parse(pkg, "Recibiste una transferencia", "Te envió $1,500.00 Juan Pérez")!!
        assertEquals(150000L, p.amountCents)
        assertEquals(IN, p.direction)
    }

    @Test fun incomeWinsOverPaymentWord() {
        assertEquals(IN, BankParser.parse(pkg, "", "Recibiste un pago de $300.00")!!.direction)
    }

    @Test fun accentsDoNotMatter() {
        assertEquals(IN, BankParser.parse(pkg, "Depósito", "Llegó un depósito de $10,000.00")!!.direction)
    }

    @Test fun promosCodesAndFailuresAreIgnored() {
        assertNull(BankParser.parse(pkg, "", "Obtén 5% de cashback en compras de más de $500"))
        assertNull(BankParser.parse(pkg, "", "Tu código de seguridad es 123456"))
        assertNull(BankParser.parse(pkg, "", "Tu compra por $85.00 fue rechazada"))
        assertNull(BankParser.parse(pkg, "", "Tu fecha límite de pago es mañana: $2,000.00"))
    }

    @Test fun noAmountOrNoMovementIsIgnored() {
        assertNull(BankParser.parse(pkg, "Compra", "Tu tarjeta está lista"))
        assertNull(BankParser.parse(pkg, "", "Tu saldo es $5,000.00"))
    }

    /** Real notices from the phone (2026-11-04): title and text come apart. */
    @Test fun realPhoneNotices() {
        assertEquals(
            BankParser.Parsed(9, OUT, "Adrian Revolut"),
            BankParser.parse("com.nu.production", "Transferencia exitosa", "Enviaste $0.09 a Adrian Revolut."),
        )
        assertEquals(
            BankParser.Parsed(9, IN, "Adrian Tavera Aquino"),
            BankParser.parse("com.revolut.revolut", "Has recibido MXN0.09", "Adrian Tavera Aquino te ha enviado dinero. Pulsa para ver los detalles"),
        )
        assertEquals(
            BankParser.Parsed(18018, OUT, "Ippudo"),
            BankParser.parse("com.revolut.revolut", "Ippudo", "🍽️ Pagado MXN180.18 (¥1,510) en Ippudo..."),
        )
    }

    /** Samples given by the user (2026-11-04): package, text, then amount, direction and merchant, or null to ignore. */
    private data class Sample(val pkg: String, val text: String, val cents: Long?, val direction: String? = null, val merchant: String = "")

    private val samples = listOf(
        Sample("com.nu.production", "Compra aprobada por \$150.00 MXN en OXXO", 15000, OUT, "OXXO"),
        Sample("com.nu.production", "Enviaste \$500.00 MXN a Juan Pérez", 50000, OUT, "Juan Pérez"),
        Sample("com.nu.production", "Recibiste una transferencia por \$1,200.00 MXN de Maria", 120000, IN, "Maria"),
        Sample("com.nu.production", "Recuerda que tu fecha límite de pago es el 15 de Noviembre", null),
        Sample("com.revolut.revolut", "Pagaste \$25.00 con tu tarjeta en Starbucks", 2500, OUT, "Starbucks"),
        Sample("com.revolut.revolut", "Enviaste \$300.00 a Carlos", 30000, OUT, "Carlos"),
        Sample("com.revolut.revolut", "Transferencia recibida: +\$1,000.00 de Pedro", 100000, IN, "Pedro"),
        Sample("com.revolut.revolut", "Ganaste \$50.00 de cashback este mes", null),
        Sample("com.mercadopago.wallet", "Pagaste \$89.00 en Mercado Libre", 8900, OUT, "Mercado Libre"),
        Sample("com.mercadopago.wallet", "Transferiste \$200.00 a cuenta CLABE ****1234", 20000, OUT, "CLABE ****1234"),
        Sample("com.mercadopago.wallet", "¡Te ingresaron \$450.00! Dinero disponible en tu cuenta", 45000, IN),
        Sample("com.mercadopago.wallet", "Tienes un cupón de 10% de descuento disponible", null),
        Sample("com.bancomer.mbanking", "BBVA: Compra por \$430.00 en Walmart el 04/11", 43000, OUT, "Walmart"),
        Sample("com.bancomer.mbanking", "BBVA: Transferencia enviada por \$1,500.00 a favor de Ana", 150000, OUT, "Ana"),
        Sample("com.bancomer.mbanking", "BBVA: Depósito recibido por \$3,000.00 abonado a tu cuenta", 300000, IN),
        Sample("com.bancomer.mbanking", "BBVA: Tu código de seguridad para ingresar es 849201", null),
        Sample("com.storicard.stori", "Compra realizada por \$120.00 en Farmacias Guadalajara", 12000, OUT, "Farmacias Guadalajara"),
        Sample("com.storicard.stori", "Transferencia exitosa de \$400.00 a Banco X", 40000, OUT, "Banco X"),
        Sample("com.storicard.stori", "Abono recibido por \$800.00 en tu cuenta Stori", 80000, IN),
        Sample("com.storicard.stori", "¡Felicidades! Incrementamos tu línea de crédito", null),
        Sample("card.plata.app", "Pago con tarjeta Plata por \$95.00 en Uber", 9500, OUT, "Uber"),
        Sample("card.plata.app", "Envío de dinero completado por \$250.00", 25000, OUT),
        Sample("card.plata.app", "Recibiste \$600.00 en tu saldo Plata", 60000, IN),
        Sample("card.plata.app", "Pago de tarjeta pendiente. Revisa tu estado de cuenta", null),
    )

    @Test fun userSamples() {
        val wrong = samples.mapNotNull { s ->
            val p = BankParser.parse(s.pkg, "", s.text)
            val ok = if (s.cents == null) p == null else p != null && p.amountCents == s.cents && p.direction == s.direction && p.merchant == s.merchant
            if (ok) null else "${s.text} → $p"
        }
        assertEquals("", wrong.joinToString("\n"))
    }

    @Test fun stableIdIsSameWithinAMinute() {
        assertEquals(BankParser.stableId(pkg, 8500, 120_000), BankParser.stableId(pkg, 8500, 150_000))
    }
}
