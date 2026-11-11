package com.adrian.minimallauncher.data

import java.text.Normalizer
import java.util.UUID

/**
 * Reads a bank app's notification and says what happened: a purchase, money received, cash taken out, a payment made
 * to a card, or the card's payment reminder. Pure Kotlin so it is tested in `app/src/test`. Per-bank rules go in
 * [bankRules] (built from the banks' real notices, 2026-11-11); anything they do not match falls to the general
 * keywords.
 */
object BankParser {
    enum class Type { PURCHASE, INCOME, WITHDRAWAL, CARD_PAYMENT, STATEMENT }

    /**
     * [digits]: the card's last 4 when the notice names them. [credit]: the movement is on a credit card (a purchase on
     * credit, cash from a card), so it goes to the bank's card unless [digits] say otherwise. [dueDay]: for a
     * [Type.STATEMENT], the day of the month it must be paid by.
     */
    data class Parsed(
        val amountCents: Long,
        val direction: String,
        val merchant: String = "",
        val type: Type = if (direction == PendingPayment.IN) Type.INCOME else Type.PURCHASE,
        val digits: String? = null,
        val credit: Boolean = false,
        val dueDay: Int? = null,
    )

    /** A bank-specific rule: a pattern over the normalized text, what it means and whether it is on credit. */
    private data class Rule(val pattern: Regex, val type: Type, val credit: Boolean = false)

    private fun r(pattern: String) = Regex(pattern)

    /** Per-package rules, checked before anything else (the general ignore list would drop reminders). */
    private val bankRules: Map<String, List<Rule>> = mapOf(
        "com.revolut.revolut" to listOf(
            Rule(r("estado de cuenta|recuerda pagar"), Type.STATEMENT),
            Rule(r("retiro"), Type.WITHDRAWAL),
            Rule(r("dinero recibido|te envio|te ha enviado"), Type.INCOME),
            // "Balance actual" belongs to the debit account; a purchase without it is taken as the credit card.
            Rule(r("gastaste(?s:.)*balance"), Type.PURCHASE),
            Rule(r("gastaste|compra"), Type.PURCHASE, credit = true),
        ),
        "com.nu.production" to listOf(
            Rule(r("fecha de pago|para no generar intereses"), Type.STATEMENT),
            Rule(r("retiro de efectivo"), Type.WITHDRAWAL),
            Rule(r("recibiste una transferencia|recibiste"), Type.INCOME),
            Rule(r("compra aprobada|compra"), Type.PURCHASE, credit = true),
        ),
        "com.storicard.stori" to listOf(
            Rule(r("fecha de corte|tienes hasta"), Type.STATEMENT),
            Rule(r("disposicion de efectivo"), Type.WITHDRAWAL, credit = true),
            Rule(r("transferencia(?s:.)*recibida|recibiste"), Type.INCOME),
            Rule(r("compra exitosa|compra"), Type.PURCHASE, credit = true),
        ),
        "card.plata.app" to listOf(
            Rule(r("tu pago de(?s:.)*vence|vence el"), Type.STATEMENT),
            Rule(r("recibimos tu pago"), Type.CARD_PAYMENT, credit = true),
            Rule(r("retiro en cajero|retiro"), Type.WITHDRAWAL, credit = true),
            Rule(r("compra de(?s:.)*aprobada|compra"), Type.PURCHASE, credit = true),
        ),
        "com.mercadopago.wallet" to listOf(
            Rule(r("retiro de efectivo"), Type.WITHDRAWAL),
            Rule(r("te ingresaron|recibiste"), Type.INCOME),
            Rule(r("pagaste|compra"), Type.PURCHASE, credit = true),
        ),
    )

    /** Notices with money in them that are not a movement: promos, codes, reminders, failed attempts. */
    private val ignore = listOf(
        "promocion", "promo ", "cashback", "meses sin intereses", "msi", "codigo", "token", "fecha limite", "por vencer",
        "vence", "pago minimo", "rechazad", "declinad", "no se pudo", "no pudimos", "invierte", "rendimiento", "gana ", "oferta",
        "credito preaprobado", "prestamo disponible", "cupon", "linea de credito", "estado de cuenta", "pago de tarjeta pendiente",
    ).map { it.trim() }

    /**
     * Never a movement, whatever the bank: a declined try, a code, the minimum payment (not what the card owes), an ad.
     * Plain "cashback" is not here: Plata's real purchases mention it.
     */
    private val alwaysIgnore = listOf(
        "rechazad", "declinad", "no se pudo", "no pudimos", "codigo", "token", "pago minimo",
        "promocion", "promo ", "oferta", "cupon", "obten ", "gana ", "meses sin intereses", "credito preaprobado", "prestamo disponible",
    )

    private val incomeWords = listOf(
        "recibiste", "recibid", "te envi", "te transfirio", "te depositaron", "te ingresaron", "deposito", "abono", "nomina",
        "ingreso", "te llego", "llego dinero", "reembolso", "devolucion",
    )

