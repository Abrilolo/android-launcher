package com.adrian.minimallauncher

import androidx.compose.ui.text.AnnotatedString
import com.adrian.minimallauncher.feature.finance.PesosTransformation
import com.adrian.minimallauncher.feature.finance.amountInput
import com.adrian.minimallauncher.feature.finance.formatPesos
import com.adrian.minimallauncher.feature.finance.wholePesos
import org.junit.Assert.assertEquals
import org.junit.Test

class PesosFormatTest {
    @Test fun formatsWithCommas() {
        assertEquals("$0", formatPesos(0))
        assertEquals("$999", formatPesos(99_900))
        assertEquals("$1,000", formatPesos(100_000))
        assertEquals("$1,234,567.50", formatPesos(123_456_750))
        assertEquals("-$12,000", wholePesos(-1_200_000))
    }

    @Test fun keepsOnePointAndTwoDecimals() {
        assertEquals("1234.56", amountInput("1,234.567"))
        assertEquals("12.34", amountInput("12.3.4"))
        assertEquals("12.5", amountInput("12,5"))
    }

    @Test fun showsMoneyWhileTyping() {
        val shown = PesosTransformation.filter(AnnotatedString("1234567.5"))
        assertEquals("$1,234,567.5", shown.text.text)
        assertEquals(1, shown.offsetMapping.originalToTransformed(0))
        assertEquals(12, shown.offsetMapping.originalToTransformed(9))
        assertEquals(9, shown.offsetMapping.transformedToOriginal(12))
        assertEquals(0, shown.offsetMapping.transformedToOriginal(0))
        assertEquals("", PesosTransformation.filter(AnnotatedString("")).text.text)
    }
}
