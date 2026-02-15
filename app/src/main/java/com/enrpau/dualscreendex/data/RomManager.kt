package com.enrpau.dualscreendex.data

import android.content.Context
import android.util.Log // Import Logging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import androidx.core.content.edit

object RomManager {
    private const val PREFS_NAME = "RomProfiles"
    private const val KEY_CUSTOM_PROFILES = "SAVED_PROFILES"
    private const val KEY_SELECTED_ID = "SELECTED_PROFILE_ID"
    private const val TAG = "DualDex_RomManager"

    private val builtInProfiles = listOf(
        RomProfile(
            id = "vanilla_modern",
            name = "Modern (Gen 6+)",
            isBuiltIn = true,
            dexFilePath = "dex/vanilla_pokedex.csv",
            regionalFilePath = "dex/vanilla_regional.csv",
            matchupFilePath = "dex/vanilla_matchup.csv",
            baseMechanics = RomProfile.Mechanics.GEN_6_PLUS
        ),
        RomProfile(
            id = "vanilla_classic",
            name = "Classic (Gen 2-5)",
            isBuiltIn = true,
            dexFilePath = "dex/vanilla_pokedex.csv",
            regionalFilePath = "dex/vanilla_regional.csv",
            matchupFilePath = "dex/vanilla_matchup.csv",
            baseMechanics = RomProfile.Mechanics.GEN_2_TO_5
        ),
        RomProfile(
            id = "vanilla_retro",
            name = "Retro (Gen 1)",
            isBuiltIn = true,
            dexFilePath = "dex/vanilla_pokedex.csv",
            regionalFilePath = null,
            matchupFilePath = "dex/vanilla_matchup.csv",
            baseMechanics = RomProfile.Mechanics.GEN_1
        ),
        RomProfile(
            id = "lumi_plat",
            name = "Luminescent Platinum",
            isBuiltIn = true,
            dexFilePath = "dex/luminescent_pokedex.csv",
            regionalFilePath = "dex/luminescent_regional.csv",
            matchupFilePath = "dex/vanilla_matchup.csv",
            baseMechanics = RomProfile.Mechanics.GEN_6_PLUS
        ),
        RomProfile(
            id = "radi_red",
            name = "Radical Red",
            isBuiltIn = true,
            dexFilePath = "dex/radicalred_pokedex.csv",
            regionalFilePath = "dex/radicalred_regional.csv",
            matchupFilePath = "dex/vanilla_matchup.csv",
            baseMechanics = RomProfile.Mechanics.GEN_6_PLUS
        ),
    )

    private var customProfiles: MutableList<RomProfile> = mutableListOf()

    var currentProfile: RomProfile = builtInProfiles.first()
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val json = prefs.getString(KEY_CUSTOM_PROFILES, null)
        Log.d(TAG, "Initializing... Found JSON: $json")

        if (json != null) {
            try {
                val type = object : TypeToken<List<RomProfile>>() {}.type
                val loaded: List<RomProfile> = Gson().fromJson(json, type)
                customProfiles.clear()
                customProfiles.addAll(loaded)
                Log.d(TAG, "Loaded ${customProfiles.size} custom profiles.")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing profiles: ${e.message}")
                e.printStackTrace()
            }
        }

        val selectedId = prefs.getString(KEY_SELECTED_ID, null)
        if (selectedId != null) {
            val all = builtInProfiles + customProfiles
            currentProfile = all.find { it.id == selectedId } ?: builtInProfiles.first()
        }
    }

    fun getAllProfiles(): List<RomProfile> {
        val all = builtInProfiles + customProfiles
        Log.d(TAG, "Getting all profiles. Total: ${all.size} (Built-in: ${builtInProfiles.size}, Custom: ${customProfiles.size})")
        return all
    }

    fun saveCustomProfile(context: Context, name: String, dexFile: File, regionalsFile: File?, matchupFile: File?, mechanics: RomProfile.Mechanics) {
        Log.d(TAG, "Attempting to save profile: $name")

        val newProfile = RomProfile(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            isBuiltIn = false,
            dexFilePath = dexFile.absolutePath,
            regionalFilePath = regionalsFile?.absolutePath,
            matchupFilePath = matchupFile?.absolutePath,
            baseMechanics = mechanics
        )

        customProfiles.add(newProfile)
        Log.d(TAG, "Profile added to memory. New Custom Count: ${customProfiles.size}")
        saveToPrefs(context)
    }

    fun deleteCustomProfile(context: Context, profile: RomProfile) {
        if (profile.isBuiltIn) return
        customProfiles.remove(profile)
        if (currentProfile == profile) {
            selectProfile(context, builtInProfiles.first())
        }
        saveToPrefs(context)
    }

    fun selectProfile(context: Context, profile: RomProfile) {
        currentProfile = profile
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_SELECTED_ID, profile.id)
            }
    }

    private fun saveToPrefs(context: Context) {
        try {
            val json = Gson().toJson(customProfiles)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putString(KEY_CUSTOM_PROFILES, json)
                }
            Log.d(TAG, "Saved to Prefs. JSON length: ${json.length}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save prefs: ${e.message}")
        }
    }
}