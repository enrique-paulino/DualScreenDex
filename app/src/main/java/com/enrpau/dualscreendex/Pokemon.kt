package com.enrpau.dualscreendex

data class Pokemon(
    val name: String,
    val id: Int,
    val type1: PokemonType,
    val type2: PokemonType?,
    val spriteUrl: String? = null // idk if I want to add this, would have to upload all images to app to allow for offline-use
)