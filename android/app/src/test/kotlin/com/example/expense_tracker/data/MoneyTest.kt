package com.example.expense_tracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesRupeesWithoutFloatingPointRounding() {
        assertEquals(123456L, Money.parseMinorUnits("1234.56"))
        assertEquals(123456L, Money.parseMinorUnits("1,234.56"))
        assertEquals(500L, Money.parseMinorUnits("5"))
    }

    @Test
    fun rejectsInvalidPrecisionAndAmounts() {
        assertNull(Money.parseMinorUnits("1.234"))
        assertNull(Money.parseMinorUnits("1,23"))
        assertNull(Money.parseMinorUnits("0"))
        assertNull(Money.parseMinorUnits("-10"))
        assertNull(Money.parseMinorUnits("₹10"))
    }

    @Test
    fun formatsMinorUnitsAsRupees() {
        assertEquals("₹1,234.56", Money.formatMinorUnits(123_456))
    }
}
