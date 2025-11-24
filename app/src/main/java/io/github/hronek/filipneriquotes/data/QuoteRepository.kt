package io.github.hronek.filipneriquotes.data

import android.content.Context
import java.time.LocalDate
import java.util.Locale

class QuoteRepository(private val context: Context) {
    private val parser = QuoteParser(context)

    private fun languageToAsset(lang: String): String = when (lang.lowercase(Locale.ROOT)) {
        "cs", "cz" -> "quotes_cz.txt"
        "pl" -> "quotes_pol.txt"
        "it" -> "quotes_it.txt"
        "de" -> "quotes_de.txt"
        "es" -> "quotes_spa.txt"
        "fr" -> "quotes_fra.txt"
        else -> "quotes_en.txt"
    }

    fun getQuoteFor(date: LocalDate = LocalDate.now(), languageOverride: String? = null): String {
        val lang = (languageOverride ?: Locale.getDefault().language)
        val primaryAsset = languageToAsset(lang)
        val primary = parser.loadQuotesFromAsset(primaryAsset)
        val fallback = if (primaryAsset == "quotes_en.txt") primary else parser.loadQuotesFromAsset("quotes_en.txt")
        val text = parser.findQuoteFor(date, primary, fallback)
        return text ?: "Quote not found for ${'$'}{date.monthValue}/${'$'}{date.dayOfMonth}."
    }

    fun getAllQuotesRaw(languageOverride: String? = null): List<String> {
        val lang = (languageOverride ?: Locale.getDefault().language)
        val primaryAsset = languageToAsset(lang)
        val primary = parser.loadQuotesFromAsset(primaryAsset)
        val fallback = if (primaryAsset == "quotes_en.txt") primary else parser.loadQuotesFromAsset("quotes_en.txt")
        // Merge by key (month, day), take primary first then fallback, and order by month then day
        val keys = (primary.keys + fallback.keys).toSet().sortedWith(compareBy({ it.first }, { it.second }))
        return keys.mapNotNull { key -> primary[key] ?: fallback[key] }
    }
}
