package com.rahuld.ledgercore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

// FAILS BY DESIGN. Kept red on purpose; excluded from `testCi`, included in `test`.
//
// What it asserts: reversing E7 puts ACC-001 back where it would have been had E7 never happened,
// fees included. What it reveals: the ledger has no fee-reversal semantics. The three 25.00 fees
// were correct when assessed at EOD5 and are append-only, so after E9 the customer is still 75.00
// short. In production that gap is closed by an ops-initiated fee reversal event, which this
// stream and this model do not have. That missing event type is the first item under
// "what you cut" in the architecture document.
@Tag("by-design")
class FailingByDesignTest {
    @Test
    fun `reversing E7 restores the pre-E7 fee position`() {
        val withE7 = replayFixedStream()
        val withoutE7 = Replay(Ledger(ACCOUNTS), STREAM.filter { it.id != "E7" && it.id != "E9" }).run()

        val feesAfterReversal = withE7.ledger.journal.count { it.accountId == "ACC-001" && it.kind == EntryKind.FEE }
        val feesNeverHadE7 = withoutE7.ledger.journal.count { it.accountId == "ACC-001" && it.kind == EntryKind.FEE }
        assertEquals(
            feesNeverHadE7,
            feesAfterReversal,
            "fee count: 0 without E7, 3 after E7+E9",
        )

        assertEquals(
            withoutE7.ledger.ledger("ACC-001", Day(6)).toString(),
            withE7.ledger.ledger("ACC-001", Day(6)).toString(),
            "day 6 ledger: 285.00 without E7, 210.00 after E7+E9 (75.00 of fees survive the reversal)",
        )
    }
}
