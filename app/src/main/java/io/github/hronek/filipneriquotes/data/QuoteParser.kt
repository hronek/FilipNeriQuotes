package io.github.hronek.filipneriquotes.data

import android.content.Context
import java.time.LocalDate

class QuoteParser(private val context: Context) {
    private val lineRegex = Regex("^(\\d{1,2})/(\\d{1,2})\\.\\s(.*)$")

    fun loadQuotesFromAsset(assetName: String): Map<Pair<Int, Int>, String> {
        return try {
            context.assets.open(assetName).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.mapNotNull { parseLine(it) }.toMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun parseLine(line: String): Pair<Pair<Int, Int>, String>? {
        val m = lineRegex.find(line.trim()) ?: return null
        val month = m.groupValues[1].toInt()
        val day = m.groupValues[2].toInt()
        val text = m.groupValues[3].trim()
        return (month to day) to text
    }

    fun findQuoteFor(date: LocalDate, primary: Map<Pair<Int, Int>, String>, fallback: Map<Pair<Int, Int>, String>): String? {
        val key = date.monthValue to date.dayOfMonth
        primary[key]?.let { return it }
        // Fallback to English if primary missing
        fallback[key]?.let { return it }
        return null
    }
}
