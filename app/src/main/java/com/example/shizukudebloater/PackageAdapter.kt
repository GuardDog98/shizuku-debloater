package com.example.shizukudebloater

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

enum class PackageAction { DISABLE, UNINSTALL }

class PackageAdapter(private val onAction: (String, PackageAction) -> Unit)
    : ListAdapter<String, PackageAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_package, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = getItem(position)
        holder.bind(pkg)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPackage: TextView = view.findViewById(R.id.tvPackage)
        private val btnDisable: Button = view.findViewById(R.id.btnDisable)
        private val btnUninstall: Button = view.findViewById(R.id.btnUninstall)

        fun bind(pkg: String) {
            tvPackage.text = pkg
            btnDisable.setOnClickListener { onAction(pkg, PackageAction.DISABLE) }
            btnUninstall.setOnClickListener { onAction(pkg, PackageAction.UNINSTALL) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }
}
