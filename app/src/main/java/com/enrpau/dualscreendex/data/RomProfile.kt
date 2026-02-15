package com.enrpau.dualscreendex.data

import java.io.File

data class RomProfile(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,

    // --- The 3 Files ---
    val dexFilePath: String,
    val regionalFilePath: String? = null,
    val matchupFilePath: String? = null,

    val baseMechanics: Mechanics = Mechanics.GEN_6_PLUS
) {
    enum class Mechanics {
        GEN_1,      // Special/Physical split by type, No Dark/Steel/Fairy
        GEN_2_TO_5, // No Fairy, Steel resistances exist
        GEN_6_PLUS  // Modern Type Chart
    }

    fun hasRegionals(): Boolean = regionalFilePath != null
}