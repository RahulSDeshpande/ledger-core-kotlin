package com.rahuld.ledgercore

import java.math.BigDecimal

// rounded[D] = round(sum exact[0..D]) - sum rounded[0..D-1].
// The rounded series then sums to round(sum exact) with no remainder.
fun cumulativeRound(
    exact: List<BigDecimal>,
    scale: Int,
): List<BigDecimal> {
    var exactSum = BigDecimal.ZERO
    var roundedSum = BigDecimal.ZERO.setScale(scale)
    return exact.map { e ->
        exactSum += e
        val r = exactSum.setScale(scale, ROUNDING) - roundedSum
        roundedSum += r
        r
    }
}

fun naiveRound(
    exact: List<BigDecimal>,
    scale: Int,
): List<BigDecimal> = exact.map { it.setScale(scale, ROUNDING) }

val DAILY_RATE: BigDecimal = BigDecimal("0.0004")

fun overdraftFee(currency: Currency): Money = Money.of("25", currency)

// Exact accrual keeps full precision; rounding happens only in the cumulative schedule.
fun exactAccrual(closing: Money): BigDecimal = (if (closing.isNegative()) BigDecimal.ZERO else closing.amount) * DAILY_RATE

class InterestSchedule(
    val currency: Currency,
) {
    private val exacts = LinkedHashMap<Day, BigDecimal>()
    val days: List<Day> get() = exacts.keys.toList()
    val exact: List<BigDecimal> get() = exacts.values.toList()

    fun accrue(
        day: Day,
        closing: Money,
    ) {
        exacts[day] = exactAccrual(closing)
    }

    fun rounded(): List<BigDecimal> = cumulativeRound(exact, currency.scale)

    fun total(): Money = Money(rounded().fold(BigDecimal.ZERO.setScale(currency.scale)) { s, r -> s + r }, currency)
}
