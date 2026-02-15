package com.enrpau.dualscreendex

import com.enrpau.dualscreendex.data.RomProfile

object GenerationHelper {

    fun getGenSpecificTypes(pokemon: Pokemon, mechanics: RomProfile.Mechanics): Pair<PokemonType, PokemonType> {
        var t1 = pokemon.type1
        var t2 = pokemon.type2 ?: PokemonType.UNKNOWN

        // gen 1
        if (mechanics == RomProfile.Mechanics.GEN_1) {
            // magnemite/magneton was pure electric in gen 1
            if (pokemon.name.equals("magnemite", true) ||
                pokemon.name.equals("magneton", true)) {
                t2 = PokemonType.UNKNOWN
            }
        }

        // gen 1-5
        if (mechanics != RomProfile.Mechanics.GEN_6_PLUS) {

            if (t1 == PokemonType.FAIRY) {
                t1 = PokemonType.NORMAL
            }

            if (t2 == PokemonType.FAIRY) {
                t2 = PokemonType.UNKNOWN
            }
        }

        return Pair(t1, t2)
    }
}