    private val paymentWords = listOf(
        "compra", "pagaste", "pagado", "pago", "cargo", "retiro", "retiraste", "enviaste", "transferiste", "transferencia enviada",
        "transferencia exitosa", "envio de dinero", "domiciliacion", "cobro",
    )

    /** `$1,234.56`, `$ 85`, `MXN 85.00`, `85.00 MXN`. */
    private val amount = Regex("""(?:\$\s?|mxn\s?)(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?|(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?\s?mxn""")

    /** "terminación 1234", "terminada en 1234", "tarjeta *1234", "•••• 1234", "x1234". */
    private val cardDigits = Regex("""(?:terminacion|terminada en|termina en|con terminacion|tarjeta|cuenta|\*+|•+|x{1,4})\s*(?:en\s*)?(\d{4})\b""")

    /** "antes del 23", "es el 24", "hasta el 2 de diciembre", "vence el 26". */
    private val dueDayPattern = Regex("""(?:antes del|es el|hasta el|vence el|vence el dia|pagar el)\s+(\d{1,2})\b""")

    private val merchantAfterEn = Regex("""\b(?:en|a)\s+(?:favor\s+de\s+|cuenta\s+)?([A-Za-z0-9][^$\n,.!]{1,32}?)(?=\s+(?:por|con|el|de\s+\$|aprobada)|[,.!\n]|$)""", RegexOption.IGNORE_CASE)
    /** "Juan Pérez te envió" (Revolut): the sender just before "te envió", after any greeting. */
    private val senderBeforeSent = Regex("""([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ ]{1,40}?)\s+te\s+(?:ha\s+)?(?:enviado|envi[oó]|transferido|transfiri[oó])""", RegexOption.IGNORE_CASE)
    private val senderAfterDe = Regex("""\bde\s+([A-Za-zÀ-ÿ][^$\n,.!]{1,32}?)(?=\s+(?:por|a\s+tu|en\s+tu)|[,.!\n]|$)""", RegexOption.IGNORE_CASE)

    fun parse(pkg: String, title: String, text: String): Parsed? {
        val raw = listOf(title, text).filter { it.isNotBlank() }.joinToString("\n")
        val norm = normalize(raw)
        if (alwaysIgnore.any { it in norm }) return null
        val cents = amountCents(norm) ?: return null
        if (cents <= 0) return null
        val digits = cardDigits.find(norm)?.groupValues?.get(1)

        bankRules[pkg]?.firstOrNull { it.pattern.containsMatchIn(norm) }?.let { rule ->
            val direction = if (rule.type == Type.INCOME) PendingPayment.IN else PendingPayment.OUT
            val dueDay = if (rule.type == Type.STATEMENT) dueDayPattern.find(norm)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..31 } else null
            val merchant = when (rule.type) {
                Type.PURCHASE, Type.WITHDRAWAL -> merchant(raw, PendingPayment.OUT)
                Type.INCOME -> merchant(raw, PendingPayment.IN)
                else -> ""
            }
            return Parsed(cents, direction, merchant, rule.type, digits, credit = rule.credit || rule.type == Type.STATEMENT || rule.type == Type.CARD_PAYMENT, dueDay = dueDay)
        }

        if (ignore.any { it in norm }) return null
        val direction = when {
            incomeWords.any { it in norm } -> PendingPayment.IN
            paymentWords.any { it in norm } -> PendingPayment.OUT
            else -> return null
        }
        val type = when {
            direction == PendingPayment.IN -> Type.INCOME
            "retiro" in norm || "retiraste" in norm -> Type.WITHDRAWAL
            else -> Type.PURCHASE
        }
        return Parsed(cents, direction, merchant(raw, direction), type, digits, credit = "credito" in norm)
    }

    /** Same notice → same id, so a re-posted or updated notification is not added twice. */
    fun stableId(pkg: String, amountCents: Long, postedAt: Long): String =
        UUID.nameUUIDFromBytes("$pkg|$amountCents|${postedAt / 60_000}".toByteArray()).toString()

    internal fun amountCents(norm: String): Long? {
        val m = amount.find(norm) ?: return null
        val whole = (m.groupValues[1].ifEmpty { m.groupValues[3] }).replace(",", "").toLongOrNull() ?: return null
        val dec = m.groupValues[2].ifEmpty { m.groupValues[4] }.padEnd(2, '0').take(2).toLong()
        return whole * 100 + dec
    }

    private fun merchant(raw: String, direction: String): String {
        val match = if (direction == PendingPayment.IN) senderBeforeSent.find(raw) ?: senderAfterDe.find(raw) else merchantAfterEn.find(raw)
        return match?.groupValues?.get(1)?.trim().orEmpty().take(32)
    }

    /** Lowercase without accents, so "Depósito" and "deposito" match the same word. */
    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
}
