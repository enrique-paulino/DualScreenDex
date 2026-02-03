package com.enrpau.dualscreendex

object TypeMatchup {
    enum class Gen {
        GEN_1,
        GEN_2_5,
        GEN_6_PLUS
    }

    private val modernChart = mapOf(
        PokemonType.NORMAL to mapOf(
            PokemonType.ROCK to 0.5,
            PokemonType.GHOST to 0.0,
            PokemonType.STEEL to 0.5
        ),

        PokemonType.FIRE to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 0.5,
            PokemonType.GRASS to 2.0,
            PokemonType.ICE to 2.0,
            PokemonType.BUG to 2.0,
            PokemonType.ROCK to 0.5,
            PokemonType.DRAGON to 0.5,
            PokemonType.STEEL to 2.0
        ),

        PokemonType.WATER to mapOf(
            PokemonType.FIRE to 2.0,
            PokemonType.WATER to 0.5,
            PokemonType.GRASS to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.ROCK to 2.0,
            PokemonType.DRAGON to 0.5
        ),

        PokemonType.ELECTRIC to mapOf(
            PokemonType.WATER to 2.0,
            PokemonType.ELECTRIC to 0.5,
            PokemonType.GRASS to 0.5,
            PokemonType.GROUND to 0.0,
            PokemonType.FLYING to 2.0,
            PokemonType.DRAGON to 0.5
        ),

        PokemonType.GRASS to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 2.0,
            PokemonType.GRASS to 0.5,
            PokemonType.POISON to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.FLYING to 0.5,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 2.0,
            PokemonType.DRAGON to 0.5,
            PokemonType.STEEL to 0.5
        ),

        PokemonType.ICE to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 0.5,
            PokemonType.GRASS to 2.0,
            PokemonType.ICE to 0.5,
            PokemonType.GROUND to 2.0,
            PokemonType.FLYING to 2.0,
            PokemonType.DRAGON to 2.0,
            PokemonType.STEEL to 0.5
        ),

        PokemonType.FIGHTING to mapOf(
            PokemonType.NORMAL to 2.0,
            PokemonType.ICE to 2.0,
            PokemonType.POISON to 0.5,
            PokemonType.FLYING to 0.5,
            PokemonType.PSYCHIC to 0.5,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 2.0,
            PokemonType.GHOST to 0.0,
            PokemonType.DARK to 2.0,
            PokemonType.STEEL to 2.0,
            PokemonType.FAIRY to 0.5
        ),

        PokemonType.POISON to mapOf(
            PokemonType.GRASS to 2.0,
            PokemonType.POISON to 0.5,
            PokemonType.GROUND to 0.5,
            PokemonType.ROCK to 0.5,
            PokemonType.GHOST to 0.5,
            PokemonType.STEEL to 0.0,
            PokemonType.FAIRY to 2.0
        ),

        PokemonType.GROUND to mapOf(
            PokemonType.FIRE to 2.0,
            PokemonType.ELECTRIC to 2.0,
            PokemonType.GRASS to 0.5,
            PokemonType.POISON to 2.0,
            PokemonType.FLYING to 0.0,
            PokemonType.BUG to 0.5,
            PokemonType.ROCK to 2.0,
            PokemonType.STEEL to 2.0
        ),

        PokemonType.FLYING to mapOf(
            PokemonType.ELECTRIC to 0.5,
            PokemonType.GRASS to 2.0,
            PokemonType.FIGHTING to 2.0,
            PokemonType.BUG to 2.0,
            PokemonType.ROCK to 0.5,
            PokemonType.STEEL to 0.5
        ),

        PokemonType.PSYCHIC to mapOf(
            PokemonType.FIGHTING to 2.0,
            PokemonType.POISON to 2.0,
            PokemonType.PSYCHIC to 0.5,
            PokemonType.DARK to 0.0,
            PokemonType.STEEL to 0.5
        ),

        PokemonType.BUG to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.GRASS to 2.0,
            PokemonType.FIGHTING to 0.5,
            PokemonType.POISON to 0.5,
            PokemonType.FLYING to 0.5,
            PokemonType.PSYCHIC to 2.0,
            PokemonType.GHOST to 0.5,
            PokemonType.DARK to 2.0,
            PokemonType.STEEL to 0.5,
            PokemonType.FAIRY to 0.5
        ),

        PokemonType.ROCK to mapOf(
            PokemonType.FIRE to 2.0,
            PokemonType.ICE to 2.0,
            PokemonType.FIGHTING to 0.5,
            PokemonType.GROUND to 0.5,
            PokemonType.FLYING to 2.0,
            PokemonType.BUG to 2.0,
            PokemonType.STEEL to 0.5
        ),

        PokemonType.GHOST to mapOf(
            PokemonType.NORMAL to 0.0,
            PokemonType.PSYCHIC to 2.0,
            PokemonType.GHOST to 2.0,
            PokemonType.DARK to 0.5
        ),

        PokemonType.DRAGON to mapOf(
            PokemonType.DRAGON to 2.0,
            PokemonType.STEEL to 0.5,
            PokemonType.FAIRY to 0.0
        ),

        PokemonType.DARK to mapOf(
            PokemonType.FIGHTING to 0.5,
            PokemonType.PSYCHIC to 2.0,
            PokemonType.GHOST to 2.0,
            PokemonType.DARK to 0.5,
            PokemonType.FAIRY to 0.5
        ),

        PokemonType.STEEL to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.WATER to 0.5,
            PokemonType.ELECTRIC to 0.5,
            PokemonType.ICE to 2.0,
            PokemonType.ROCK to 2.0,
            PokemonType.STEEL to 0.5,
            PokemonType.FAIRY to 2.0
        ),

        PokemonType.FAIRY to mapOf(
            PokemonType.FIRE to 0.5,
            PokemonType.FIGHTING to 2.0,
            PokemonType.POISON to 0.5,
            PokemonType.DRAGON to 2.0,
            PokemonType.DARK to 2.0,
            PokemonType.STEEL to 0.5
        )
    )

    fun getMultiplier(attack: PokemonType, defense: PokemonType, gen: Gen) : Double {
        val baseMultiplier = modernChart[attack]?.get(defense) ?: 1.0

        return when (gen) {
            Gen.GEN_1 -> applyGen1Rules(attack, defense, baseMultiplier)
            Gen.GEN_2_5 -> applyGen2Rules(attack, defense, baseMultiplier)
            else -> baseMultiplier
        }

    }

    private fun applyGen1Rules(attack: PokemonType, defense: PokemonType, current: Double): Double {
        if (isTypeMissingInGen1(attack) || isTypeMissingInGen1(defense)) {
            return 1.0
        }

        if (attack == PokemonType.GHOST && defense == PokemonType.PSYCHIC) return 0.0
        if (attack == PokemonType.POISON && defense == PokemonType.BUG) return 2.0
        if (attack == PokemonType.BUG && defense == PokemonType.POISON) return 2.0
        if (attack == PokemonType.ICE && defense == PokemonType.FIRE) return 1.0

        return current
    }

    private fun applyGen2Rules(attack: PokemonType, defense: PokemonType, current: Double): Double {
        // steel resisted ghost & dark in gen 2-5
        if (defense == PokemonType.STEEL && (attack == PokemonType.GHOST || attack == PokemonType.DARK)) {
            return 0.5
        }
        return current
    }

    private fun isTypeMissingInGen1(type: PokemonType): Boolean {
        return type == PokemonType.DARK || type == PokemonType.STEEL || type == PokemonType.FAIRY
    }

}