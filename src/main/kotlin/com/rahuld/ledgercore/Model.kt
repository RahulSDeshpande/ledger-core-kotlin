package com.rahuld.ledgercore

@JvmInline
value class Day(
    val n: Int,
) : Comparable<Day> {
    init {
        require(n in 1..6) { "day $n outside window 1..6" }
    }

    override fun compareTo(other: Day) = n.compareTo(other.n)

    override fun toString() = "D$n"
}

class Account(
    val id: String,
    val currency: Currency,
)

enum class EntryKind { CREDIT, DEBIT, SETTLEMENT, FEE, INTEREST, REVERSAL }

class Entry(
    val seq: Int,
    val accountId: String,
    val kind: EntryKind,
    val amount: Money, // signed: debits negative
    val valueDate: Day,
    val bookedDay: Day, // what the event states
    val processedDay: Day, // open day when replay posted it; differs for late events (E10)
    val ref: String, // event id
    val memo: String = "",
)

enum class AuthState { APPROVED, DECLINED, SETTLED, RELEASED, EXPIRED }

data class Authorization(
    val id: String,
    val accountId: String,
    val amount: Money,
    val day: Day,
    val state: AuthState,
) {
    val active get() = state == AuthState.APPROVED
}

sealed interface Event {
    val id: String
    val bookedDay: Day
    val accountId: String
    val valueDate: Day

    data class Credit(
        override val id: String,
        override val bookedDay: Day,
        override val accountId: String,
        val amount: Money,
        override val valueDate: Day,
    ) : Event

    data class Debit(
        override val id: String,
        override val bookedDay: Day,
        override val accountId: String,
        val amount: Money,
        override val valueDate: Day,
    ) : Event

    data class Auth(
        override val id: String,
        override val bookedDay: Day,
        override val accountId: String,
        val authId: String,
        val amount: Money,
        override val valueDate: Day,
    ) : Event

    data class Settlement(
        override val id: String,
        override val bookedDay: Day,
        override val accountId: String,
        val authId: String,
        val amount: Money,
        override val valueDate: Day,
    ) : Event

    data class Reversal(
        override val id: String,
        override val bookedDay: Day,
        override val accountId: String,
        val reverses: String,
        override val valueDate: Day,
    ) : Event

    data class InstalmentCredit(
        override val id: String,
        override val bookedDay: Day,
        override val accountId: String,
        val amount: Money,
        val parts: Int,
        override val valueDate: Day,
    ) : Event
}

class Flag(
    val eventId: String,
    val accountId: String,
    val reason: String,
)
