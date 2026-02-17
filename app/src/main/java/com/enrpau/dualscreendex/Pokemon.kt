package com.enrpau.dualscreendex

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pokemon(
    val name: String,
    val id: Int,
    val type1: PokemonType,
    val type2: PokemonType?,
    val variantLabel: String? = null,
    val japaneseKana: String? = null
) : Parcelable
