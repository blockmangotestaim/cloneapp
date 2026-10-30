package com.example.clonespace

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val showMenu: Boolean,
    private val onClick: (Row) -> Unit,
    private val onMenu: (View, Row) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<AppAdapter.VH>() {

    var rows: List<Row> = emptyList()
        set(v) { field = v; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val name: TextView = v.findViewById(R.id.name)
        val sub: TextView = v.findViewById(R.id.sub)
        val menu: ImageButton = v.findViewById(R.id.menu)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false))

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val r = rows[i]
        h.icon.setImageDrawable(r.icon)
        h.name.text = r.label
        h.sub.text = r.note ?: r.pkg
        h.sub.setTextColor(if (r.noteIsError) Color.parseColor("#C62828") else Color.GRAY)
        h.itemView.setOnClickListener { onClick(r) }
        h.menu.visibility = if (showMenu) View.VISIBLE else View.GONE
        h.menu.setOnClickListener { onMenu(it, r) }
    }
}
