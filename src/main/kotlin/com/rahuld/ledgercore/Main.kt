package com.rahuld.ledgercore

import com.rahuld.ledgercore.Currency.AED
import com.rahuld.ledgercore.Currency.BHD

val ACCOUNTS = listOf(Account("ACC-001", AED), Account("ACC-002", BHD))

private fun aed(s: String) = Money.of(s, AED)

private fun bhd(s: String) = Money.of(s, BHD)

// Stream order is the input order. E10 is booked Day 5 but arrives after E9 (Day 6).
val STREAM: List<Event> =
    listOf(
        Event.Credit(
            id = "E1",
            bookedDay = Day(1),
            accountId = "ACC-001",
            amount = aed("1200.00"),
            valueDate = Day(1),
        ),
        Event.Debit(
            id = "E2",
            bookedDay = Day(1),
            accountId = "ACC-001",
            amount = aed("950.00"),
            valueDate = Day(1),
        ),
        Event.Auth(
            id = "E3",
            bookedDay = Day(2),
            accountId = "ACC-001",
            authId = "Auth-A",
            amount = aed("200.00"),
            valueDate = Day(2),
        ),
        Event.Credit(
            id = "E4",
            bookedDay = Day(3),
            accountId = "ACC-001",
            amount = aed("400.00"),
            valueDate = Day(3),
        ),
        Event.Settlement(
            id = "E5",
            bookedDay = Day(4),
            accountId = "ACC-001",
            authId = "Auth-A",
            amount = aed("185.00"),
            valueDate = Day(4),
        ),
        Event.Settlement(
            id = "E6",
            bookedDay = Day(4),
            accountId = "ACC-001",
            authId = "Auth-Z",
            amount = aed("180.00"),
            valueDate = Day(4),
        ),
        Event.Debit(
            id = "E7",
            bookedDay = Day(5),
            accountId = "ACC-001",
            amount = aed("620.00"),
            valueDate = Day(2),
        ),
        Event.Auth(
            id = "E8",
            bookedDay = Day(5),
            accountId = "ACC-001",
            authId = "Auth-B",
            amount = aed("90.00"),
            valueDate = Day(5),
        ),
        Event.Reversal(
            id = "E9",
            bookedDay = Day(6),
            accountId = "ACC-001",
            reverses = "E7",
            valueDate = Day(2),
        ),
        Event.InstalmentCredit(
            id = "E10",
            bookedDay = Day(5),
            accountId = "ACC-002",
            amount = bhd("10.000"),
            parts = 3,
            valueDate = Day(5),
        ),
    )

fun replayFixedStream(): Replay = Replay(Ledger(ACCOUNTS), STREAM).run()

fun main() {
    print(render(replayFixedStream()))
}
