package com.enrpau.dualscreendex

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.graphics.toColorInt
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

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
    private var currentGen: TypeMatchup.Gen = TypeMatchup.Gen.GEN_6_PLUS // Default
    private lateinit var searchContainer: com.google.android.material.textfield.TextInputLayout

    // minimized tab stuff
    private lateinit var cardBattleTab: androidx.cardview.widget.CardView
    private lateinit var tvBattleTabText: TextView

    private lateinit var adapter: PokemonAdapter
    private lateinit var pokedexHelper: PokedexHelper

    // state tracking
    private var navigationList: List<Pokemon> = emptyList()
    private var currentVariantList: List<Pokemon> = emptyList()
    private var selectedIndex: Int = 0
    private var currentVariantIndex: Int = 0

    // data sources
    private var allPokemonCache: List<Pokemon> = emptyList() // full db
    private var battleList: List<Pokemon> = emptyList()      // live scanner data

    // flags
    private var isViewingBattleMode: Boolean = false // true = scanner data, false = pokedex data

    data class MatchupData(val type: PokemonType, val multiplier: Double)

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val startMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startScreenCaptureService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val pokemonReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val found = intent.getBooleanExtra("FOUND", false)

            // nothing found, hide the tab
            if (!found) {
                battleList = emptyList()
                cardBattleTab.visibility = View.GONE
                return
            }

            // found something
            val names = intent.getStringArrayListExtra("NAMES") ?: return
            val ids = intent.getIntegerArrayListExtra("IDS")
            val type1s = intent.getStringArrayListExtra("TYPE1S") ?: return
            val type2s = intent.getStringArrayListExtra("TYPE2S") ?: return

            val scannedList = ArrayList<Pokemon>()
            for (i in names.indices) {
                val t1 = PokemonType.fromString(type1s[i])
                val t2 = PokemonType.fromString(type2s[i])
                val pId = if (ids != null && ids.size > i) ids[i] else 0
                scannedList.add(Pokemon(names[i], pId, t1, t2, null))
            }

            if (scannedList.isNotEmpty()) {
                battleList = scannedList

                // prep the tab ui (text/color) just in case
                val combinedNames = battleList.joinToString(" & ") { it.name.replaceFirstChar { c -> c.uppercase() } }
                tvBattleTabText.text = combinedNames

                val typeColor = battleList[0].type1.colorHex
                cardBattleTab.setCardBackgroundColor(typeColor)
                tvBattleTabText.setTextColor(Color.WHITE)

                // don't show tab if we're already looking at the battle screen
                if (containerBattle.visibility == View.VISIBLE && isViewingBattleMode) {
                    cardBattleTab.visibility = View.GONE
                } else {
                    cardBattleTab.visibility = View.VISIBLE
                }

                // update main screen if we're live
                if (isViewingBattleMode) {
                    val currentNames = navigationList.map { it.name }
                    val newNames = battleList.map { it.name }

                    if (currentNames != newNames) {
                        Log.d("DualDex", "Live Update: $newNames")
                        navigationList = battleList
                        selectedIndex = 0
                        refreshDisplay()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadTheme(this)

        pokedexHelper = PokedexHelper(this)
        allPokemonCache = pokedexHelper.getAllPokemon()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        searchContainer = findViewById(R.id.searchContainer)

        // grab views
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

        // load up the db
        val dbHelper = PokedexHelper(this)
        allPokemonCache = dbHelper.getAllPokemon()
        updateUI("", 0, PokemonType.NORMAL, PokemonType.NORMAL)

        // setup list
        rvList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter = PokemonAdapter(allPokemonCache) { selectedPokemon ->
            // clicked a dex item, show static info
            isViewingBattleMode = false
            navigationList = allPokemonCache

            selectedIndex = navigationList.indexOfFirst {
                it.name == selectedPokemon.name && it.variantLabel == selectedPokemon.variantLabel
            }
            if (selectedIndex == -1) selectedIndex = 0

            switchToBattleModeView()
            refreshDisplay()

            // show tab if we have a background battle running
            if (battleList.isNotEmpty()) {
                cardBattleTab.visibility = View.VISIBLE
            }
        }
        rvList.adapter = adapter

        // load gen number
        val prefs = getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)
        val savedGenName = prefs.getString("SELECTED_GEN", TypeMatchup.Gen.GEN_6_PLUS.name)
        currentGen = try {
            TypeMatchup.Gen.valueOf(savedGenName ?: "GEN_6_PLUS")
        } catch (e: Exception) {
            TypeMatchup.Gen.GEN_6_PLUS
        }

        // tab click
        cardBattleTab.setOnClickListener {
            // clicked the tab, switch to live scan data
            if (battleList.isNotEmpty()) {
                isViewingBattleMode = true
                navigationList = battleList
                selectedIndex = 0

                switchToBattleModeView()
                refreshDisplay()
            }
        }

        // variant toggle button
        btnVariantToggle.setOnClickListener {
            cycleVariant()
        }

        // settings button
        searchContainer.setEndIconOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // listeners
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { adapter.filter(s.toString().trim()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        applyThemeColors()

        btnBack.setOnClickListener { switchToListMode() }
        navLeftBtn.setOnClickListener { cycleSelection(-1) }
        navRightBtn.setOnClickListener { cycleSelection(1) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvName.post {
            val captureIntent = projectionManager.createScreenCaptureIntent()
            startMediaProjection.launch(captureIntent)
        }
    }

    private fun refreshDisplay() {
        if (navigationList.isEmpty()) return

        if (selectedIndex < 0) selectedIndex = 0
        if (selectedIndex >= navigationList.size) selectedIndex = navigationList.size - 1

        val current = navigationList[selectedIndex]
        updateUI(current.name, current.id, current.type1, current.type2 ?: PokemonType.UNKNOWN, current.variantLabel)
        updateNavigationHeader()
    }

    private fun cycleVariant() {
        if (currentVariantList.size <= 1) return

        currentVariantIndex = (currentVariantIndex + 1) % currentVariantList.size
        val p = currentVariantList[currentVariantIndex]
        updateUI(p.name, p.id, p.type1, p.type2 ?: PokemonType.UNKNOWN, p.variantLabel)
    }

    private fun cycleSelection(direction: Int) {
        if (navigationList.size <= 1) return

        val newIndex = selectedIndex + direction

        if (isViewingBattleMode) {
            // battle mode doesn't loop
            if (newIndex >= 0 && newIndex < navigationList.size) {
                selectedIndex = newIndex
                refreshDisplay()
            }
        } else {
            // dex mode loops around
            if (newIndex < 0) {
                selectedIndex = navigationList.size - 1
            } else if (newIndex >= navigationList.size) {
                selectedIndex = 0
            } else {
                selectedIndex = newIndex
            }
            refreshDisplay()
        }
    }

    private fun updateNavigationHeader() {
        if (navigationList.size <= 1) {
            navLeftBtn.visibility = View.INVISIBLE
            navRightBtn.visibility = View.INVISIBLE
            return
        }

        // left arrow
        if (isViewingBattleMode && selectedIndex <= 0) {
            navLeftBtn.visibility = View.INVISIBLE
        } else {
            navLeftBtn.visibility = View.VISIBLE
            val prevIndex = if (selectedIndex - 1 < 0) navigationList.size - 1 else selectedIndex - 1
            tvPrevName.text = navigationList[prevIndex].name.replaceFirstChar { it.uppercase() }
        }

        // right arrow
        if (isViewingBattleMode && selectedIndex >= navigationList.size - 1) {
            navRightBtn.visibility = View.INVISIBLE
        } else {
            navRightBtn.visibility = View.VISIBLE
            val nextIndex = if (selectedIndex + 1 >= navigationList.size) 0 else selectedIndex + 1
            tvNextName.text = navigationList[nextIndex].name.replaceFirstChar { it.uppercase() }
        }
    }

    private fun updateUI(name: String, id: Int, rawType1: PokemonType, rawType2: PokemonType, startVariant: String? = null) {
        val theme = ThemeManager.currentTheme

        // check if new pokemon or variant
        if (currentVariantList.isEmpty() || !currentVariantList[0].name.equals(name, ignoreCase = true)) {
            currentVariantList = pokedexHelper.getVariantsFor(name)

            val targetIndex = currentVariantList.indexOfFirst { it.variantLabel == startVariant }
            currentVariantIndex = if (targetIndex != -1) targetIndex else 0
        }

        val activePokemon = if (currentVariantList.isNotEmpty() && currentVariantList.indices.contains(currentVariantIndex)) {
            currentVariantList[currentVariantIndex]
        } else {
            Pokemon(name, id, rawType1, rawType2, startVariant)
        }

        val displayType1 = activePokemon.type1
        val displayType2 = activePokemon.type2 ?: PokemonType.UNKNOWN
        val displayBaseColor = displayType1.colorHex

        tvName.text = activePokemon.name.replaceFirstChar { it.uppercase() }

        // show variant name in id tag
        if (activePokemon.variantLabel != null) {
            tvId.text = String.format("#%03d (%s)", activePokemon.id, activePokemon.variantLabel)
        } else {
            tvId.text = String.format("#%03d", activePokemon.id)
        }

        tvName.setTextColor(theme.headerTextColor)
        tvId.setTextColor(theme.subTextColor)

        // header card
        val headerParams = cardHeader.layoutParams as android.view.ViewGroup.MarginLayoutParams
        headerParams.setMargins(32, 32, 32, 16)
        cardHeader.layoutParams = headerParams

        val headerColor = when (theme.id) {
            "red" -> Color.TRANSPARENT
            "dynamic", "magical" -> displayBaseColor.blendWithWhite(0.7f)
            "oled" -> Color.BLACK
            else -> theme.windowBackground
        }

        cardHeader.setCardBackgroundColor(headerColor)
        cardHeader.radius = theme.cardCornerRadius
        cardHeader.cardElevation = if (theme.id == "dynamic") 8f else 0f

        // battle view screen
        val battleParams = cardData.layoutParams as android.view.ViewGroup.MarginLayoutParams
        battleParams.setMargins(32, 0, 32, 32)
        cardData.layoutParams = battleParams
        cardData.radius = theme.cardCornerRadius
        val innerLayout = cardData.getChildAt(0)
        innerLayout.background = null

        if (theme.isRetroScreen) {
            containerBattle.background = ThemeManager.createScanlineDrawable(this, withBorder = true)
            containerBattle.setPadding(24, 24, 24, 24)
            cardData.setCardBackgroundColor(Color.TRANSPARENT)
            cardData.cardElevation = 0f
            innerLayout.setPadding(0, 16, 0, 16)
        } else {
            if (theme.isGradientBg) {
                containerBattle.background = null
            } else if (theme.id == "dynamic") {
                val pastelColor = displayBaseColor.blendWithWhite(0.9f)
                containerBattle.setBackgroundColor(pastelColor)
            } else {
                containerBattle.setBackgroundColor(theme.windowBackground)
            }
            containerBattle.setPadding(0, 0, 0, 0)
            cardData.setCardBackgroundColor(theme.gridBackgroundColor)
            cardData.cardElevation = if (theme.id == "dynamic") 4f else 0f
            innerLayout.setPadding(16, 16, 16, 16)
        }

        // variant button
        if (currentVariantList.size > 1) {
            btnVariantToggle.visibility = View.VISIBLE

            val nextIndex = (currentVariantIndex + 1) % currentVariantList.size
            val nextForm = currentVariantList[nextIndex]
            val label = nextForm.variantLabel ?: "Normal"

            btnVariantToggle.text = "Switch to $label"

            btnVariantToggle.setTextColor(theme.labelTextColor)
            btnVariantToggle.setStrokeColor(android.content.res.ColorStateList.valueOf(theme.labelTextColor))
        } else {
            btnVariantToggle.visibility = View.GONE
        }

        // labels
        lblWeak.setTextColor(theme.labelTextColor)
        lblResist.setTextColor(theme.labelTextColor)
        lblWeak.setPadding(0, 0, 0, 0)
        gridWeak.setPadding(0, 0, 0, 0)
        lblResist.setPadding(0, 0, 0, 0)
        gridResist.setPadding(0, 0, 0, 0)

        // navigation
        val buttonColor = if (theme.id == "red") theme.searchBoxColor else headerColor
        btnBack.backgroundTintList = android.content.res.ColorStateList.valueOf(buttonColor)
        val arrowColor = if (theme.id == "oled" || theme.id == "red") Color.WHITE else theme.headerTextColor
        btnBack.setColorFilter(arrowColor)

        val navTextColor = if (theme.id == "oled") Color.LTGRAY else theme.subTextColor
        tvPrevName.setTextColor(navTextColor)
        tvNextName.setTextColor(navTextColor)

        fun recolorNav(layout: LinearLayout) {
            for (i in 0 until layout.childCount) {
                val v = layout.getChildAt(i)
                if (v is TextView) v.setTextColor(navTextColor)
            }
        }
        recolorNav(navLeftBtn)
        recolorNav(navRightBtn)

        layoutTypes.removeAllViews()
        addFullWidthTypeBadge(layoutTypes, displayType1)
        if (displayType2 != PokemonType.UNKNOWN) {
            addFullWidthTypeBadge(layoutTypes, displayType2)
        }

        val weaknessList = ArrayList<MatchupData>()
        val resistanceList = ArrayList<MatchupData>()

        for (attacker in PokemonType.entries) {
            if (attacker == PokemonType.UNKNOWN) continue
            val mult = TypeMatchup.getMultiplier(attacker, displayType1, currentGen) *
                    (if (displayType2 != PokemonType.UNKNOWN) TypeMatchup.getMultiplier(attacker, displayType2, currentGen) else 1.0)

            if (mult > 1.0) weaknessList.add(MatchupData(attacker, mult))
            if (mult < 1.0) resistanceList.add(MatchupData(attacker, mult))
        }

        weaknessList.sortByDescending { it.multiplier }
        resistanceList.sortBy { it.multiplier }

        populateSmartGrid(gridWeak, weaknessList)
        populateSmartGrid(gridResist, resistanceList)

        lblWeak.visibility = if (weaknessList.isNotEmpty()) View.VISIBLE else View.GONE
        gridWeak.visibility = if (weaknessList.isNotEmpty()) View.VISIBLE else View.GONE
        lblResist.visibility = if (resistanceList.isNotEmpty()) View.VISIBLE else View.GONE
        gridResist.visibility = if (resistanceList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun startScreenCaptureService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "START_CAPTURE"
            putExtra("RESULT_CODE", resultCode)
            putExtra("DATA", data)
        }
        startForegroundService(intent)
    }

    private fun switchToBattleModeView() {
        if (containerBattle.visibility == View.VISIBLE) {
            // already visible, hide tab cause we're looking at it
            cardBattleTab.visibility = View.GONE
            return
        }

        containerPokedex.visibility = View.GONE
        containerBattle.visibility = View.VISIBLE
        cardBattleTab.visibility = View.GONE // hide tab on open
        btnBack.visibility = View.VISIBLE

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun switchToListMode() {
        containerPokedex.visibility = View.VISIBLE
        containerBattle.visibility = View.GONE
        btnBack.visibility = View.GONE

        // show tab if battle is still active
        if (battleList.isNotEmpty()) {
            cardBattleTab.visibility = View.VISIBLE
        }
    }

    private fun populateSmartGrid(container: LinearLayout, list: List<MatchupData>) {
        container.removeAllViews()
        val rows = list.chunked(3)
        for (rowItems in rows) {
            val rowLayout = LinearLayout(this)
            rowLayout.orientation = LinearLayout.HORIZONTAL
            rowLayout.weightSum = rowItems.size.toFloat()
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 16)
            rowLayout.layoutParams = params
            for (data in rowItems) {
                val badge = createWeaknessBadgeView(data.type, data.multiplier)
                val itemParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                itemParams.setMargins(8, 0, 8, 0)
                rowLayout.addView(badge, itemParams)
            }
            container.addView(rowLayout)
        }
    }

    private fun applyThemeColors() {
        val theme = ThemeManager.currentTheme
        val root = findViewById<View>(R.id.main_root)

        // background
        if (theme.isGradientBg) {  // magical theme
            root.background = ThemeManager.StarryDrawable()
            window.statusBarColor = "#E1BEE7".toColorInt()
            containerPokedex.setBackgroundColor(Color.TRANSPARENT)
        } else {
            root.background = null
            root.setBackgroundColor(theme.windowBackground)
            containerPokedex.setBackgroundColor(theme.contentBackground)
            window.statusBarColor = theme.windowBackground
        }

        // special pokedex theme
        if (theme.isRetroScreen) {
            containerPokedex.background = ThemeManager.createScanlineDrawable(this, withBorder = true)
            containerPokedex.setPadding(24, 24, 24, 24)
        } else {
            if (!theme.isGradientBg) {
                containerPokedex.setBackgroundColor(theme.contentBackground)
            }
            containerPokedex.setPadding(0, 0, 0, 0)
        }

        // search bar
        val searchBox = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.searchContainer)
        val etSearch = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)

        val iconColor = if (theme.id == "oled" || theme.id == "red") Color.WHITE else Color.parseColor("#5D4037")

        searchBox.boxBackgroundColor = theme.searchBoxColor
        searchBox.setBoxStrokeColorStateList(android.content.res.ColorStateList.valueOf(theme.searchStrokeColor))
        searchBox.defaultHintTextColor = android.content.res.ColorStateList.valueOf(theme.searchHintColor)
        searchBox.setEndIconTintList(android.content.res.ColorStateList.valueOf(iconColor))
        etSearch.setTextColor(theme.searchTextColor)
        etSearch.setHintTextColor(theme.searchHintColor)

        searchBox.setBoxCornerRadii(theme.searchCornerRadius, theme.searchCornerRadius, theme.searchCornerRadius, theme.searchCornerRadius)
        searchBox.boxStrokeWidth = theme.searchStrokeWidth
        searchBox.boxStrokeWidthFocused = theme.searchStrokeWidth + 2

        val params = searchBox.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val density = resources.displayMetrics.density
        val marginPx = (theme.searchMarginHorizontal * density).toInt()
        params.setMargins(marginPx, marginPx / 2, marginPx, marginPx / 2)
        searchBox.layoutParams = params

        if (::adapter.isInitialized) {
            adapter.updateSettings(currentGen, theme)
        }
        if (navigationList.isNotEmpty()) refreshDisplay()
    }
    private fun createWeaknessBadgeView(type: PokemonType, mult: Double): View {
        val inflater = android.view.LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.badge_weakness, null, false)
        val tvType = view.findViewById<TextView>(R.id.tvBadgeType)
        val tvMult = view.findViewById<TextView>(R.id.tvBadgeMult)
        tvType.text = type.displayName
        val multText = when(mult) {
            0.5 -> "½"
            0.25 -> "¼"
            0.0 -> "0"
            else -> mult.toInt().toString()
        }
        tvMult.text = "× $multText"
        tvType.setBackgroundColor(type.colorHex)
        tvMult.setBackgroundColor(type.colorHex)
        return view
    }

    private fun addFullWidthTypeBadge(container: LinearLayout, type: PokemonType) {
        val tv = TextView(this)
        tv.text = type.displayName.uppercase()
        tv.setTextColor(Color.WHITE)
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
        tv.gravity = android.view.Gravity.CENTER
        tv.textSize = 14f
        tv.setBackgroundColor(type.colorHex)
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        params.setMargins(1, 0, 1, 0)
        container.addView(tv, params)
    }

    private fun Int.blendWithWhite(ratio: Float): Int {
        return ColorUtils.blendARGB(this, Color.WHITE, ratio)
    }

    override fun onResume() {
        super.onResume()
        reloadSettings()
        // register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(pokemonReceiver, android.content.IntentFilter("POKEMON_DETECTED"))

        // tell scanner to wake up
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "RESUME"
        }
        // using startService to send command to existing service
        startService(intent)
    }

    override fun onPause() {
        super.onPause()
        // unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pokemonReceiver)

        // pause scanner to save battery
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "PAUSE"
        }
        startService(intent)
    }

    private fun reloadSettings() {
        val prefs = getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)

        val savedGenName = prefs.getString("SELECTED_GEN", TypeMatchup.Gen.GEN_6_PLUS.name)
        val newGen = try {
            TypeMatchup.Gen.valueOf(savedGenName ?: "GEN_6_PLUS")
        } catch (e: Exception) {
            TypeMatchup.Gen.GEN_6_PLUS
        }
        currentGen = newGen

        ThemeManager.loadTheme(this)
        applyThemeColors()

        // update adapter to fix generation type differences
        if (::adapter.isInitialized) {
            adapter.updateSettings(currentGen, ThemeManager.currentTheme)
        }

        // update individual pokemon view
        if (navigationList.isNotEmpty()) {
            refreshDisplay()
        }
    }
}