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
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    // ui references
    private lateinit var tvName: TextView
    private lateinit var tvId: TextView
    private lateinit var layoutTypes: LinearLayout
    private lateinit var gridWeak: LinearLayout
    private lateinit var gridResist: LinearLayout
    private lateinit var lblWeak: TextView
    private lateinit var lblResist: TextView

    private lateinit var navLeftBtn: LinearLayout
    private lateinit var navRightBtn: LinearLayout
    private lateinit var tvPrevName: TextView
    private lateinit var tvNextName: TextView

    private lateinit var containerBattle: View
    private lateinit var cardHeader: androidx.cardview.widget.CardView
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

    // state tracking
    private var navigationList: List<Pokemon> = emptyList()
    private var selectedIndex: Int = 0

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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // grab views
        tvName = findViewById(R.id.tvTargetName)
        tvId = findViewById(R.id.tvTargetId)
        layoutTypes = findViewById(R.id.layoutTargetTypes)
        gridWeak = findViewById(R.id.gridWeak)
        gridResist = findViewById(R.id.gridResist)
        lblWeak = findViewById(R.id.lblWeak)
        lblResist = findViewById(R.id.lblResist)
        navLeftBtn = findViewById(R.id.navLeftContainer)
        navRightBtn = findViewById(R.id.navRightContainer)
        tvPrevName = findViewById(R.id.tvPrevName)
        tvNextName = findViewById(R.id.tvNextName)

        containerBattle = findViewById(R.id.containerBattle)
        cardHeader = findViewById(R.id.cardHeader)
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

            selectedIndex = navigationList.indexOfFirst { it.name == selectedPokemon.name }
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

        // gen filter button
        searchContainer.setEndIconOnClickListener {
            showGenerationDialog()
        }

        // listeners
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { adapter.filter(s.toString().trim()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
        updateUI(current.name, current.id, current.type1, current.type2 ?: PokemonType.UNKNOWN)
        updateNavigationHeader()
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

    private fun updateUI(name: String, id: Int, rawType1: PokemonType, rawType2: PokemonType) {
        tvName.text = name.replaceFirstChar { it.uppercase() }
        tvId.text = String.format("#%03d", id)

        val tempPokemon = Pokemon(name, id, rawType1, rawType2, null)

        val (type1, type2) = GenerationHelper.getGenSpecificTypes(tempPokemon, currentGen)

        val baseColor = type1.colorHex
        val bgColor = baseColor.blendWithWhite(0.85f)
        val headerColor = baseColor.blendWithWhite(0.65f)

        containerBattle.setBackgroundColor(bgColor)
        cardHeader.setCardBackgroundColor(headerColor)
        window.statusBarColor = headerColor

        btnBack.backgroundTintList = android.content.res.ColorStateList.valueOf(headerColor)
        btnBack.setColorFilter("#5D4037".toColorInt())

        layoutTypes.removeAllViews()
        addFullWidthTypeBadge(layoutTypes, type1)
        if (type2 != PokemonType.UNKNOWN) {
            addFullWidthTypeBadge(layoutTypes, type2)
        }

        val weaknessList = ArrayList<MatchupData>()
        val resistanceList = ArrayList<MatchupData>()

        for (attacker in PokemonType.entries) {
            if (attacker == PokemonType.UNKNOWN) continue

            val mult = TypeMatchup.getMultiplier(attacker, type1, currentGen) *
                    (if (type2 != PokemonType.UNKNOWN) TypeMatchup.getMultiplier(attacker, type2, currentGen) else 1.0)

            if (mult > 1.0) weaknessList.add(MatchupData(attacker, mult))
            if (mult < 1.0) resistanceList.add(MatchupData(attacker, mult))
        }

        weaknessList.sortByDescending { it.multiplier }
        resistanceList.sortBy { it.multiplier }

        populateSmartGrid(gridWeak, weaknessList)
        populateSmartGrid(gridResist, resistanceList)

        if (weaknessList.isNotEmpty()) {
            lblWeak.visibility = View.VISIBLE
            gridWeak.visibility = View.VISIBLE
        } else {
            lblWeak.visibility = View.GONE
            gridWeak.visibility = View.GONE
        }

        if (resistanceList.isNotEmpty()) {
            lblResist.visibility = View.VISIBLE
            gridResist.visibility = View.VISIBLE
        } else {
            lblResist.visibility = View.GONE
            gridResist.visibility = View.GONE
        }
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

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

    private fun showGenerationDialog() {
        val gens = TypeMatchup.Gen.entries.toTypedArray()
        val genNames = gens.map {
            when(it) {
                TypeMatchup.Gen.GEN_1 -> "Gen 1 (Red/Blue)"
                TypeMatchup.Gen.GEN_2_5 -> "Gen 2-5 (Gold - Black/White)"
                TypeMatchup.Gen.GEN_6_PLUS -> "Gen 6+ (X/Y - Scarlet/Violet)"
            }
        }.toTypedArray()

        val checkedItem = gens.indexOf(currentGen)

        android.app.AlertDialog.Builder(this)
            .setTitle("Select Game Generation")
            .setSingleChoiceItems(genNames, checkedItem) { dialog, which ->
                val selected = gens[which]

                if (currentGen != selected) {
                    currentGen = selected

                    // saving gen preference
                    val prefs = getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)
                    prefs.edit { putString("SELECTED_GEN", currentGen.name) }

                    adapter.updateGeneration(currentGen)

                    if (navigationList.isNotEmpty()) {
                        refreshDisplay()
                    }

                    Toast.makeText(this, "Matchups updated for ${genNames[which]}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
}