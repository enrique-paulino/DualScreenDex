package com.enrpau.dualscreendex

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.BufferedReader
import java.io.InputStreamReader

class PokedexHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "pokedex.db"
        const val DATABASE_VERSION = 6

        private const val CSV_VANILLA = "vanilla.csv"
        private const val CSV_REGIONAL = "vanilla-regional.csv"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // _id as primary key to not conflict with variant data
        val createTable = """
            CREATE TABLE pokemon (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT, 
                type1 TEXT, 
                type2 TEXT,
                pokedex_id INTEGER,
                variant TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)

        loadVanillaData(db)
        loadRegionalData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pokemon")
        onCreate(db)
    }

    private fun loadVanillaData(db: SQLiteDatabase) {
        try {
            db.beginTransaction()
            val inputStream = context.assets.open(CSV_VANILLA)
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.readLine() // skip header

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = parseCsvLine(line!!)
                if (tokens.size < 3) continue

                // Your CSV has: ID, Name, ['Type1', 'Type2']
                val id = tokens[0].toIntOrNull() ?: 0
                val name = tokens[1].trim()
                val rawTypes = tokens[2]

                val (t1, t2) = parseTypes(rawTypes)

                val values = ContentValues().apply {
                    put("name", name)
                    put("pokedex_id", id)
                    put("type1", t1)
                    put("type2", t2)
                    put("variant", null as String?) // Standard pokemon have no variant label
                }

                db.insert("pokemon", null, values)
            }
            db.setTransactionSuccessful()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    private fun loadRegionalData(db: SQLiteDatabase) {
        try {
            val list = context.assets.list("")
            if (list == null || !list.contains(CSV_REGIONAL)) return

            db.beginTransaction()
            val inputStream = context.assets.open(CSV_REGIONAL)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // skip header
            val header = reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = parseCsvLine(line!!)

                if (tokens.size >= 4) {
                    val name = tokens[0].trim()
                    val id = tokens[1].trim().toIntOrNull() ?: 0

                    val rawTypes = tokens[2].trim()
                    val typeParts = rawTypes.split(",").map { it.trim() }

                    val t1 = typeParts.getOrElse(0) { "Normal" }.uppercase()
                    val t2 = if (typeParts.size > 1) typeParts[1].uppercase() else null

                    val variant = tokens[3].trim()

                    val values = ContentValues().apply {
                        put("name", name)
                        put("pokedex_id", id)
                        put("type1", t1)
                        put("type2", t2)
                        put("variant", variant)
                    }
                    db.insert("pokemon", null, values)
                }
            }
            db.setTransactionSuccessful()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }
    private fun parseTypes(raw: String): Pair<String, String?> {
        // strip brackets and quotes
        val clean = raw.replace("[", "")
            .replace("]", "")
            .replace("'", "")
            .replace("\"", "")

        val parts = clean.split(",").map { it.trim() }

        val t1 = parts.getOrElse(0) { "Normal" } // fallback
        val t2 = if (parts.size > 1) parts[1] else null

        return Pair(t1, t2)
    }

    // handles apostrophe inside quotes correctly
    // e.g. 1, "Farfetch'd", "['Normal', 'Flying']"
    private fun parseCsvLine(line: String): List<String> {
        val result = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes // toggle state
                char == ',' && !inQuotes -> {
                    // comma outside quotes = new token
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        result.add(sb.toString()) // add final token
        return result
    }

    // standard getters
    fun getAllPokemon(): List<Pokemon> {
        val list = ArrayList<Pokemon>()
        val db = this.readableDatabase
        // order by id, then by variant
        val cursor = db.rawQuery("SELECT * FROM pokemon ORDER BY pokedex_id ASC, variant ASC", null)

        if (cursor.moveToFirst()) {
            do {
                val pName = cursor.getString(1)
                val t1Str = cursor.getString(2)
                val t2Str = cursor.getString(3)
                val pId = cursor.getInt(4)
                val variant = cursor.getString(5)

                val t1 = PokemonType.fromString(t1Str)
                val t2 = if (t2Str != null) PokemonType.fromString(t2Str) else null

                list.add(Pokemon(pName, pId, t1, t2, variant))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getVariantsFor(name: String): List<Pokemon> {
        val list = ArrayList<Pokemon>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM pokemon WHERE name LIKE ? ORDER BY variant ASC", arrayOf(name))

        if (cursor.moveToFirst()) {
            do {
                val pName = cursor.getString(1)
                val t1Str = cursor.getString(2)
                val t2Str = cursor.getString(3)
                val pId = cursor.getInt(4)
                val variant = cursor.getString(5)

                val t1 = PokemonType.fromString(t1Str)
                val t2 = if (t2Str != null) PokemonType.fromString(t2Str) else null

                list.add(Pokemon(pName, pId, t1, t2, variant))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}