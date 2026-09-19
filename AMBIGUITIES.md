# AMBIGUITIES

Format: Ambiguity / Options / Chosen / Why / Where.

1. Fee value_date when a day is found negative later. / Day evaluated, or day of discovery. / Day evaluated. / Criteria 1 and 2 both speak of Day 2 as the day the fee lands. / `feeSweep`, Replay.kt.
2. Do fees cascade. / A fee value-dated D2 counts toward D3+, or is excluded. / Counts. / The rule defines closing as all entries with value_date <= day; a fee is an entry. D2 fee -> D4 -360 -> fee -> D5 -385 -> fee. / `feeSweep`.
3. Fee when the causing entry is reversed. / Reverse fee, or keep it. / Keep. / Append-only; the fee was correct when booked. Undoing it needs a fee-reversal event that the stream lacks. / `FailingByDesignTest`.
4. Interest basis: balance as known at EOD, or restated later. / As-known, or re-accrue. / As-known. / Re-accrual needs correction entries and re-opens closed days. ACC-002 would be 0.008 under restatement, is 0.004 here. / `endOfDay`.
5. E10 booked Day 5, arrives after Day 6 events. / Sort by booked day, reject, or accept late. / Accept, processed in stream order. / Stream order is the input contract; `processedDay` records when it landed. / `Replay.run`.
6. E10 instalment value dates. / All D5, or D5/D6/D7. / All D5. / Brief gives one value_date; D7 is outside the window. / `apply`, InstalmentCredit.
7. Overdraft fee on the BHD account. / 25.000 BHD, converted AED, or none. / 25.000 BHD. / Only AED is specified; never triggered. / `overdraftFee`.
8. Settlement below hold. / Release the whole hold, or keep the residual. / Release all. / Single presentment per auth in this stream; residual holds need an expiry rule per scheme. / `settle`.
9. Settlement above hold. / Cap at hold, tolerance, or unbounded. / Unbounded. / The hold is an estimate, not a limit. / `settle`, NUMBERS.md.
10. Settlement without authorization (E6). / Reject, or post and flag. / Post and flag. / The scheme has already paid the merchant; refusing creates an unreconciled position. Criterion 4 refused. / `settle`.
11. Auth approval reads balance as of the auth's day, or as of everything known now. / As of the auth's day, with all entries known at processing time. / Current knowledge is the only honest view: E7 is already posted when E8 arrives, so Auth-B sees -335.00. / `authorize`, Replay.kt.
12. Declined auth is an error or a business outcome. / Errors line, or auths line. / Auths line. / The system worked; the customer had no funds. / Report.kt.
13. Reversal value_date. / Original's value_date, or the reversal's booked day. / Original's. / Brief states value_date Day 2 for E9. / `Event.Reversal` in `apply`.
14. Rounding mode. / HALF_EVEN or HALF_UP. / HALF_EVEN. / No cumulative sum in this stream lands on a half unit, so both give 0.66; documented for the general case. / `ROUNDING`.
15. Day 6 interest capitalized same day. / Same day before expiry, or after the window. / Same day, before hold expiry. / Brief: capitalize at end of Day 6. / `endOfDay`.
16. "Once per day per account". / Per value date, or per booking day. / Per value date. / Three fees booked on Day 5 for three different value dates; one per booking day would drop two of them. / `feeSweep`, Replay.kt.
17. Day 6 closing with or without the capitalization credit. / Include, or print before. / Print before, show `final` after. / The accrual basis cannot contain its own result. / `endOfDay`, Report.kt.
18. "Three equal instalments" when 10.000 is not divisible by 3. / Round each to 3.334 and discard the mismatch, or largest remainder. / Largest remainder, first instalment carries the extra unit. / Sum must be exact; criterion 7 refused. / `Money.split`.
19. Which days count as restated. / Only the back-dated day, or every closed day whose ledger changed. / Every changed day. / E7 at vd D2 also moves D3 (650 -> 5) and D4; listing only D2 hides two of the three. / `accountDay`.
20. Reversal entry `ref`. / The reversed event (E7), or the reversal's own id (E9). / E9, with `memo = "reverses E7"`. / `ref` is the event id on every entry; putting E7 there would make E9 disappear from the journal. / `Event.Reversal` in `apply`.
21. `available` printed on an auth line. / Before the hold, or after the decision. / After the decision. / Approved shows the post-hold figure (50.00); declined creates no hold, so it shows the unchanged figure (-335.00). First run printed -425.00 for Auth-B, changed. / `authorize`, Replay.kt.
22. Opening balance. / A field, or an entry of zero. / Entry of zero, ref `OPEN`. / One code path for balances; the journal is the only state. / `Replay.run`.
23. Interest entry when the total is zero. / Skip, or post 0. / Post. / One INTEREST entry per account, always; the report reads it unconditionally. / `endOfDay`.
24. `AuthState.RELEASED` is never reached in this stream. / Drop it, or keep it. / Keep. / Settlement moves the auth to SETTLED and expiry to EXPIRED; RELEASED is the state a merchant-initiated auth reversal would produce, which is a Part 2 lifecycle end and needs no event here. / `AuthState`, Model.kt.
25. "Once per day" fee guard: a separate set, or the journal itself. / Set of (account, day), or `journal.any { FEE && valueDate == day }`. / Journal. / The append-only log already holds the fact; a second structure could drift from it. / `feeSweep`, Replay.kt.
