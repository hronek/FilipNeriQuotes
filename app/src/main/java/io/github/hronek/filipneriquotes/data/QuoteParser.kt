package io.github.hronek.filipneriquotes.data

import android.content.Context
import java.time.LocalDate

class QuoteParser(private val context: Context) {
    private val lineRegex = Regex("^\\s*(\\d{1,2})/(\\d{1,2})\\.\\s+(.*)$")

    fun loadQuotesFromAsset(assetName: String): Map<Pair<Int, Int>, String> {
        return try {
            context.assets.open(assetName).bufferedReader(Charsets.UTF_8).useLines { seq ->
                val parsed = seq.mapNotNull { parseRaw(it) }.toList()
                // Heuristic to decide orientation: prefer Day/Month (new standard). If ambiguous, detect legacy Month/Day
                val countAgt12 = parsed.count { it.first.first > 12 } // first looks like day>12 => D/M
                val countBgt12 = parsed.count { it.first.second > 12 } // second>12 => M/D legacy
                val dayFirst = when {
                    countAgt12 > 0 && countBgt12 == 0 -> true
                    countBgt12 > 0 && countAgt12 == 0 -> false
                    else -> true // ambiguous or mixed: default to Day/Month
                }
                parsed.mapNotNull { (ab, text) ->
                    val a = ab.first
                    val b = ab.second
                    val month: Int
                    val day: Int
                    if (dayFirst) {
                        day = a; month = b
                    } else {
                        month = a; day = b
                    }
                    // Validate ranges 1..12 for month, 1..31 for day
                    if (month in 1..12 && day in 1..31) (month to day) to text else null
                }.toMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Parses raw numbers only; orientation decided in loadQuotesFromAsset()
    private fun parseRaw(line: String): Pair<Pair<Int, Int>, String>? {
        val m = lineRegex.find(line) ?: return null
        val a = m.groupValues[1].toInt()
        val b = m.groupValues[2].toInt()
        val text = m.groupValues[3].trim()
        return (a to b) to text
    }

    fun findQuoteFor(date: LocalDate, primary: Map<Pair<Int, Int>, String>, fallback: Map<Pair<Int, Int>, String>): String? {
        val key = date.monthValue to date.dayOfMonth
        primary[key]?.let { return it }
        // Fallback to English if primary missing
        fallback[key]?.let { return it }
        return null
    }
}
