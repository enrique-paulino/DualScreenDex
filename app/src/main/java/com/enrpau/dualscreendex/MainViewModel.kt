package com.enrpau.dualscreendex

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.enrpau.dualscreendex.data.RomManager
import com.enrpau.dualscreendex.data.RomProfile

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = PokemonRepository(application)

    private val resetHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val resetRunnable = Runnable {
        _isBattleMode.value = false
        battleList = emptyList()
        _showBattleTab.value = false
    }

    private val _displayedPokemon = MutableLiveData<Pokemon>()
    val displayedPokemon: LiveData<Pokemon> = _displayedPokemon

    private val _pokedexList = MutableLiveData<List<Pokemon>>()
    val pokedexList: LiveData<List<Pokemon>> = _pokedexList

    private val _isBattleMode = MutableLiveData(false)
    val isBattleMode: LiveData<Boolean> = _isBattleMode

    private val _showBattleTab = MutableLiveData(false)
    val showBattleTab: LiveData<Boolean> = _showBattleTab
    val battleTabText = MutableLiveData<String>()
    val battleTabColor = MutableLiveData<Int>()

    private var isLiveUpdateActive = true

    val isPrevButtonVisible = MutableLiveData<Boolean>()
    val isNextButtonVisible = MutableLiveData<Boolean>()

    private var fullList = repository.getAllPokemon()
    private var currentFilteredList = fullList
    private var battleList: List<Pokemon> = emptyList()
    private var userDismissedBattle = false

    val weaknessList = MutableLiveData<List<MainActivity.MatchupData>>()
    val resistanceList = MutableLiveData<List<MainActivity.MatchupData>>()

    private var selectedIndex = 0
    private var currentVariantList: List<Pokemon> = emptyList()
    private var currentVariantIndex = 0
    val prevPokemonName = MutableLiveData<String>()
    val nextPokemonName = MutableLiveData<String>()

    private var currentMechanics: RomProfile.Mechanics = RomManager.currentProfile.baseMechanics

    init {
        _pokedexList.value = fullList
        if (fullList.isNotEmpty()) selectPokemon(fullList[0])
    }

    fun refreshSettings() {
        repository.reloadDatabase()

        val newList = repository.getAllPokemon()
        _pokedexList.value = newList

        currentVariantList = emptyList()

        val profile = RomManager.currentProfile
        currentMechanics = profile.baseMechanics

        val current = displayedPokemon.value
        if (current != null) {
            val newVersion = newList.find { it.name.equals(current.name, true) && it.variantLabel == current.variantLabel }
                ?: newList.find { it.id == current.id }

            if (newVersion != null) {
                _displayedPokemon.value = newVersion!!
                calculateMatchups(newVersion)
            }
        }
    }

    fun onSearchQuery(query: String) {
        currentFilteredList = repository.filterPokemon(query)
        _pokedexList.value = currentFilteredList
    }

    fun onPokemonSelectedFromList(pokemon: Pokemon) {
        isLiveUpdateActive = false
        selectedIndex = currentFilteredList.indexOfFirst { it.name == pokemon.name }
        if (selectedIndex == -1) selectedIndex = 0
        selectPokemon(pokemon)
        _isBattleMode.value = true
    }

    fun onScanResult(names: ArrayList<String>?, ids: ArrayList<Int>?, t1s: ArrayList<String>?, t2s: ArrayList<String>?) {
        resetHandler.removeCallbacks(resetRunnable)

        if (names.isNullOrEmpty()) {
            if (_isBattleMode.value != true) {
                _showBattleTab.value = false
                battleList = emptyList()
            } else {
                resetHandler.postDelayed(resetRunnable, 5000)
            }
            return
        }

        val scanned = ArrayList<Pokemon>()
        val allPokemon = repository.getAllPokemon()

        for (i in names.indices) {
            val pId = ids?.getOrNull(i) ?: 0
            val t1 = PokemonType.fromString(t1s?.getOrNull(i) ?: "unknown")

            val match = allPokemon.firstOrNull {
                it.id == pId && it.type1 == t1
            } ?: allPokemon.firstOrNull { it.id == pId }

            if (match != null) {
                scanned.add(match)
            } else {
                scanned.add(Pokemon(names[i], pId, t1, PokemonType.UNKNOWN, null))
            }
        }

        battleList = scanned

        val label = battleList.joinToString(" & ") { it.name.replaceFirstChar { c -> c.uppercase() } }
        battleTabText.value = label
        if (battleList.isNotEmpty()) {
            battleTabColor.value = battleList[0].type1.colorHex
        }

        if (_isBattleMode.value == true) {
            if (isLiveUpdateActive) {
                if (battleList.isNotEmpty()) {
                    selectedIndex = 0
                    selectPokemon(battleList[0])
                }
            }
            _showBattleTab.value = false

        } else {
            _showBattleTab.value = true
        }
    }

    fun onBattleTabClicked() {
        if (battleList.isNotEmpty()) {
            isLiveUpdateActive = true
            _isBattleMode.value = true
            selectedIndex = 0
            selectPokemon(battleList[0])
        }
    }

    fun onBackToListClicked() {
        _isBattleMode.value = false
        _showBattleTab.value = battleList.isNotEmpty()
        userDismissedBattle = true
    }

    fun onNextClicked() = cycleSelection(1)
    fun onPrevClicked() = cycleSelection(-1)

    fun onVariantToggleClicked() {
        if (currentVariantList.size <= 1) return
        currentVariantIndex = (currentVariantIndex + 1) % currentVariantList.size

        val target = currentVariantList[currentVariantIndex]
        _displayedPokemon.value = target
        calculateMatchups(target)
    }

    private fun selectPokemon(pokemon: Pokemon) {
        if (currentVariantList.isEmpty() || !currentVariantList[0].name.equals(pokemon.name, true)) {
            currentVariantList = repository.getVariantsFor(pokemon.name)
        }

        if (currentVariantList.isNotEmpty()) {
            val matchIndex = currentVariantList.indexOfFirst { it.variantLabel == pokemon.variantLabel }
            if (matchIndex != -1) {
                currentVariantIndex = matchIndex
            } else {
                currentVariantIndex = if (currentVariantList.isNotEmpty()) 0 else 0
            }
        }

        val target = if (currentVariantList.isNotEmpty() && currentVariantIndex < currentVariantList.size) {
            currentVariantList[currentVariantIndex]
        } else {
            pokemon
        }
        _displayedPokemon.value = target
        calculateMatchups(target)

        val activeList = if (isLiveUpdateActive) battleList else currentFilteredList

        if (activeList.isNotEmpty()) {
            val currentIndex = activeList.indexOfFirst {
                it.name == target.name && it.variantLabel == target.variantLabel
            }

            if (currentIndex != -1) {
                fun getDisplayName(p: Pokemon): String {
                    val name = p.name.replaceFirstChar { it.uppercase() }
                    return if (p.variantLabel != null) "$name (${p.variantLabel})" else name
                }

                if (isLiveUpdateActive) {
                    val hasPrev = currentIndex > 0
                    val hasNext = currentIndex < activeList.size - 1
                    isPrevButtonVisible.value = hasPrev
                    isNextButtonVisible.value = hasNext
                    if (hasPrev) prevPokemonName.value = getDisplayName(activeList[currentIndex - 1])
                    if (hasNext) nextPokemonName.value = getDisplayName(activeList[currentIndex + 1])

                } else {
                    val showButtons = activeList.size > 1
                    isPrevButtonVisible.value = showButtons
                    isNextButtonVisible.value = showButtons

                    if (showButtons) {
                        val prevIndex = if (currentIndex - 1 < 0) activeList.size - 1 else currentIndex - 1
                        val nextIndex = if (currentIndex + 1 >= activeList.size) 0 else currentIndex + 1
                        prevPokemonName.value = getDisplayName(activeList[prevIndex])
                        nextPokemonName.value = getDisplayName(activeList[nextIndex])
                    }
                }
            }
        } else {
            isPrevButtonVisible.value = false
            isNextButtonVisible.value = false
        }
    }

    private fun calculateMatchups(pokemon: Pokemon) {
        val weak = ArrayList<MainActivity.MatchupData>()
        val resist = ArrayList<MainActivity.MatchupData>()

        for (attacker in PokemonType.entries) {
            if (attacker == PokemonType.UNKNOWN) continue

            val (t1, t2) = GenerationHelper.getGenSpecificTypes(pokemon, currentMechanics)

            val mult = TypeMatchup.getMultiplier(attacker, t1, getApplication()) *
                    (if (t2 != PokemonType.UNKNOWN) TypeMatchup.getMultiplier(attacker, t2, getApplication()) else 1.0)

            if (mult > 1.0) weak.add(MainActivity.MatchupData(attacker, mult))
            if (mult < 1.0) resist.add(MainActivity.MatchupData(attacker, mult))
        }

        weak.sortByDescending { it.multiplier }
        resist.sortBy { it.multiplier }

        weaknessList.value = weak
        resistanceList.value = resist
    }

    private fun cycleSelection(direction: Int) {
        val activeList = if (isLiveUpdateActive) battleList else currentFilteredList
        if (activeList.isEmpty()) return

        var newIndex = selectedIndex + direction

        if (isLiveUpdateActive) {
            if (newIndex in activeList.indices) {
                selectedIndex = newIndex
                selectPokemon(activeList[selectedIndex])
            }
        } else {
            if (newIndex < 0) newIndex = activeList.size - 1
            if (newIndex >= activeList.size) newIndex = 0
            selectedIndex = newIndex
            selectPokemon(activeList[selectedIndex])
        }
    }

    fun hasVariants(): Boolean = currentVariantList.size > 1
    fun getNextVariantName(): String {
        if (currentVariantList.isEmpty()) return ""
        val nextIdx = (currentVariantIndex + 1) % currentVariantList.size
        return currentVariantList[nextIdx].variantLabel ?: "Normal"
    }
}