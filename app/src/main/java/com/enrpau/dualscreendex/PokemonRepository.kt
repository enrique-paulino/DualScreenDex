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

        val processedRegionals = regionalList.map { p ->
            val rawName = nameMap[p.id] ?: "Unknown"
            val baseName = rawName.replaceFirstChar { it.uppercase() }
            // adapter takes care of region suffix
            val fullName = baseName

            Pokemon(fullName, p.id, p.type1, p.type2, p.variantLabel)
        }

        allPokemon = (baseList + processedRegionals).sortedBy { it.id }
    }

    fun getAllPokemon(): List<Pokemon> = allPokemon

    fun getVariantsFor(name: String): List<Pokemon> {
        val target = allPokemon.find { it.name.equals(name, true) } ?: return emptyList()

        return allPokemon.filter { it.id == target.id }
    }

    fun filterPokemon(query: String): List<Pokemon> {
        if (query.isBlank()) return allPokemon
        return allPokemon.filter {
            it.name.contains(query, ignoreCase = true) ||
                    (it.variantLabel?.contains(query, ignoreCase = true) == true)
        }
    }
}