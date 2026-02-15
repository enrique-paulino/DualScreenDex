package com.enrpau.dualscreendex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // ui references
    private lateinit var tvName: TextView
    private lateinit var tvId: TextView
    private lateinit var layoutTypes: LinearLayout
    private lateinit var gridWeak: LinearLayout
    private lateinit var gridResist: LinearLayout
    private lateinit var lblWeak: TextView
    private lateinit var lblResist: TextView
    private lateinit var btnVariantToggle: com.google.android.material.button.MaterialButton

    private lateinit var navLeftBtn: LinearLayout
    private lateinit var navRightBtn: LinearLayout
    private lateinit var tvPrevName: TextView
    private lateinit var tvNextName: TextView

    private lateinit var containerBattle: View
    private lateinit var cardHeader: androidx.cardview.widget.CardView
    private lateinit var cardData: androidx.cardview.widget.CardView
    private lateinit var containerPokedex: LinearLayout
    private lateinit var btnBack: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var rvList: androidx.recyclerview.widget.RecyclerView
    private lateinit var etSearch: com.google.android.material.textfield.TextInputEditText
    private lateinit var searchContainer: com.google.android.material.textfield.TextInputLayout

    private lateinit var cardBattleTab: androidx.cardview.widget.CardView
    private lateinit var tvBattleTabText: TextView

    private lateinit var adapter: PokemonAdapter

    private val pokemonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val found = intent.getBooleanExtra("FOUND", false)
            if (!found) {
                viewModel.onScanResult(null, null, null, null)
                return
            }
            viewModel.onScanResult(
                intent.getStringArrayListExtra("NAMES"),
                intent.getIntegerArrayListExtra("IDS"),
                intent.getStringArrayListExtra("TYPE1S"),
                intent.getStringArrayListExtra("TYPE2S")
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initViews()

        rvList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter = PokemonAdapter(emptyList()) { selectedPokemon ->
            viewModel.onPokemonSelectedFromList(selectedPokemon)
            hideKeyboard()
        }
        rvList.adapter = adapter

        setupListeners()
        loadSettings()

        viewModel.displayedPokemon.observeForeverSafe { pokemon ->
            updateCardUI(pokemon)
        }

        viewModel.pokedexList.observeForeverSafe { list ->
            adapter.updateList(list)
        }

        viewModel.isBattleMode.observeForeverSafe { isBattle ->
            if (isBattle) {
                containerPokedex.visibility = View.GONE
                containerBattle.visibility = View.VISIBLE
                btnBack.visibility = View.VISIBLE
            } else {
                containerPokedex.visibility = View.VISIBLE
                containerBattle.visibility = View.GONE
                btnBack.visibility = View.GONE
            }
        }

        viewModel.prevPokemonName.observeForeverSafe { name ->
            tvPrevName.text = name.replaceFirstChar { it.uppercase() }
        }

        viewModel.nextPokemonName.observeForeverSafe { name ->
            tvNextName.text = name.replaceFirstChar { it.uppercase() }
        }

        viewModel.isPrevButtonVisible.observeForeverSafe { visible ->
            navLeftBtn.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }

        viewModel.isNextButtonVisible.observeForeverSafe { visible ->
            navRightBtn.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }

        viewModel.showBattleTab.observeForeverSafe { show ->
            cardBattleTab.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.battleTabText.observeForeverSafe { text ->
            tvBattleTabText.text = text
        }

        viewModel.battleTabColor.observeForeverSafe { color ->
            cardBattleTab.setCardBackgroundColor(color)
            tvBattleTabText.setTextColor(Color.WHITE)
        }

        viewModel.weaknessList.observeForeverSafe { list ->
            populateSmartGrid(gridWeak, list)
            lblWeak.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            gridWeak.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.resistanceList.observeForeverSafe { list ->
            populateSmartGrid(gridResist, list)
            lblResist.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            gridResist.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
        }

        if (!isAccessibilityServiceEnabled()) {
            android.widget.Toast.makeText(this, "Please enable DualDex in Accessibility Settings", android.widget.Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } else {
            android.widget.Toast.makeText(this, "Scanner Ready", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return prefString?.contains("$packageName/$packageName.DualDexAccessibilityService") == true
    }

    private fun initViews() {
        tvName = findViewById(R.id.tvTargetName)
        tvId = findViewById(R.id.tvTargetId)
        layoutTypes = findViewById(R.id.layoutTargetTypes)
        gridWeak = findViewById(R.id.gridWeak)
        gridResist = findViewById(R.id.gridResist)
        lblWeak = findViewById(R.id.lblWeak)
        lblResist = findViewById(R.id.lblResist)
        btnVariantToggle = findViewById(R.id.btnVariantToggle)
        navLeftBtn = findViewById(R.id.navLeftContainer)
        navRightBtn = findViewById(R.id.navRightContainer)
        tvPrevName = findViewById(R.id.tvPrevName)
        tvNextName = findViewById(R.id.tvNextName)

        containerBattle = findViewById(R.id.containerBattle)
        cardHeader = findViewById(R.id.cardHeader)
        cardData = findViewById(R.id.cardData)
        containerPokedex = findViewById(R.id.containerPokedex)
        btnBack = findViewById(R.id.btnBackToList)
        rvList = findViewById(R.id.rvPokemonList)
        etSearch = findViewById(R.id.etSearch)
        searchContainer = findViewById(R.id.searchContainer)
        cardBattleTab = findViewById(R.id.cardBattleTab)
        tvBattleTabText = findViewById(R.id.tvBattleTabText)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        cardBattleTab.setOnClickListener { viewModel.onBattleTabClicked() }
        btnVariantToggle.setOnClickListener { viewModel.onVariantToggleClicked() }
        btnBack.setOnClickListener { viewModel.onBackToListClicked() }
        navLeftBtn.setOnClickListener { viewModel.onPrevClicked() }
        navRightBtn.setOnClickListener { viewModel.onNextClicked() }

        searchContainer.setEndIconOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.onSearchQuery(s.toString().trim())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateCardUI(pokemon: Pokemon) {
        val theme = ThemeManager.currentTheme
        val displayBaseColor = pokemon.type1.colorHex
        val cleanName = pokemon.name.replaceFirstChar { it.uppercase() }
        val displayName = if (pokemon.variantLabel != null) "$cleanName (${pokemon.variantLabel})" else cleanName

        tvName.text = displayName
        tvId.text = String.format("#%03d", pokemon.id)
        tvName.setTextColor(theme.headerTextColor)
        tvId.setTextColor(theme.subTextColor)

        val headerColor = when (theme.id) {
            "red" -> Color.TRANSPARENT
            "dynamic", "magical" -> displayBaseColor.blendWithWhite(0.7f)
            "oled" -> Color.BLACK
            else -> theme.windowBackground
        }
        cardHeader.setCardBackgroundColor(headerColor)
        cardHeader.radius = theme.cardCornerRadius
        cardData.radius = theme.cardCornerRadius
        cardHeader.cardElevation = if (theme.id == "dynamic") 8f else 0f

        if (theme.isRetroScreen) {
            containerBattle.background = ThemeManager.createScanlineDrawable(this, true)
            cardData.setCardBackgroundColor(Color.TRANSPARENT)
            cardData.cardElevation = 0f
        } else {
            containerBattle.background = null
            if (theme.id == "dynamic") {
                containerBattle.setBackgroundColor(displayBaseColor.blendWithWhite(0.9f))
            } else {
                containerBattle.setBackgroundColor(theme.windowBackground)
            }
            cardData.setCardBackgroundColor(theme.gridBackgroundColor)
            cardData.cardElevation = if (theme.id == "dynamic") 4f else 0f
        }

        if (viewModel.hasVariants()) {
            btnVariantToggle.visibility = View.VISIBLE
            btnVariantToggle.text = "Switch to ${viewModel.getNextVariantName()}"
            btnVariantToggle.setTextColor(theme.labelTextColor)
            btnVariantToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(theme.labelTextColor))
        } else {
            btnVariantToggle.visibility = View.GONE
        }

        lblWeak.setTextColor(theme.labelTextColor)
        lblResist.setTextColor(theme.labelTextColor)

        layoutTypes.removeAllViews()
        addFullWidthTypeBadge(layoutTypes, pokemon.type1)
        if (pokemon.type2 != null && pokemon.type2 != PokemonType.UNKNOWN) {
            addFullWidthTypeBadge(layoutTypes, pokemon.type2)
        }
    }

    private fun loadSettings() {
        viewModel.refreshSettings()
        applyThemeColors()
        if (::adapter.isInitialized) {
            val currentMechanics = com.enrpau.dualscreendex.data.RomManager.currentProfile.baseMechanics
            val currentTheme = ThemeManager.currentTheme
            adapter.updateSettings(currentMechanics, currentTheme)
            val freshList = viewModel.repository.getAllPokemon()
            adapter.updateList(freshList)
        }
    }

    private fun applyThemeColors() {
        val theme = ThemeManager.currentTheme
        val root = findViewById<View>(R.id.main_root)

        if (theme.isGradientBg) {
            root.background = ThemeManager.StarryDrawable()
            window.statusBarColor = "#E1BEE7".toColorInt()
        } else {
            root.background = null
            root.setBackgroundColor(theme.windowBackground)
            window.statusBarColor = theme.windowBackground
        }

        if (theme.isRetroScreen) {
            containerPokedex.background = ThemeManager.createScanlineDrawable(this, withBorder = true)
            containerPokedex.setPadding(24, 24, 24, 24)
        } else {
            containerPokedex.background = null
            if (!theme.isGradientBg) {
                containerPokedex.setBackgroundColor(theme.contentBackground)
            }
            containerPokedex.setPadding(0, 0, 0, 0)
        }

        val searchBox = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchContainer)
        val etSearch = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)

        searchBox.boxBackgroundColor = theme.searchBoxColor
        etSearch.setTextColor(theme.searchTextColor)
        etSearch.setHintTextColor(theme.searchHintColor)
        searchBox.defaultHintTextColor = android.content.res.ColorStateList.valueOf(theme.searchHintColor)
        searchBox.setBoxStrokeColorStateList(android.content.res.ColorStateList.valueOf(theme.searchStrokeColor))
        searchBox.boxStrokeWidth = theme.searchStrokeWidth
        searchBox.boxStrokeWidthFocused = theme.searchStrokeWidth + 2
        searchBox.setBoxCornerRadii(theme.searchCornerRadius, theme.searchCornerRadius, theme.searchCornerRadius, theme.searchCornerRadius)

        val params = searchBox.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val density = resources.displayMetrics.density
        val marginPx = (theme.searchMarginHorizontal * density).toInt()
        params.setMargins(marginPx, marginPx / 2, marginPx, marginPx / 2)
        searchBox.layoutParams = params

        val iconColor = if (theme.id == "oled" || theme.id == "red") Color.WHITE else "#5D4037".toColorInt()
        searchBox.setEndIconTintList(android.content.res.ColorStateList.valueOf(iconColor))

        if (::adapter.isInitialized) {
            val currentMechanics = com.enrpau.dualscreendex.data.RomManager.currentProfile.baseMechanics
            adapter.updateSettings(currentMechanics, theme)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    data class MatchupData(val type: PokemonType, val multiplier: Double)
    private fun Int.blendWithWhite(ratio: Float): Int = ColorUtils.blendARGB(this, Color.WHITE, ratio)

    private fun populateSmartGrid(container: LinearLayout, list: List<MatchupData>) {
        container.removeAllViews()
        val rows = list.chunked(3)
        for (rowItems in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = rowItems.size.toFloat()
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 16)
                }
            }
            for (data in rowItems) {
                val badge = createWeaknessBadgeView(data.type, data.multiplier)
                rowLayout.addView(badge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(8, 0, 8, 0)
                })
            }
            container.addView(rowLayout)
        }
    }

    private fun createWeaknessBadgeView(type: PokemonType, mult: Double): View {
        val view = layoutInflater.inflate(R.layout.badge_weakness, null, false)
        view.findViewById<TextView>(R.id.tvBadgeType).apply {
            text = type.displayName
            setBackgroundColor(type.colorHex)
        }
        view.findViewById<TextView>(R.id.tvBadgeMult).apply {
            text = "× ${if(mult == 0.5) "½" else if(mult == 0.25) "¼" else if(mult == 0.0) "0" else mult.toInt().toString()}"
            setBackgroundColor(type.colorHex)
        }
        return view
    }

    private fun addFullWidthTypeBadge(container: LinearLayout, type: PokemonType) {
        val tv = TextView(this).apply {
            text = type.displayName.uppercase()
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setBackgroundColor(type.colorHex)
        }
        container.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(1, 0, 1, 0)
        })
    }

    // helper to force updates even when paused or in background
    private fun <T> androidx.lifecycle.LiveData<T>.observeForeverSafe(observer: (T) -> Unit) {
        val wrapper = androidx.lifecycle.Observer<T> { data ->
            observer(data)
        }
        this.observeForever(wrapper)

        lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                    this@observeForeverSafe.removeObserver(wrapper)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadSettings()

        val filter = IntentFilter("com.enrpau.dualscreendex.POKEMON_DETECTED")
        ContextCompat.registerReceiver(this, pokemonReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pokemonReceiver)
    }
}