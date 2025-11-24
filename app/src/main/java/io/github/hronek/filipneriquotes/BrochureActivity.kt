package io.github.hronek.filipneriquotes

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import java.util.Locale
import io.github.hronek.filipneriquotes.data.Prefs
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class BrochureActivity : AppCompatActivity() {
    private val TAG = "FNQ-Brochure"
    private lateinit var webView: WebView
    private lateinit var scaleDetector: ScaleGestureDetector
    private var currentTextZoom: Int = 130

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_brochure)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.menu_brochure)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        webView = findViewById(R.id.webView)
        configureWebView(webView)
        // Scale detector to change only text size on pinch
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                // Convert pinch factor to a delta in text zoom, clamp to sensible range
                val delta = ((factor - 1f) * 100).toInt()
                currentTextZoom = (currentTextZoom + delta).coerceIn(80, 220)
                webView.settings.textZoom = currentTextZoom
                return true
            }
        })
        webView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            // Consume when scaling, otherwise let WebView handle normal scrolling/taps
            event.actionMasked == MotionEvent.ACTION_MOVE && scaleDetector.isInProgress
        }
        loadBrochureHtml()
    }

    private fun configureWebView(wv: WebView) {
        wv.webViewClient = WebViewClient()
        wv.settings.apply {
            // We manage zoom as text size via pinch; disable built-in page zoom
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptEnabled = false
            textZoom = currentTextZoom
            // Reflow text to fit viewport when size changes (where supported)
            try {
                @Suppress("DEPRECATION")
                this.layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            } catch (_: Throwable) { }
        }

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.settings, isDark)
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                wv.settings,
                if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }

    private fun loadBrochureHtml() {
        val lang = Prefs.getLanguage(this).let { if (it == "auto") Locale.getDefault().language else it }
        val candidate = when (lang.lowercase(Locale.ROOT)) {
            "cs", "cz" -> "brochure_cs.html"
            "pl" -> "brochure_pol.html"
            "en" -> "brochure_en.html"
            "de" -> "brochure_de.html"
            "es" -> "brochure_spa.html"
            "fr" -> "brochure_fra.html"
            else -> "brochure_it.html"
        }
        val chosen = if (assetExists(candidate)) candidate else {
            Log.w(TAG, "Missing $candidate in assets, falling back to brochure_it.html")
            if (assetExists("brochure_it.html")) "brochure_it.html" else candidate
        }
        // Read HTML, inject responsive meta/CSS to keep full width and scale text nicely
        val html = readAssetText(chosen)
        val enhanced = injectResponsiveHead(html)
        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            enhanced,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun assetExists(name: String): Boolean {
        var input: InputStream? = null
        return try {
            input = assets.open(name)
            true
        } catch (e: Exception) {
            false
        } finally {
            try { input?.close() } catch (_: Exception) {}
        }
    }

    private fun readAssetText(name: String): String {
        assets.open(name).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
                return sb.toString()
            }
        }
    }

    private fun injectResponsiveHead(html: String): String {
        val headInject = """
            <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
            <style>
              html, body { max-width: 100%; overflow-x: hidden; }
              body { margin: 16px; line-height: 1.65; }
              img, iframe, table { max-width: 100%; height: auto; }
              * { box-sizing: border-box; }
              @media (prefers-color-scheme: dark) {
                body { background: #121212; color: #e5e5e5; }
                a { color: #8ab4f8; }
              }
            </style>
        """.trimIndent()
        val headTag = Regex("(?i)<head\\s*>")
        if (headTag.containsMatchIn(html)) {
            return headTag.replace(html, "$0\n$headInject\n")
        }
        val htmlTag = Regex("(?i)<html\\b[^>]*>")
        if (htmlTag.containsMatchIn(html)) {
            return htmlTag.replace(html, "$0\n<head>\n$headInject\n</head>\n")
        }
        return "<head>\n$headInject\n</head>\n$html"
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
