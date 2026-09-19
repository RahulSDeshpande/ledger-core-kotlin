package com.rahuld.ledgercore

class Ledger(
    accounts: List<Account>,
) {
    val accounts: Map<String, Account> = accounts.associateBy { it.id }

    private val _journal = ArrayList<Entry>()
    val journal: List<Entry> get() = _journal

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
}
