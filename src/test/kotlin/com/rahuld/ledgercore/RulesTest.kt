package com.rahuld.ledgercore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RulesTest {
    @Test
    fun `split 10 BHD by 3 gives 3334 3333 3333 and sums exactly`() {
        val total = Money.of("10.000", Currency.BHD)
        val parts = total.split(3)
        assertEquals(
            listOf(
                "3.334",
                "3.333",
                "3.333",
            ),
            parts.map { it.toString() },
        )
        assertEquals(total, parts.reduce { a, b -> a + b })
    }

    @Test
    fun `split with no remainder is even`() {
        val parts = Money.of("9.00", Currency.AED).split(3)
        assertEquals(
            listOf(
                "3.00",
                "3.00",
                "3.00",
            ),
            parts.map { it.toString() },
        )
    }

    private val exactAccruals =
        listOf(
            "0.10",
            "0.10",
            "0.26",
            "0.114",
            "0",
            "0.084",
        ).map { BigDecimal(it) }

    @Test
    fun `cumulative rounding sums to the rounded exact total`() {
        val rounded = cumulativeRound(exactAccruals, 2)
        assertEquals(
            listOf(
                "0.10",
                "0.10",
                "0.26",
                "0.11",
                "0.00",
                "0.09",
            ),
            rounded.map { it.toPlainString() },
        )
        val exactTotal = exactAccruals.reduce(BigDecimal::add).setScale(2, ROUNDING)
        assertEquals(exactTotal, rounded.reduce(BigDecimal::add))
        assertEquals("0.66", exactTotal.toPlainString())
    }

    @Test
    fun `naive per-day rounding loses a minor unit on this series`() {
        val naive = naiveRound(exactAccruals, 2).reduce(BigDecimal::add)
        assertEquals("0.65", naive.toPlainString())
    }

    @Test
    fun `currency mismatch throws`() {
        val aed = Money.of("1.00", Currency.AED)
        val bhd = Money.of("1.000", Currency.BHD)
        assertThrows(IllegalArgumentException::class.java) { aed + bhd }
        assertThrows(IllegalArgumentException::class.java) { aed - bhd }
    }

    @Test
    fun `scale is enforced by the constructor and set by the factory`() {
        assertThrows(IllegalArgumentException::class.java) { Money(BigDecimal("1.0"), Currency.AED) }
        assertEquals("1.00", Money.of("1", Currency.AED).toString())
        assertEquals("0.004", Money.of("0.0040", Currency.BHD).toString())
    }
}
