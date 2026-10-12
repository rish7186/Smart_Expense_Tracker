package com.example.expense_tracker.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object Money {
    private val amountPattern = Regex("""(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d{1,2})?""")

    fun parseMinorUnits(input: String): Long? {
        val normalized = input.trim()
        if (!amountPattern.matches(normalized)) return null

        return try {
            BigDecimal(normalized.replace(",", ""))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
                .takeIf { it > 0 }
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun formatMinorUnits(amountMinor: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
            currency = Currency.getInstance("INR")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(BigDecimal.valueOf(amountMinor, 2))
    }
}
