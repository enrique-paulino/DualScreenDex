package com.enrpau.dualscreendex.data

import android.content.Context
import com.enrpau.dualscreendex.Pokemon
import com.enrpau.dualscreendex.PokemonType
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CsvParsers {

    // format: id, name, type1, type2
    fun parsePokedex(context: Context, profile: RomProfile): List<Pokemon> {
        return parseFile(context, profile.dexFilePath, profile.isBuiltIn) { tokens ->
            val id = tokens[0].trim().toInt()
            val name = tokens[1].trim()
            val t1 = PokemonType.fromString(tokens[2].trim())
            val t2 = if (tokens.size > 3 && tokens[3].isNotBlank()) {
                PokemonType.fromString(tokens[3].trim())
            } else {
                PokemonType.UNKNOWN
            }
            // Base pokemon have no variant label
            Pokemon(name, id, t1, t2, null)
        }
    }


    // format: id, variantlabel, type1, type2 e.g. "1, alolan, normal, fire"
    // the pokemon should have the same id as the base form
    fun parseRegionalForms(context: Context, profile: RomProfile): List<Pokemon> {
        val path = profile.regionalFilePath ?: return emptyList()

        return parseFile(context, path, profile.isBuiltIn) { tokens ->
            val id = tokens[0].trim().toInt()
            val label = tokens[1].trim()
            val t1 = PokemonType.fromString(tokens[2].trim())
            val t2 = if (tokens.size > 3 && tokens[3].isNotBlank()) {
                PokemonType.fromString(tokens[3].trim())
            } else {
                PokemonType.UNKNOWN
            }

            Pokemon("TEMP_NAME", id, t1, t2, label)
        }
    }

    // format: attacker x defender standard chart
    fun parseMatchupChart(context: Context, profile: RomProfile): Map<String, Double> {
        val path = profile.matchupFilePath ?: "dex/vanilla_matchup.csv"
        val isAsset = profile.isBuiltIn || profile.matchupFilePath == null

        val reader = getReader(context, path, isAsset) ?: return emptyMap()
        val customTable = mutableMapOf<String, Double>()

        val lines = reader.readLines()
        if (lines.isEmpty()) return emptyMap()

        // Handle BOM and Header
        val headers = lines[0].split(",").map { it.trim().uppercase().replace("\uFEFF", "") }

        for (i in 1 until lines.size) {
            val tokens = lines[i].split(",")
            if (tokens.isEmpty()) continue

            val attacker = tokens[0].trim().uppercase().replace("\uFEFF", "")

            for (j in 1 until tokens.size) {
                if (j >= headers.size) break
                val defender = headers[j]
                val valueStr = tokens[j].trim()

                val multiplier = when (valueStr) {
                    "1/2" -> 0.5
                    "" -> 1.0
                    else -> valueStr.toDoubleOrNull() ?: 1.0
                }

                customTable["${attacker}_${defender}"] = multiplier
            }
        }
        return customTable
    }

    private fun parseFile(context: Context, path: String, isAsset: Boolean, mapper: (List<String>) -> Pokemon): List<Pokemon> {
        val reader = getReader(context, path, isAsset) ?: return emptyList()
        val list = mutableListOf<Pokemon>()

        reader.use { r ->
            r.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.isNotEmpty() && tokens[0].trim().toIntOrNull() != null) {
                    try {
                        list.add(mapper(tokens))
                    } catch (e: Exception) {
                        // skip bad lines
                    }
                }
            }
        }
        return list
    }

    private fun getReader(context: Context, path: String, isAsset: Boolean): BufferedReader? {
        return try {
            val inputStream = if (isAsset) {
                context.assets.open(path)
            } else {
                File(path).inputStream()
            }
            BufferedReader(InputStreamReader(inputStream))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}