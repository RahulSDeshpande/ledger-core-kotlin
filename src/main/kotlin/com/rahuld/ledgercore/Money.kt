package com.rahuld.ledgercore

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

val ROUNDING: RoundingMode = RoundingMode.HALF_EVEN

enum class Currency(
    val scale: Int,
) {
    AED(2),
    BHD(3),
}

class Money(
    val amount: BigDecimal,
    val currency: Currency,
) {
    init {
        require(amount.scale() == currency.scale) {
            "scale ${amount.scale()} does not match ${currency.name} scale ${currency.scale}"
        }
    }

    companion object {
        fun of(
            text: String,
            currency: Currency,
        ): Money = Money(BigDecimal(text).setScale(currency.scale, ROUNDING), currency)

        fun zero(currency: Currency): Money = of("0", currency)
    }

    private fun same(other: Money) = require(currency == other.currency) { "currency mismatch: $currency vs ${other.currency}" }

    operator fun plus(other: Money): Money {
        same(other)
        return Money(amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        same(other)
        return Money(amount - other.amount, currency)
    }

    operator fun unaryMinus(): Money = Money(amount.negate(), currency)

    fun isNegative(): Boolean = amount.signum() < 0

    // Largest-remainder split in minor units: the first `total mod n` parts carry one extra unit.
    fun split(n: Int): List<Money> {
        require(n > 0)
        val (q, r) = amount.unscaledValue().divideAndRemainder(BigInteger.valueOf(n.toLong()))
        return List(n) { i ->
            val units = if (i < r.toInt()) q + BigInteger.ONE else q
            Money(BigDecimal(units, currency.scale), currency)
        }
    }

    override fun equals(other: Any?) = other is Money && currency == other.currency && amount == other.amount

    override fun hashCode() = 31 * amount.hashCode() + currency.hashCode()

    override fun toString() = amount.toPlainString()
}
