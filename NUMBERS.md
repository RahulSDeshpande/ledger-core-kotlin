# NUMBERS

| Constant | Value | Where | Why this, not half |
|---|---|---|---|
| Overdraft fee | 25.00 AED / 25.000 BHD | `overdraftFee` in Rules.kt | Mandated in AED. Half (12.50) still cascades in this stream: D2 -382.50, D3 +17.50, D4 -360.00, D5 -372.50. Magnitude does not change the logic. BHD value is an assumption, never triggered. |
| Daily rate | 0.0004 | `DAILY_RATE` in Rules.kt | Mandated. Half (0.0002) makes D4 exact 0.057; cumulative rounding still sums exactly, naive rounding would still drift. |
| Rounding mode | HALF_EVEN | `ROUNDING` in Money.kt | Removes upward bias over many accruals. HALF_UP is common in regulation and gives the same figures on this stream. |
| Instalment count | 3 | E10 in Main.kt | From the stream. |
| Window | 6 days | `Day` in Model.kt | From the stream. Half (3) would never exercise back-dating. |
| Over-settle tolerance | unbounded | `settle` in Replay.kt | Scheme rules vary by merchant category (fuel, hotels). A cap is a product rule, not a ledger rule. |
| Hold expiry | EOD Day 6 | `endOfDay` in Replay.kt | Window end. Real schemes: 7 to 30 days by merchant category. |
