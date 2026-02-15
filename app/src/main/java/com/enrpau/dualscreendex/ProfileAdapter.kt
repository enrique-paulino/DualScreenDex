package com.enrpau.dualscreendex

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enrpau.dualscreendex.data.RomProfile
import com.google.android.material.card.MaterialCardView

class ProfileAdapter(
    private var profiles: List<RomProfile>,
    private val currentId: String,
    private val onSelect: (RomProfile) -> Unit,
    private val onDelete: (RomProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardProfile)
        val name: TextView = view.findViewById(R.id.tvProfileName)
        val btnDelete: View = view.findViewById(R.id.btnDeleteProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_row, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val p = profiles[position]
        holder.name.text = p.name

        val isSelected = p.id == currentId
        holder.card.strokeColor = if (isSelected) Color.BLUE else Color.TRANSPARENT
        holder.card.strokeWidth = if (isSelected) 4 else 0

        // hide delete button for built-in profiles
        holder.btnDelete.visibility = if (p.isBuiltIn) View.GONE else View.VISIBLE

        holder.card.setOnClickListener { onSelect(p) }
        holder.btnDelete.setOnClickListener { onDelete(p) }
    }

    override fun getItemCount() = profiles.size
}