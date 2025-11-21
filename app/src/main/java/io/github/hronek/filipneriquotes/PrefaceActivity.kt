package io.github.hronek.filipneriquotes

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import java.util.Locale
import java.io.BufferedReader
import io.github.hronek.filipneriquotes.data.Prefs
import com.google.android.material.appbar.MaterialToolbar

class PrefaceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preface)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val tv: TextView = findViewById(R.id.tvPreface)
        tv.text = loadPrefaceText()
    }

    private fun loadPrefaceText(): String {
        val lang = Prefs.getLanguage(this).let { if (it == "auto") Locale.getDefault().language else it }
        val assetName = when (lang.lowercase(Locale.ROOT)) {
            "cs", "cz" -> "preface_cs.txt"
            "pl" -> "preface_pl.txt"
            "it" -> "preface_it.txt"
            "de" -> "preface_de.txt"
            "es" -> "preface_es.txt"
            "fr" -> "preface_fr.txt"
            else -> "preface_en.txt"
        }
        val primary = readAssetSafe(assetName)
        if (primary != null) return primary
        return readAssetSafe("preface_en.txt") ?: ""
    }

    private fun readAssetSafe(name: String): String? = try {
        assets.open(name).bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
    } catch (e: Exception) {
        null
    }
}
