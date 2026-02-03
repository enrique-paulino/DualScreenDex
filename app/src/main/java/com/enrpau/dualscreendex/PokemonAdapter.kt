package com.enrpau.dualscreendex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.GradientDrawable

class PokemonAdapter(
    private var fullList: List<Pokemon>,
    private val onClick: (Pokemon) -> Unit
) : RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder>() {

    private var filteredList = fullList.toMutableList()
    private var currentGen: TypeMatchup.Gen = TypeMatchup.Gen.GEN_6_PLUS

    fun updateGeneration(newGen: TypeMatchup.Gen) {
        currentGen = newGen
        notifyDataSetChanged()
    }

    inner class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvRowName)
        val typeContainer: LinearLayout = itemView.findViewById(R.id.rowTypesContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pokemon_row, parent, false)
        return PokemonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) {
        val pokemon = filteredList[position]

        holder.tvName.text = pokemon.name.replaceFirstChar { it.uppercase() }
        val tvId = holder.itemView.findViewById<TextView>(R.id.tvRowId)
        tvId.text = String.format("#%03d", pokemon.id) // turns 1 into #001

        val (t1, t2) = GenerationHelper.getGenSpecificTypes(pokemon, currentGen)

        holder.typeContainer.removeAllViews()

        addMiniBadge(holder.typeContainer, t1)
        if (t2 != PokemonType.UNKNOWN) {
            addMiniBadge(holder.typeContainer, t2)
        }

        holder.itemView.setOnClickListener { onClick(pokemon) }
    }

    override fun getItemCount() = filteredList.size

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