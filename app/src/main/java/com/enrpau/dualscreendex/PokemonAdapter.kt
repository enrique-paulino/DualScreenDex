package com.enrpau.dualscreendex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.enrpau.dualscreendex.data.RomProfile

class PokemonAdapter(
    private var fullList: List<Pokemon>,
    private val onClick: (Pokemon) -> Unit
) : RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder>() {

    private var filteredList = fullList.toMutableList()
    private var currentMechanics: RomProfile.Mechanics = RomProfile.Mechanics.GEN_6_PLUS
    private var currentTheme: AppTheme = ThemeManager.currentTheme

    inner class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvRowName)
        val tvId: TextView = itemView.findViewById(R.id.tvRowId)
        val typeContainer: LinearLayout = itemView.findViewById(R.id.rowTypesContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pokemon_row, parent, false)
        return PokemonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) {
        val pokemon = filteredList[position]

        val cleanName = pokemon.name.replaceFirstChar { it.uppercase() }

        val displayName = if (pokemon.variantLabel != null) {
            "$cleanName (${pokemon.variantLabel})"
        } else {
            cleanName
        }

        holder.tvName.text = displayName
        holder.tvId.text = String.format("#%03d", pokemon.id)

        holder.tvName.setTextColor(currentTheme.listTextColor)
        holder.tvId.setTextColor(ColorUtils.setAlphaComponent(currentTheme.listTextColor, 128))

        val t1 = pokemon.type1
        val t2 = pokemon.type2 ?: PokemonType.UNKNOWN

        holder.typeContainer.removeAllViews()
        addMiniBadge(holder.typeContainer, t1)
        if (t2 != PokemonType.UNKNOWN) {
            addMiniBadge(holder.typeContainer, t2)
        }

        holder.itemView.setOnClickListener { onClick(pokemon) }
    }

    override fun getItemCount() = filteredList.size

    fun updateList(newList: List<Pokemon>) {
        filteredList.clear()
        filteredList = ArrayList(newList)
        fullList = ArrayList(newList)
        notifyDataSetChanged()
    }

    fun updateSettings(mechanics: RomProfile.Mechanics, theme: AppTheme) {
        this.currentMechanics = mechanics
        this.currentTheme = theme
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            fullList.toMutableList()
        } else {
            fullList.filter { it.name.contains(query, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }

    private fun addMiniBadge(container: LinearLayout, type: PokemonType) {
        val tv = TextView(container.context)
        tv.text = type.displayName.take(3).uppercase()
        tv.textSize = 10f
        tv.setTextColor(android.graphics.Color.WHITE)
        tv.setPadding(12, 4, 12, 4)

        val bg = GradientDrawable()
        bg.setColor(type.colorHex)
        bg.cornerRadius = 8f
        tv.background = bg

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 0, 0)
        container.addView(tv, params)
    }
}