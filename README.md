# ledger-core

In-memory account ledger. Plain Kotlin/JVM, Gradle, JUnit 5. No framework, no persistence.

## Run

```
./gradlew run       # replays the fixed stream, prints Day 1..6 and the interest block
./gradlew test      # full suite; FailingByDesignTest fails on purpose (below), so `build` is red too
./gradlew testCi    # same suite without tag "by-design"; this is the green check
./gradlew ktlintCheck   # formatting gate: ktlint 1.5.0, ktlint_official style
```

## Reading the output

```
== Day 5 ==
ACC-001 AED  closing   -410.00  available   -410.00  holds      0.00
  fees:     FEE 25.00 vd D2 (booked D5) | ...
  auths:    Auth-B DECLINED 90.00 (available -335.00)
  errors:   (none)
  restated: D2 -395.00, D3 5.00, D4 -385.00
```

| Line | Meaning |
|---|---|
| `closing` | sum of entries with value_date <= this day, as known at this EOD |
| `available` | closing minus active holds |
| `fees` | fees booked at this EOD; `vd` is the day the fee is value-dated to |
| `auths` | authorization events of this day; `available` is the balance after the decision |
| `errors` | flags raised this day; nothing in the stream is rejected |
| `restated` | closed days whose ledger now differs from the closing printed for them |

The interest block lists, per account, each day's exact accrual, the cumulatively rounded accrual, the capitalized total, and the final balance. Day 6 `closing` is printed before capitalization; `final` is after.

## The failing test

`FailingByDesignTest` asserts that reversing E7 restores the fee position from before E7. It fails: the three 25.00 fees were correct when assessed and the journal is append-only, so the customer stays 75.00 short after E9. The model has no fee-reversal event. The test is the evidence for that gap. It carries `@Tag("by-design")`; `test` runs it, `testCi` skips it.

## Design

`Ledger` holds one growing `List<Entry>` and a map of authorizations. `ledger(account, day)` and `available(account, day)` fold the journal on every call; nothing is cached. `Replay` walks events in stream order, opens the next day when an event's booked day is later than the open day, and runs end-of-day for each day it skips: fee sweep over every unassessed day in ascending order, interest accrual on the balance as known, record the day, and on Day 6 capitalize interest and expire holds. Back-dated and late events post normally; the `restated` line shows what they changed. `Report` only formats what `Replay` recorded.

Built with Claude Code as pair. Every decision in AMBIGUITIES.md and REJECTED.md is reviewed and owned by the candidate.
