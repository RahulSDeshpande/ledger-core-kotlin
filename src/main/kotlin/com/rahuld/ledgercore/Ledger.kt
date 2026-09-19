package com.rahuld.ledgercore

class Ledger(
    accounts: List<Account>,
) {
    val accounts: Map<String, Account> = accounts.associateBy { it.id }

    private val _journal = ArrayList<Entry>()
    val journal: List<Entry> get() = _journal

    private val _auths = LinkedHashMap<String, Authorization>()
    val auths: Map<String, Authorization> get() = _auths

    fun account(id: String): Account = accounts.getValue(id)

    fun post(
        accountId: String,
        kind: EntryKind,
        amount: Money,
        valueDate: Day,
        bookedDay: Day,
        processedDay: Day,
        ref: String,
        memo: String = "",
    ): Entry {
        require(amount.currency == account(accountId).currency)
        val e =
            Entry(
                seq = _journal.size + 1,
                accountId = accountId,
                kind = kind,
                amount = amount,
                valueDate = valueDate,
                bookedDay = bookedDay,
                processedDay = processedDay,
                ref = ref,
                memo = memo,
            )
        _journal.add(e)
        return e
    }

    fun ledger(
        accountId: String,
        asOf: Day,
    ): Money =
        _journal
            .asSequence()
            .filter { it.accountId == accountId && it.valueDate <= asOf }
            .fold(Money.zero(account(accountId).currency)) { acc, e -> acc + e.amount }

    fun holds(accountId: String): Money =
        _auths.values
            .asSequence()
            .filter { it.accountId == accountId && it.active }
            .fold(Money.zero(account(accountId).currency)) { acc, a -> acc + a.amount }

    fun available(
        accountId: String,
        day: Day,
    ): Money = ledger(accountId, day) - holds(accountId)

    // Approved iff available stays >= 0 after the hold. Declined auths are kept so the report can show them.
    fun authorize(
        authId: String,
        accountId: String,
        amount: Money,
        day: Day,
    ): Authorization {
        require(authId !in _auths) { "duplicate auth $authId" }
        val ok = !(available(accountId, day) - amount).isNegative()
        val a =
            Authorization(
                id = authId,
                accountId = accountId,
                amount = amount,
                day = day,
                state = if (ok) AuthState.APPROVED else AuthState.DECLINED,
            )
        _auths[authId] = a
        return a
    }

    fun findActiveAuth(authId: String): Authorization? = _auths[authId]?.takeIf { it.active }

    fun transition(
        authId: String,
        to: AuthState,
    ): Authorization {
        val a = _auths.getValue(authId)
        require(a.active) { "auth $authId is ${a.state}, cannot move to $to" }
        return a.copy(state = to).also { _auths[authId] = it }
    }

    fun activeAuths(): List<Authorization> = _auths.values.filter { it.active }
}
