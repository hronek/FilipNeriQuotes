package io.github.hronek.filipneriquotes

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import io.github.hronek.filipneriquotes.data.Prefs
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

class BrochureActivity : AppCompatActivity() {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private lateinit var pager: ViewPager2
    private lateinit var adapter: PdfPageAdapter
    private val TAG = "FNQ-Brochure"
    private var nightMode: Boolean = false
    private var invertFilter: ColorMatrixColorFilter? = null

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

        pager = findViewById(R.id.viewPager)

        // Detect dark theme and prepare invert filter for PDF pages
        nightMode = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        if (nightMode) {
            val m = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                 0f,-1f, 0f, 0f, 255f,
                 0f, 0f,-1f, 0f, 255f,
                 0f, 0f, 0f, 1f,   0f
            ))
            invertFilter = ColorMatrixColorFilter(m)
        }

        if (!openRenderer()) {
            Toast.makeText(this, "Unable to open brochure", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val pageCount = (renderer?.pageCount ?: 0)
        val targetWidth = resources.displayMetrics.widthPixels
        adapter = PdfPageAdapter(renderer, pageCount, targetWidth, nightMode, invertFilter)
        pager.adapter = adapter
    }

    private fun openRenderer(): Boolean {
        // Choose language-specific brochure with fallback to Italian
        val lang = Prefs.getLanguage(this).let { if (it == "auto") Locale.getDefault().language else it }
        val assetName = when (lang.lowercase(Locale.ROOT)) {
            "cs", "cz" -> "brochure_cs.pdf"
            "pl" -> "brochure_pl.pdf"
            "en" -> "brochure_en.pdf"
            "de" -> "brochure_de.pdf"
            "es" -> "brochure_es.pdf"
            "fr" -> "brochure_fr.pdf"
            else -> "brochure_it.pdf"
        }
        val cache = File(cacheDir, assetName)
        if (!cache.exists()) {
            try {
                val chosen = try { assets.open(assetName) } catch (e: Exception) {
                    Log.w(TAG, "Missing $assetName in assets, falling back to brochure_it.pdf")
                    assets.open("brochure_it.pdf")
                }
                chosen.use { input ->
                    FileOutputStream(cache).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy brochure to cache", e)
                return false
            }
        }
        return try {
            fileDescriptor = ParcelFileDescriptor.open(cache, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor!!)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open PdfRenderer", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            renderer?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
    }

    private class PdfPageAdapter(
        private val renderer: PdfRenderer?,
        private val pageCount: Int,
        private val targetWidth: Int,
        private val nightMode: Boolean,
        private val invertFilter: ColorMatrixColorFilter?
    ) : RecyclerView.Adapter<PdfPageViewHolder>() {
        private val cache = object : LruCache<Int, Bitmap>((8 * 1024 * 1024) /* 8MB */) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
            val iv = ImageView(parent.context)
            iv.adjustViewBounds = false
            iv.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            return PdfPageViewHolder(iv)
        }

        override fun getItemCount(): Int = pageCount

        override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
            val bmp = getPageBitmap(position)
            holder.image.setImageBitmap(bmp)
            if (nightMode) {
                holder.image.colorFilter = invertFilter
                holder.image.setBackgroundColor(Color.BLACK)
            } else {
                holder.image.clearColorFilter()
                holder.image.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        private fun getPageBitmap(index: Int): Bitmap? {
            cache.get(index)?.let { return it }
            val r = renderer ?: return null
            if (index < 0 || index >= r.pageCount) return null
            r.openPage(index).use { page ->
                val width = targetWidth.coerceAtLeast(400)
                val height = (width.toFloat() * page.height.toFloat() / page.width.toFloat()).toInt().coerceAtLeast(400)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(index, bitmap)
                return bitmap
            }
        }
    }

    private class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView as ImageView
    }
}
