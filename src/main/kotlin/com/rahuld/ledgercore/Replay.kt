package com.rahuld.ledgercore

class AuthLine(
    val accountId: String,
    val authId: String,
    val state: AuthState,
    val amount: Money,
    val available: Money?,
)

class AccountDay(
    val accountId: String,
    val currency: Currency,
    val closing: Money,
    val available: Money,
    val holds: Money,
    val fees: List<Entry>,
    val auths: List<AuthLine>,
    val errors: List<Flag>,
    val restated: List<Pair<Day, Money>>,
)

class DayReport(
    val day: Day,
    val accounts: List<AccountDay>,
)

class Replay(
    val ledger: Ledger,
    private val events: List<Event>,
) {
    private var closedThrough = 0
    private val openDay get() = Day(closedThrough + 1)

    // Closing as printed at each EOD. Report/test record only; no balance query reads it.
    val closings = LinkedHashMap<Day, Map<String, Money>>()

    private val authLines = ArrayList<Pair<Day, AuthLine>>()
    private val flags = ArrayList<Pair<Day, Flag>>()

    val days = ArrayList<DayReport>()

    fun run(): Replay {
        for (a in ledger.accounts.values) {
            ledger.post(
                accountId = a.id,
                kind = EntryKind.CREDIT,
                amount = Money.zero(a.currency),
                valueDate = Day(1),
                bookedDay = Day(1),
                processedDay = Day(1),
                ref = "OPEN",
                memo = "opening balance",
            )
        }
        for (e in events) {
            closeThrough(e.bookedDay.n - 1)
            apply(e)
        }
        closeThrough(6)
        return this
    }

    private fun closeThrough(lastDay: Int) {
        while (closedThrough < lastDay) {
            closedThrough++
            endOfDay(Day(closedThrough))
        }
    }

    // bookedDay is what the event states; processedDay is the day open when it arrived (E10: 5 vs 6).
    private fun post(
        e: Event,
        kind: EntryKind,
        amount: Money,
        valueDate: Day = e.valueDate,
        memo: String = "",
    ): Entry =
        ledger.post(
            accountId = e.accountId,
            kind = kind,
            amount = amount,
            valueDate = valueDate,
            bookedDay = e.bookedDay,
            processedDay = openDay,
            ref = e.id,
            memo = memo,
        )

    private fun apply(e: Event) {
        when (e) {
            is Event.Credit ->
                post(
                    e = e,
                    kind = EntryKind.CREDIT,
                    amount = e.amount,
                )
            is Event.Debit ->
                post(
                    e = e,
                    kind = EntryKind.DEBIT,
                    amount = -e.amount,
                )
            is Event.Auth -> authorize(e)
            is Event.Settlement -> settle(e)
            is Event.Reversal -> {
                val original = ledger.journal.first { it.ref == e.reverses && it.accountId == e.accountId }
                post(
                    e = e,
                    kind = EntryKind.REVERSAL,
                    amount = -original.amount,
                    valueDate = original.valueDate,
                    memo = "reverses ${e.reverses}",
                )
            }
            is Event.InstalmentCredit ->
                e.amount.split(e.parts).forEachIndexed { i, part ->
                    post(
                        e = e,
                        kind = EntryKind.CREDIT,
                        amount = part,
                        memo = "instalment ${i + 1}/${e.parts}",
                    )
                }
        }
    }

    private fun authorize(e: Event.Auth) {
        val before = ledger.available(e.accountId, e.valueDate)
        val a =
            ledger.authorize(
                authId = e.authId,
                accountId = e.accountId,
                amount = e.amount,
                day = e.valueDate,
            )
        // Available after the decision: the hold applies only when approved.
        authLine(a, if (a.active) before - a.amount else before)
    }

    // An unmatched settlement is still posted: the scheme has already paid the merchant. See AMBIGUITIES #10.
    private fun settle(e: Event.Settlement) {
        if (ledger.findActiveAuth(e.authId) == null) {
            post(
                e = e,
                kind = EntryKind.SETTLEMENT,
                amount = -e.amount,
                memo = "UNMATCHED_AUTH",
            )
            flags += openDay to
                Flag(
                    eventId = e.id,
                    accountId = e.accountId,
                    reason = "${e.authId} settlement ${e.amount} has no authorization — posted, flagged UNMATCHED_AUTH",
                )
            return
        }
        post(
            e = e,
            kind = EntryKind.SETTLEMENT,
            amount = -e.amount,
            memo = "settles ${e.authId}",
        )
        authLine(ledger.transition(e.authId, AuthState.SETTLED), amount = e.amount)
    }

    private fun authLine(
        a: Authorization,
        available: Money? = null,
        amount: Money = a.amount,
    ) {
        authLines += openDay to
            AuthLine(
                accountId = a.accountId,
                authId = a.id,
                state = a.state,
                amount = amount,
                available = available,
            )
    }

    private fun endOfDay(n: Day) {
        val closing = ledger.accounts.keys.associateWith { ledger.ledger(it, n) }
        closings[n] = closing
        days += DayReport(n, ledger.accounts.values.map { accountDay(it, n) })
        if (n.n == 6) {
            for (a in ledger.activeAuths()) authLine(ledger.transition(a.id, AuthState.EXPIRED))
        }
    }

    private fun accountDay(
        a: Account,
        n: Day,
    ): AccountDay {
        val closing = closings.getValue(n).getValue(a.id)
        val holds = ledger.holds(a.id)
        return AccountDay(
            accountId = a.id,
            currency = a.currency,
            closing = closing,
            available = closing - holds,
            holds = holds,
            fees = ledger.journal.filter { it.accountId == a.id && it.kind == EntryKind.FEE && it.processedDay == n },
            auths = authLines.filter { (d, l) -> d == n && l.accountId == a.id }.map { it.second },
            errors = flags.filter { (d, f) -> d == n && f.accountId == a.id }.map { it.second },
            restated =
                closings
                    .filter { (d, _) -> d < n }
                    .map { (d, printed) -> d to ledger.ledger(a.id, d) to printed.getValue(a.id) }
                    .filter { (now, printed) -> now.second != printed }
                    .map { (now, _) -> now },
        )
    }
}
