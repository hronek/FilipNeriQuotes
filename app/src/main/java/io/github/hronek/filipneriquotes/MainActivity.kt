package io.github.hronek.filipneriquotes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import android.view.View
import com.google.android.material.button.MaterialButton
import io.github.hronek.filipneriquotes.data.QuoteRepository
import java.time.LocalDate
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import io.github.hronek.filipneriquotes.data.Prefs
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

class MainActivity : AppCompatActivity() {
    private lateinit var tvQuote: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvAttribution: TextView
    private lateinit var repo: QuoteRepository
    private lateinit var navContainer: View
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnToday: MaterialButton
    private lateinit var btnNext: MaterialButton
    private var currentDate: LocalDate = LocalDate.now()
    private var userNavigatedByArrows: Boolean = false
    private var timeTickReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply theme according to preference before inflating views
        when (Prefs.getTheme(this)) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        // Apply app language according to preference before inflating views
        val langPref = Prefs.getLanguage(this)
        val appLocales = if (langPref == "auto") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(langPref)
        AppCompatDelegate.setApplicationLocales(appLocales)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        tvQuote = findViewById(R.id.tvQuote)
        tvDate = findViewById(R.id.tvDate)
        tvAttribution = findViewById(R.id.tvAttribution)
        repo = QuoteRepository(this)
        // Navigation buttons
        navContainer = findViewById(R.id.navButtons)
        btnPrev = findViewById(R.id.btnPrev)
        btnToday = findViewById(R.id.btnToday)
        btnNext = findViewById(R.id.btnNext)
        // Explicitly refresh resource-based texts to avoid stale state restore after locale change
        refreshStaticTexts()
        btnPrev.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            userNavigatedByArrows = true
            refreshQuote()
        }
        btnToday.setOnClickListener {
            currentDate = LocalDate.now()
            userNavigatedByArrows = false
            refreshQuote()
        }
        btnNext.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            userNavigatedByArrows = true
            refreshQuote()
        }
        // Show Preface on first launch
        if (!Prefs.isPrefaceShown(this)) {
            Prefs.setPrefaceShown(this, true)
            startActivity(Intent(this, PrefaceActivity::class.java))
        }
        updateNavVisibility()
        refreshQuote()
    }

    private fun refreshQuote() {
        val lang = Prefs.getLanguage(this).let { if (it == "auto") null else it }
        tvDate.text = formatDate(currentDate)
        tvQuote.text = repo.getQuoteFor(currentDate, lang)
    }

    private fun formatDate(date: LocalDate): String {
        // Determine app language (explicit) or device language
        val appLang = Prefs.getLanguage(this).let { if (it == "auto") Locale.getDefault().language else it }
        val m = date.monthValue
        val d = date.dayOfMonth
        // Common conventions (no leading zeros by using single M and d semantics via manual build)
        return when (appLang.lowercase(Locale.ROOT)) {
            // Czech: day.month with dot separator, no leading zeros
            "cs", "cz" -> "$d.$m"
            // Polish & German: typically day.month with dot separator
            "pl" -> "$d.$m"
            "de" -> "$d.$m"
            // French, Spanish, Italian: day/month with slash
            "fr" -> "$d/$m"
            "es" -> "$d/$m"
            "it" -> "$d/$m"
            // English (assume US-style): month/day with slash
            else -> "$m/$d"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_language -> {
                showLanguageDialog()
                true
            }
            R.id.action_preface -> {
                startActivity(Intent(this, PrefaceActivity::class.java))
                true
            }
            R.id.action_brochure -> {
                startActivity(Intent(this, BrochureActivity::class.java))
                true
            }
            R.id.action_quotes -> {
                startActivity(Intent(this, QuotesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLanguageDialog() {
        val entries = arrayOf(
            getString(R.string.language_auto),
            getString(R.string.language_cs),
            getString(R.string.language_pl),
            getString(R.string.language_it),
            getString(R.string.language_de),
            getString(R.string.language_es),
            getString(R.string.language_fr),
            getString(R.string.language_en)
        )
        val values = arrayOf("auto","cs","pl","it","de","es","fr","en")
        val current = Prefs.getLanguage(this)
        val checked = values.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.language_select_title)
            .setSingleChoiceItems(entries, checked) { dialog, which ->
                val sel = values[which]
                Prefs.setLanguage(this, sel)
                // Apply app locales immediately and recreate to update all strings/menu
                val locales = if (sel == "auto") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(sel)
                AppCompatDelegate.setApplicationLocales(locales)
                recreate()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun checkDayChangeAutoRefresh() {
        val now = LocalDate.now()
        if (!userNavigatedByArrows && now != currentDate) {
            currentDate = now
            refreshQuote()
        }
    }


    override fun onPause() {
        super.onPause()
        timeTickReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        timeTickReceiver = null
    }

    private fun refreshStaticTexts() {
        btnToday.setText(R.string.btn_today)
        tvAttribution.setText(R.string.attribution_text)
    }

    override fun onResume() {
        super.onResume()
        updateNavVisibility()
        checkDayChangeAutoRefresh()
        // Ensure texts are correct after possible view state restoration
        refreshStaticTexts()
        // Post one more refresh to run after any late state restore that might overwrite text
        btnToday.post { refreshStaticTexts() }
        if (timeTickReceiver == null) {
            timeTickReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_TIME_TICK) {
                        checkDayChangeAutoRefresh()
                    }
                }
            }
            registerReceiver(timeTickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        }
    }

    private fun updateNavVisibility() {
        navContainer.visibility = if (Prefs.isNavButtonsEnabled(this)) View.VISIBLE else View.GONE
    }

}