package com.rahuld.ledgercore

fun Money.w(): String = "%9s".format(amount.toPlainString())

fun render(replay: Replay): String =
    buildString {
        for (d in replay.days) {
            appendLine("== Day ${d.day.n} ==")
            for (a in d.accounts) {
                appendLine("${a.accountId} ${a.currency}  closing ${a.closing.w()}  available ${a.available.w()}  holds ${a.holds.w()}")
                appendLine(
                    "  fees:     " +
                        a.fees
                            .joinToString(" | ") { "FEE ${-it.amount} vd ${it.valueDate} (booked ${it.bookedDay})" }
                            .ifEmpty { "(none)" },
                )
                appendLine(
                    "  auths:    " +
                        a.auths
                            .joinToString(" | ") { l ->
                                "${l.authId} ${l.state} ${l.amount}" + (l.available?.let { " (available $it)" } ?: "")
                            }.ifEmpty { "(none)" },
                )
                appendLine(
                    "  errors:   " +
                        a.errors
                            .joinToString(" | ") { "${it.eventId} ${it.reason}" }
                            .ifEmpty { "(none)" },
                )
                if (a.restated.isNotEmpty()) appendLine("  restated: " + a.restated.joinToString(", ") { (day, m) -> "$day $m" })
            }
        }
        appendLine("== Interest ==")
        val f = replay.final
        for ((id, s) in f.interest) {
            val ccy = replay.ledger.account(id).currency
            appendLine("$id $ccy")
            val rounded = s.rounded()
            s.days.forEachIndexed { i, day ->
                val exact = s.exact[i].setScale(ccy.scale + 2).toPlainString()
                appendLine("  $day exact $exact  rounded ${rounded[i].toPlainString()}")
            }
            appendLine("  capitalized ${f.capitalized.getValue(id).amount}  final ${f.finalBalances.getValue(id)}")
        }
    }
