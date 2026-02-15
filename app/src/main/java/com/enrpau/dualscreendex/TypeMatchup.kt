package com.enrpau.dualscreendex

import android.content.Context
import android.util.Log
import com.enrpau.dualscreendex.data.CsvParsers
import com.enrpau.dualscreendex.data.RomManager
import com.enrpau.dualscreendex.data.RomProfile

object TypeMatchup {

    private var customChart: Map<String, Double>? = null
    private var lastLoadedProfileId: String? = null

    fun getMultiplier(attacker: PokemonType, defender: PokemonType, context: Context): Double {
        val profile = RomManager.currentProfile

        ensureChartLoaded(context, profile)

        val key = "${attacker.name.uppercase()}_${defender.name.uppercase()}"
        val baseMultiplier = customChart?.get(key) ?: 1.0

        if (!profile.isBuiltIn && profile.matchupFilePath != null) {
            return baseMultiplier
        }

        return applyMechanics(attacker, defender, baseMultiplier, profile.baseMechanics)
    }

    private fun ensureChartLoaded(context: Context, profile: RomProfile) {
        if (lastLoadedProfileId != profile.id || customChart == null) {
            Log.d("TypeMatchup", "Reloading Chart for: ${profile.name}")
            customChart = CsvParsers.parseMatchupChart(context, profile)
            lastLoadedProfileId = profile.id
        }
    }

    private fun applyMechanics(atk: PokemonType, def: PokemonType, current: Double, mechanics: RomProfile.Mechanics): Double {
        when (mechanics) {
            RomProfile.Mechanics.GEN_1 -> {
                if (isTypeMissingInGen1(atk) || isTypeMissingInGen1(def)) return 1.0
                if (atk == PokemonType.GHOST && def == PokemonType.PSYCHIC) return 0.0
                if (atk == PokemonType.POISON && def == PokemonType.BUG) return 2.0
                if (atk == PokemonType.BUG && def == PokemonType.POISON) return 2.0
                if (atk == PokemonType.ICE && def == PokemonType.FIRE) return 1.0
            }
            RomProfile.Mechanics.GEN_2_TO_5 -> {
                if (def == PokemonType.STEEL && (atk == PokemonType.GHOST || atk == PokemonType.DARK)) {
                    return 0.5
                }
            }
            else -> return current
        }
        return current
    }

    private fun isTypeMissingInGen1(type: PokemonType): Boolean {
        return type == PokemonType.DARK || type == PokemonType.STEEL || type == PokemonType.FAIRY
    }
}