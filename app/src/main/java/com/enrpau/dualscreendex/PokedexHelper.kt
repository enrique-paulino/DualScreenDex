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
        const val DATABASE_VERSION = 3 // bumped for schema update

        private const val CSV_FILENAME = "pokemon_data.csv"

        // csv column indices
        // id(0), name(1), ..., types(6), ..., sprite_url(38)
        private const val COL_IDX_ID = 0
        private const val COL_IDX_NAME = 1
        private const val COL_IDX_TYPES = 6
        private const val COL_IDX_SPRITE = 38
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE pokemon (
                name TEXT PRIMARY KEY, 
                type1 TEXT, 
                type2 TEXT,
                pokedex_id INTEGER,
                sprite_url TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)

        loadDataFromCSV(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pokemon")
        onCreate(db)
    }

    private fun loadDataFromCSV(db: SQLiteDatabase) {
        try {
            db.beginTransaction()
            val assetManager = context.assets
            val inputStream = assetManager.open(CSV_FILENAME)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // skip header
            reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // parse while handling quotes
                val tokens = parseCsvLine(line!!)

                if (tokens.size < 7) continue // safety check

                val id = tokens[COL_IDX_ID].toIntOrNull() ?: 0
                val name = tokens[COL_IDX_NAME].trim()
                val rawTypes = tokens[COL_IDX_TYPES] // e.g. "['Grass', 'Poison']"
                val spriteUrl = if (tokens.size > COL_IDX_SPRITE) tokens[COL_IDX_SPRITE] else ""

                // clean up the weird array string
                val (t1, t2) = parseTypes(rawTypes)

                val values = ContentValues().apply {
                    put("name", name)
                    put("pokedex_id", id)
                    put("type1", t1)
                    put("type2", t2) // can be null
                    put("sprite_url", spriteUrl)
                }

                // ignore duplicates
                db.insertWithOnConflict("pokemon", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }

            db.setTransactionSuccessful()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    // parses: "['Grass', 'Poison']" -> Grass, Poison
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
        val cursor = db.rawQuery("SELECT * FROM pokemon ORDER BY pokedex_id ASC", null)

        if (cursor.moveToFirst()) {
            do {
                val pName = cursor.getString(0)
                val t1Str = cursor.getString(1)
                val t2Str = cursor.getString(2)
                val pId = cursor.getInt(3)
                val sUrl = cursor.getString(4)

                val t1 = PokemonType.fromString(t1Str)
                val t2 = if (t2Str != null) PokemonType.fromString(t2Str) else null

                list.add(Pokemon(pName, pId, t1, t2, sUrl))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getPokemon(name: String): Pokemon? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM pokemon WHERE name LIKE ?", arrayOf(name))

        var result: Pokemon? = null
        if (cursor.moveToFirst()) {
            val pName = cursor.getString(0)
            val t1Str = cursor.getString(1)
            val t2Str = cursor.getString(2)
            val pId = cursor.getInt(3)
            val sUrl = cursor.getString(4)

            val t1 = PokemonType.fromString(t1Str)
            val t2 = if (t2Str != null) PokemonType.fromString(t2Str) else null

            result = Pokemon(pName, pId, t1, t2, sUrl)
        }
        cursor.close()
        return result
    }
}