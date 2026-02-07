package com.enrpau.dualscreendex

import android.content.Context
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
import com.google.android.material.card.MaterialCardView

class SettingsActivity : AppCompatActivity() {

    // ui
    private lateinit var root: android.view.View
    private lateinit var toolbar: MaterialToolbar

    // text labels
    private lateinit var lblGenTitle: TextView
    private lateinit var lblGenDesc: TextView
    private lateinit var lblThemeTitle: TextView

    // gen cards
    private lateinit var cardGen6: MaterialCardView
    private lateinit var cardGen25: MaterialCardView
    private lateinit var cardGen1: MaterialCardView
    private lateinit var txtGen6: TextView
    private lateinit var txtGen25: TextView
    private lateinit var txtGen1: TextView

    // theme cards
    private lateinit var cardThemeDynamic: MaterialCardView
    private lateinit var cardThemeOled: MaterialCardView
    private lateinit var cardThemeRed: MaterialCardView
    private lateinit var cardThemeMagical: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // load theme
        ThemeManager.loadTheme(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // bind views
        root = findViewById(R.id.settings_root)
        toolbar = findViewById(R.id.topAppBar)

        lblGenTitle = findViewById(R.id.lblGenTitle)
        lblGenDesc = findViewById(R.id.lblGenDesc)
        lblThemeTitle = findViewById(R.id.lblThemeTitle)

        cardGen6 = findViewById(R.id.cardGen6)
        cardGen25 = findViewById(R.id.cardGen25)
        cardGen1 = findViewById(R.id.cardGen1)
        txtGen6 = findViewById(R.id.txtGen6)
        txtGen25 = findViewById(R.id.txtGen25)
        txtGen1 = findViewById(R.id.txtGen1)

        cardThemeDynamic = findViewById(R.id.cardThemeDynamic)
        cardThemeOled = findViewById(R.id.cardThemeOled)
        cardThemeRed = findViewById(R.id.cardThemeRed)
        cardThemeMagical = findViewById(R.id.cardThemeMagical)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar.setNavigationOnClickListener { finish() }

        val prefs = getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)

        setupGenClicks(prefs)
        setupThemeClicks(prefs)

        // set initial state
        refreshGenUI(prefs)
        refreshThemeUI(prefs)
        applyThemeToSettingsScreen()
    }

    private fun setupGenClicks(prefs: SharedPreferences) {
        val clickListener = { genCode: String ->
            prefs.edit().putString("SELECTED_GEN", genCode).apply()
            refreshGenUI(prefs)
        }

        cardGen6.setOnClickListener { clickListener("GEN_6_PLUS") }
        cardGen25.setOnClickListener { clickListener("GEN_2_TO_5") }
        cardGen1.setOnClickListener { clickListener("GEN_1") }
    }

    private fun setupThemeClicks(prefs: SharedPreferences) {
        val clickListener = { themeId: String ->
            prefs.edit().putString("SELECTED_THEME_ID", themeId).apply()

            ThemeManager.loadTheme(this)

            refreshThemeUI(prefs)
            applyThemeToSettingsScreen()
        }

        cardThemeDynamic.setOnClickListener { clickListener("dynamic") }
        cardThemeOled.setOnClickListener { clickListener("oled") }
        cardThemeRed.setOnClickListener { clickListener("red") }
        cardThemeMagical.setOnClickListener { clickListener("magical") }
    }

    private fun refreshGenUI(prefs: SharedPreferences) {
        val selected = prefs.getString("SELECTED_GEN", "GEN_6_PLUS")

        // style the card
        fun updateCard(card: MaterialCardView, txt: TextView, isSelected: Boolean) {
            val theme = ThemeManager.currentTheme
            val activeColor = if (theme.id == "red") theme.searchBoxColor else theme.headerTextColor

            if (isSelected) {
                card.strokeColor = activeColor
                card.strokeWidth = 6
                txt.setTextColor(activeColor)
            } else {
                card.strokeColor = if (theme.id == "oled") Color.DKGRAY else Color.LTGRAY
                card.strokeWidth = 2
                txt.setTextColor(if (theme.id == "oled") Color.GRAY else Color.GRAY)
            }

            card.setCardBackgroundColor(if (theme.id == "oled") Color.BLACK else Color.WHITE)
        }

        updateCard(cardGen6, txtGen6, selected == "GEN_6_PLUS")
        updateCard(cardGen25, txtGen25, selected == "GEN_2_TO_5")
        updateCard(cardGen1, txtGen1, selected == "GEN_1")
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

    private fun applyThemeToSettingsScreen() {
        val theme = ThemeManager.currentTheme

        // backgrounds
        if (theme.isGradientBg) {
            // magical theme
            root.background = ThemeManager.StarryDrawable()
            toolbar.setBackgroundColor(Color.TRANSPARENT)
        } else if (theme.isRetroScreen) {
            // pokedex theme
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
        val subTextColor = if (theme.id == "oled") Color.LTGRAY else Color.DKGRAY

        toolbar.setTitleTextColor(textColor)
        toolbar.setNavigationIconTint(textColor)

        lblGenTitle.setTextColor(textColor)
        lblGenDesc.setTextColor(subTextColor)
        lblThemeTitle.setTextColor(textColor)

        val prefs = getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)
        refreshGenUI(prefs)
    }
}