package com.example.shizukudebloater

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PackageAdapter(private val onAction: (AppEntry, PackageAction) -> Unit)
    : ListAdapter<AppEntry, PackageAdapter.VH>(DIFF) {

    private var pm = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_package, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPackage: TextView = view.findViewById(R.id.tvPackage)
        private val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        private val btnDisable: Button = view.findViewById(R.id.btnDisable)
        private val btnUninstall: Button = view.findViewById(R.id.btnUninstall)
        private val btnForce: Button = view.findViewById(R.id.btnForce)
        private val ivIcon: ImageView = view.findViewById(R.id.ivIcon)

        fun bind(entry: AppEntry) {
            tvPackage.text = entry.packageName
            tvLabel.text = entry.label
            try {
                val icon: Drawable? = itemView.context.packageManager.getApplicationIcon(entry.packageName)
                ivIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            btnDisable.setOnClickListener { onAction(entry, PackageAction.DISABLE) }
            btnUninstall.setOnClickListener { onAction(entry, PackageAction.UNINSTALL) }
            btnForce.setOnClickListener { onAction(entry, PackageAction.FORCE) }

            // Show force button only for system apps (visibility controlled by adapter)
            btnForce.visibility = if (entry.isSystem) View.VISIBLE else View.GONE

            // Slight visual hint for system apps
            itemView.alpha = if (entry.isSystem) 0.95f else 1f
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppEntry>() {
            override fun areItemsTheSame(oldItem: AppEntry, newItem: AppEntry) = oldItem.packageName == newItem.packageName
            override fun areContentsTheSame(oldItem: AppEntry, newItem: AppEntry) = oldItem == newItem
        }
    }
}
