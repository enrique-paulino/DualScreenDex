package com.enrpau.dualscreendex

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import androidx.core.content.edit
import com.enrpau.dualscreendex.data.RomManager
import com.enrpau.dualscreendex.data.RomProfile
import androidx.recyclerview.widget.RecyclerView

class SettingsActivity : AppCompatActivity() {

    // ui
    private lateinit var root: android.view.View
    private lateinit var toolbar: MaterialToolbar

    // text labels
    private lateinit var lblThemeTitle: TextView
    private lateinit var lblScannerTitle: TextView

    // theme cards
    private lateinit var cardThemeDynamic: MaterialCardView
    private lateinit var cardThemeOled: MaterialCardView
    private lateinit var cardThemeRed: MaterialCardView
    private lateinit var cardThemeMagical: MaterialCardView

    // scanner buttons
    private lateinit var btnScanSource: MaterialButton
    private lateinit var btnScanAlign: MaterialButton

    // profile ui
    private lateinit var rvProfiles: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // bind views
        root = findViewById(R.id.settings_root)
        toolbar = findViewById(R.id.topAppBar)

        lblThemeTitle = findViewById(R.id.lblThemeTitle)
        lblScannerTitle = findViewById(R.id.lblScannerTitle)

        cardThemeDynamic = findViewById(R.id.cardThemeDynamic)
        cardThemeOled = findViewById(R.id.cardThemeOled)
        cardThemeRed = findViewById(R.id.cardThemeRed)
        cardThemeMagical = findViewById(R.id.cardThemeMagical)

        btnScanSource = findViewById(R.id.btnScanSource)
        btnScanAlign = findViewById(R.id.btnScanAlign)

        rvProfiles = findViewById(R.id.rvProfiles)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar.setNavigationOnClickListener { finish() }

        val prefs = getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)

        setupThemeClicks(prefs)
        setupScannerClicks(prefs)
        setupProfiles()

        // set initial state
        refreshThemeUI(prefs)
        refreshScannerUI(prefs)
        applyThemeToSettingsScreen()
    }

    override fun onResume() {
        super.onResume()
        refreshProfileList()
    }

    private fun setupProfiles() {
        findViewById<MaterialButton>(R.id.btnCreateProfile).setOnClickListener {
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }
        rvProfiles.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        refreshProfileList()
    }

    private fun refreshProfileList() {
        val profiles = RomManager.getAllProfiles()
        val current = RomManager.currentProfile

        android.util.Log.d("DualDex_Settings", "Refreshing UI. Found ${profiles.size} profiles.")

        val adapter = ProfileAdapter(profiles, current.id,
            onSelect = { profile ->
                RomManager.selectProfile(this, profile)
                refreshProfileList()
            },
            onDelete = { profile ->
                RomManager.deleteCustomProfile(this, profile)
                refreshProfileList()
            }
        )
        rvProfiles.adapter = adapter
    }

    private fun setupThemeClicks(prefs: SharedPreferences) {
        val clickListener = { themeId: String ->
            prefs.edit { putString("SELECTED_THEME_ID", themeId) }
            ThemeManager.loadTheme(this)

            refreshThemeUI(prefs)
            applyThemeToSettingsScreen()
        }

        cardThemeDynamic.setOnClickListener { clickListener("dynamic") }
        cardThemeOled.setOnClickListener { clickListener("oled") }
        cardThemeRed.setOnClickListener { clickListener("red") }
        cardThemeMagical.setOnClickListener { clickListener("magical") }
    }

    private fun setupScannerClicks(prefs: SharedPreferences) {
        btnScanSource.setOnClickListener {
            val current = prefs.getString("SCAN_SOURCE", "top") ?: "top"
            val newMode = if (current == "top") "bottom" else "top"

            prefs.edit { putString("SCAN_SOURCE", newMode) }
            refreshScannerUI(prefs)
        }

        btnScanAlign.setOnClickListener {
            val current = prefs.getString("SCAN_ALIGN", "left") ?: "left"
            val newMode = if (current == "left") "right" else "left"

            prefs.edit { putString("SCAN_ALIGN", newMode) }
            refreshScannerUI(prefs)
        }
    }

    private fun refreshThemeUI(prefs: SharedPreferences) {
        val selected = prefs.getString("SELECTED_THEME_ID", "dynamic")
        val theme = ThemeManager.currentTheme
        val highlightColor = if (theme.id == "oled") Color.WHITE else Color.BLACK

        fun updateThemeCard(card: MaterialCardView, isSelected: Boolean) {
            if (isSelected) {
                card.strokeColor = highlightColor
                card.strokeWidth = 8
            } else {
                card.strokeColor = Color.TRANSPARENT
                card.strokeWidth = 0
            }
        }

        updateThemeCard(cardThemeDynamic, selected == "dynamic")
        updateThemeCard(cardThemeOled, selected == "oled")
        updateThemeCard(cardThemeRed, selected == "red")
        updateThemeCard(cardThemeMagical, selected == "magical")
    }

    private fun refreshScannerUI(prefs: SharedPreferences) {
        val source = prefs.getString("SCAN_SOURCE", "top")?.uppercase() ?: "TOP"
        val align = prefs.getString("SCAN_ALIGN", "left")?.uppercase() ?: "LEFT"

        btnScanSource.text = "Scan Screen: $source"
        btnScanAlign.text = "Pokemon Aligned: $align"
    }

    private fun applyThemeToSettingsScreen() {
        val theme = ThemeManager.currentTheme

        // backgrounds
        if (theme.isGradientBg) {
            root.background = ThemeManager.StarryDrawable()
            toolbar.setBackgroundColor(Color.TRANSPARENT)
        } else if (theme.isRetroScreen) {
            root.background = ThemeManager.createScanlineDrawable(this, withBorder = false)
            toolbar.setBackgroundColor(theme.windowBackground)
        } else {
            root.background = null
            val bgColor = if (theme.id == "red") theme.contentBackground else theme.windowBackground
            root.setBackgroundColor(bgColor)
            toolbar.setBackgroundColor(bgColor)
        }

        // text colors
        val textColor = if (theme.id == "oled") Color.WHITE else Color.BLACK
        if (theme.id == "oled") Color.LTGRAY else Color.DKGRAY
        val accentColor = if (theme.id == "red") theme.searchBoxColor else theme.headerTextColor

        toolbar.setTitleTextColor(textColor)
        toolbar.setNavigationIconTint(textColor)

        lblThemeTitle.setTextColor(textColor)
        lblScannerTitle.setTextColor(textColor)

        val btnTextColor = if (theme.id == "oled") Color.WHITE else accentColor
        val btnStrokeColor = if (theme.id == "oled") Color.DKGRAY else Color.LTGRAY

        fun styleButton(btn: MaterialButton) {
            btn.setTextColor(btnTextColor)
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(btnStrokeColor))
            btn.setRippleColor(android.content.res.ColorStateList.valueOf("#20000000".toColorInt()))
        }

        styleButton(btnScanSource)
        styleButton(btnScanAlign)

        val prefs = getSharedPreferences("DualDexPrefs", MODE_PRIVATE)
    }
}