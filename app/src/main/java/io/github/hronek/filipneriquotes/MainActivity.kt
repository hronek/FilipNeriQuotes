package io.github.hronek.filipneriquotes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
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

class MainActivity : AppCompatActivity() {
    private lateinit var tvQuote: TextView
    private lateinit var tvDate: TextView
    private lateinit var repo: QuoteRepository

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
        repo = QuoteRepository(this)
        // Show Preface on first launch
        if (!Prefs.isPrefaceShown(this)) {
            Prefs.setPrefaceShown(this, true)
            startActivity(Intent(this, PrefaceActivity::class.java))
        }
        refreshQuote()
    }

    private fun refreshQuote() {
        val today = LocalDate.now()
        val lang = Prefs.getLanguage(this).let { if (it == "auto") null else it }
        val dateStr = "%d/%d.".format(today.monthValue, today.dayOfMonth)
        tvDate.text = dateStr
        tvQuote.text = repo.getQuoteFor(today, lang)
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

}