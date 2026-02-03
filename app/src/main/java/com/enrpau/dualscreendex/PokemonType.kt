package com.enrpau.dualscreendex

import android.graphics.Color
import androidx.core.graphics.toColorInt

enum class PokemonType(val displayName: String, val colorHex: Int) {
    NORMAL("Normal", "#A8A77A".toColorInt()),
    FIRE("Fire", "#EE8130".toColorInt()),
    WATER("Water", "#6390F0".toColorInt()),
    ELECTRIC("Electric", "#F7D02C".toColorInt()),
    GRASS("Grass", "#7AC74C".toColorInt()),
    ICE("Ice", "#96D9D6".toColorInt()),
    FIGHTING("Fighting", "#C22E28".toColorInt()),
    POISON("Poison", "#A33EA1".toColorInt()),
    GROUND("Ground", "#E2BF65".toColorInt()),
    FLYING("Flying", "#A98FF3".toColorInt()),
    PSYCHIC("Psychic", "#F95587".toColorInt()),
    BUG("Bug", "#A6B91A".toColorInt()),
    ROCK("Rock", "#B6A136".toColorInt()),
    GHOST("Ghost", "#735797".toColorInt()),
    DRAGON("Dragon", "#6F35FC".toColorInt()),
    STEEL("Steel", "#B7B7CE".toColorInt()),
    DARK("Dark", "#705746".toColorInt()),
    FAIRY("Fairy", "#D685AD".toColorInt()),
    UNKNOWN("???", Color.LTGRAY);

    companion object {
        fun fromString(value: String?): PokemonType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: entries.find { it.displayName.equals(value, ignoreCase = true) }
                ?: UNKNOWN
        }
    }
}