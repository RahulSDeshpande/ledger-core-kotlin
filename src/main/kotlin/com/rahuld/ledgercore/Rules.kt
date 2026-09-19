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
