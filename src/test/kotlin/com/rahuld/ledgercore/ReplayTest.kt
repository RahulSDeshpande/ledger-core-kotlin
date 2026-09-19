package com.rahuld.ledgercore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReplayTest {
    private lateinit var replay: Replay
    private val acc1 = "ACC-001"
    private val acc2 = "ACC-002"

    @BeforeAll
    fun replayStream() {
        replay = replayFixedStream()
    }

    private fun day(
        n: Int,
        id: String,
    ): AccountDay = replay.days[n - 1].accounts.first { it.accountId == id }

    private fun assertDay(
        n: Int,
        id: String,
        closing: String,
        available: String,
        holds: String,
        fees: List<String> = emptyList(),
    ) {
        val d = day(n, id)
        assertEquals(
            closing,
            d.closing.toString(),
            "D$n $id closing",
        )
        assertEquals(
            available,
            d.available.toString(),
            "D$n $id available",
        )
        assertEquals(
            holds,
            d.holds.toString(),
            "D$n $id holds",
        )
        assertEquals(
            fees,
            d.fees.map { "${it.valueDate}" },
            "D$n $id fee value dates",
        )
    }

    @Test
    fun `day 1 closing 250`() {
        assertDay(
            n = 1,
            id = acc1,
            closing = "250.00",
            available = "250.00",
            holds = "0.00",
        )
        assertDay(
            n = 1,
            id = acc2,
            closing = "0.000",
            available = "0.000",
            holds = "0.000",
        )
    }

    @Test
    fun `day 2 auth A approved with available 50`() {
        assertDay(
            n = 2,
            id = acc1,
            closing = "250.00",
            available = "50.00",
            holds = "200.00",
        )
        val a = day(2, acc1).auths.single()
        assertEquals("Auth-A", a.authId)
        assertEquals(AuthState.APPROVED, a.state)
        assertEquals("50.00", a.available.toString())
    }

    @Test
    fun `day 3 closing 650`() {
        assertDay(
            n = 3,
            id = acc1,
            closing = "650.00",
            available = "450.00",
            holds = "200.00",
        )
    }

    @Test
    fun `day 4 auth A settled for 185 and auth Z posted with flag`() {
        assertDay(
            n = 4,
            id = acc1,
            closing = "285.00",
            available = "285.00",
            holds = "0.00",
        )
        val d = day(4, acc1)
        assertEquals(listOf("Auth-A" to AuthState.SETTLED), d.auths.map { it.authId to it.state })
        assertEquals("E6", d.errors.single().eventId)
        assertEquals(
            AuthState.SETTLED,
            replay.ledger.auths
                .getValue("Auth-A")
                .state,
        )
        assertNull(replay.ledger.auths["Auth-Z"])
        assertEquals(
            "UNMATCHED_AUTH",
            replay.ledger.journal
                .first { it.ref == "E6" }
                .memo,
        )
    }

    @Test
    fun `day 5 three fees cascade and auth B declined`() {
        assertDay(
            n = 5,
            id = acc1,
            closing = "-410.00",
            available = "-410.00",
            holds = "0.00",
            fees =
                listOf(
                    "D2",
                    "D4",
                    "D5",
                ),
        )
        val d = day(5, acc1)
        assertTrue(d.fees.all { it.bookedDay == Day(5) && it.processedDay == Day(5) })
        val b = d.auths.single()
        assertEquals(AuthState.DECLINED, b.state)
        assertEquals("-335.00", b.available.toString())
        assertEquals("0.00", d.holds.toString())
        assertDay(
            n = 5,
            id = acc2,
            closing = "0.000",
            available = "0.000",
            holds = "0.000",
        )
    }

    @Test
    fun `day 6 reversal restates without new fees`() {
        assertDay(
            n = 6,
            id = acc1,
            closing = "210.00",
            available = "210.00",
            holds = "0.00",
        )
        assertDay(
            n = 6,
            id = acc2,
            closing = "10.000",
            available = "10.000",
            holds = "0.000",
        )
        assertTrue(day(6, acc1).errors.isEmpty())
    }

    @Test
    fun `criterion 1 day 2 before fees is minus 370`() {
        val beforeFees =
            replay.ledger.journal
                .filter { it.accountId == acc1 && it.valueDate <= Day(2) && it.kind != EntryKind.FEE && it.kind != EntryKind.REVERSAL }
                .fold(Money.zero(Currency.AED)) { s, e -> s + e.amount }
        assertEquals("-370.00", beforeFees.toString())
    }

    @Test
    fun `restatements at day 5 and day 6`() {
        assertEquals(
            listOf(
                "D2 -395.00",
                "D3 5.00",
                "D4 -385.00",
            ),
            day(5, acc1).restated.map { (d, m) -> "$d $m" },
        )
        assertEquals(
            listOf(
                "D2 225.00",
                "D3 625.00",
                "D4 235.00",
                "D5 210.00",
            ),
            day(6, acc1).restated.map { (d, m) -> "$d $m" },
        )
        assertEquals(listOf("D5 10.000"), day(6, acc2).restated.map { (d, m) -> "$d $m" })
        assertTrue((1..4).all { day(it, acc1).restated.isEmpty() })
    }

    @Test
    fun `late E10 keeps booked day 5 and processed day 6`() {
        val parts = replay.ledger.journal.filter { it.ref == "E10" }
        assertEquals(
            listOf(
                "3.334",
                "3.333",
                "3.333",
            ),
            parts.map { it.amount.toString() },
        )
        assertTrue(parts.all { it.bookedDay == Day(5) && it.processedDay == Day(6) && it.valueDate == Day(5) })
    }

    @Test
    fun `reversal carries original value date and E7 fees stay booked`() {
        val rev = replay.ledger.journal.single { it.ref == "E9" }
        assertEquals(EntryKind.REVERSAL, rev.kind)
        assertEquals(Day(2), rev.valueDate)
        assertEquals("620.00", rev.amount.toString())
        assertEquals(3, replay.ledger.journal.count { it.accountId == acc1 && it.kind == EntryKind.FEE })
    }

    @Test
    fun `interest schedule and final balances after capitalization`() {
        assertEquals(
            listOf(
                "0.10",
                "0.10",
                "0.26",
                "0.11",
                "0.00",
                "0.09",
            ),
            replay.interest
                .getValue(acc1)
                .rounded()
                .map { it.toPlainString() },
        )
        assertEquals(
            "0.66",
            replay.final.capitalized
                .getValue(acc1)
                .amount
                .toString(),
        )
        assertEquals(
            "210.66",
            replay.final.finalBalances
                .getValue(acc1)
                .toString(),
        )
        assertEquals(
            "0.004",
            replay.final.capitalized
                .getValue(acc2)
                .amount
                .toString(),
        )
        assertEquals(
            "10.004",
            replay.final.finalBalances
                .getValue(acc2)
                .toString(),
        )
        assertEquals(1, replay.ledger.journal.count { it.accountId == acc1 && it.kind == EntryKind.INTEREST })
        assertTrue(replay.ledger.activeAuths().isEmpty())
    }
}
