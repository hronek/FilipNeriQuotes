package io.github.hronek.filipneriquotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ScaleGestureDetector
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import io.github.hronek.filipneriquotes.data.Prefs
import io.github.hronek.filipneriquotes.data.QuoteRepository
import java.text.Normalizer
import java.util.Locale

class QuotesActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var adapter: QuotesAdapter
    private lateinit var repo: QuoteRepository

    private var allLines: List<String> = emptyList()
    private var allLinesNorm: List<String> = emptyList()
    private var currentFilter: String = ""
    private var textSizeSp: Float = 16f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quotes)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        repo = QuoteRepository(this)
        recycler = findViewById(R.id.recycler)
        searchInput = findViewById(R.id.searchInput)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = QuotesAdapter(textSizeSp)
        recycler.adapter = adapter

        // Load quotes for selected language (or auto)
        val lang = Prefs.getLanguage(this).let { if (it == "auto") null else it }
        allLines = repo.getAllQuotesRaw(lang)
        allLinesNorm = allLines.map { normalize(it) }
        adapter.submit(allLines)

        // Live search
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentFilter = s?.toString()?.trim() ?: ""
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Pinch to zoom text size
        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                // clamp size between 12sp and 28sp
                textSizeSp = (textSizeSp * factor).coerceIn(12f, 28f)
                adapter.setTextSize(textSizeSp)
                return true
            }
        })
        recycler.setOnTouchListener { _: View, ev ->
            scaleDetector.onTouchEvent(ev)
            false
        }
    }

    private fun applyFilter() {
        if (currentFilter.isEmpty()) {
            adapter.submit(allLines)
        } else {
            val q = normalize(currentFilter)
            val filtered = allLines.zip(allLinesNorm)
                .filter { (_, norm) -> norm.contains(q) }
                .map { (orig, _) -> orig }
            adapter.submit(filtered)
        }
    }

    private fun normalize(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
        // Remove all diacritic marks and lowercase for case-insensitive search
        return n.replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)
    }
}
