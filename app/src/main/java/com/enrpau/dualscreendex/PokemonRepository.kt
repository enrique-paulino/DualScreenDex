package com.enrpau.dualscreendex

import android.content.Context
import com.enrpau.dualscreendex.data.CsvParsers
import com.enrpau.dualscreendex.data.RomManager

class PokemonRepository(private val context: Context) {

    private var allPokemon: List<Pokemon> = emptyList()

    init {
        RomManager.initialize(context)
        reloadDatabase()
    }

    fun reloadDatabase() {
        val profile = RomManager.currentProfile

        val baseList = CsvParsers.parsePokedex(context, profile)

        val regionalList = if (profile.hasRegionals()) {
            CsvParsers.parseRegionalForms(context, profile)
        } else {
            emptyList()
        }

        val nameMap = baseList.associate { it.id to it.name }

        val baseMap = baseList.associateBy { it.id }

        val processedRegionals = regionalList.map { p ->
            val basePokemon = baseMap[p.id]
            val baseName = basePokemon?.name ?: "Unknown"
            val japaneseKana = basePokemon?.japaneseKana

            Pokemon(baseName, p.id, p.type1, p.type2, p.variantLabel, japaneseKana)
        }


        allPokemon = (baseList + processedRegionals).sortedBy { it.id }
        println("DEBUG FIRST POKEMON: ${allPokemon.firstOrNull()?.name} - ${allPokemon.firstOrNull()?.japaneseKana}")

    }

    fun getAllPokemon(): List<Pokemon> = allPokemon

    fun getVariantsFor(name: String): List<Pokemon> {
        val target = allPokemon.find {
            it.name.equals(name, true) ||
            it.japaneseKana?.equals(name) == true
        } ?: return emptyList()


        return allPokemon.filter { it.id == target.id }
    }

    fun filterPokemon(query: String): List<Pokemon> {
        if (query.isBlank()) return allPokemon
        return allPokemon.filter {
            it.name.contains(query, ignoreCase = true) ||
            (it.japaneseKana?.contains(query) == true) ||
            (it.variantLabel?.contains(query, ignoreCase = true) == true)
        }

    }
}