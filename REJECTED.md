# REJECTED

## Criteria refused

**2. "E7 causes exactly one overdraft fee, on Day 2."** E7 is a 620.00 debit value-dated Day 2. At EOD5 the sweep finds Day 2 at -370.00, books a fee, and Day 2 closes at -395.00. Day 3 is then +5.00 (400.00 credit), Day 4 is -360.00 (185.00 and 180.00 settlements), fee, -385.00. Day 5 carries -385.00, fee, -410.00. The rule is once per day per account; three days are negative, three fees.

**4. "A settlement with an unknown authorization must be rejected and the funds must not leave the account."** In card schemes a clearing arrives after the merchant has been paid by the scheme. If the bank declines to post it, the money has still left at scheme level and the ledger no longer reconciles with the settlement file. E6 is posted as a 180.00 SETTLEMENT with memo `UNMATCHED_AUTH` and a flag under errors. Day 4 closes at 285.00, not 465.00. The opposite choice is implementable in one branch of `settle`; it was not taken.

**6. "After E9, all balances and fees return to their pre-E7 values."** E9 posts +620.00 value-dated Day 2. The three fees stay. Day 2 is 225.00, not 250.00; Day 6 is 210.00, not 285.00. The difference is exactly 75.00 of fees that were correct when assessed. The journal is append-only; restoring history would need a fee-reversal event. `FailingByDesignTest` asserts this criterion and fails.

**7. "The three BHD instalments must each be 3.334."** 3 x 3.334 = 10.002. The stream says 10.000. The split is 3.334, 3.333, 3.333, computed in minor units with the remainder on the first instalment.

**8. "If the rounded accruals do not sum to the capitalized total, the remainder is discarded."** The non-negotiable rule says they must sum exactly. Cumulative rounding (rounded_D = round(sum exact through D) - sum rounded before D) makes the sum exact by construction: 0.10 + 0.10 + 0.26 + 0.11 + 0.00 + 0.09 = 0.66 = round(0.658). Naive per-day rounding gives 0.65 and would need the discarded remainder the criterion describes.

**Rule tension, implemented as specified.** Daily interest on a positive balance and a fixed penalty fee on an overdraft are both non-Sharia constructs. The brief mandates them and this ledger books them. The production equivalents at a UAE Islamic bank are profit-share distribution from a Mudarabah pool and a cost-recovery charge on late payment with proceeds to charity.

## Approaches abandoned mid-build

Only changes that were actually made and undone during this build are listed. Timestamps refer to WORKLOG.md.

1. **Capitalize interest before printing Day 6** (spec revision, before code). The PRD's EOD order printed Day 6 after capitalization, which would show 210.66 as the closing while the accrual for Day 6 was computed on 210.00. Changed to print first; `final` carries the capitalized figure. Ambiguity 17.

2. **Auth line shows available after the hold regardless of decision** (WORKLOG, ReplayTest entry). First run printed `Auth-B DECLINED 90.00 (available -425.00)`: the requested hold subtracted from a balance it never touched. Changed so a declined auth shows the unchanged figure, -335.00. Ambiguity 21.

3. **Reversal entry with `ref = E7`** (spec revision). The PRD wanted the reversal entry to reference the reversed event. Kept `ref` as the entry's own event id (E9) and moved E7 to `memo`, so every entry in the journal still maps to exactly one stream event. Ambiguity 20.

4. **Separate set of fee-assessed (account, day) pairs** (WORKLOG, simplification entry). Worked, but duplicated a fact the journal already holds: a FEE entry value-dated D. Replaced by a journal lookup in `feeSweep`. Ambiguity 25.

5. **One accrual record per day, re-rounded on every accrual** (same entry). `InterestSchedule` kept an `AccrualLine(day, exact, rounded)` and reran cumulative rounding over the full list each day. Replaced by storing exacts only and rounding once on read. Same figures, one place for the formula.

Not on this list because they were never started: cached balances, sorting events by booked day, holds as entries, naive per-day rounding. Each was excluded in the spec before the first commit; the last one exists in code only as `naiveRound`, the counter-example in RulesTest.
