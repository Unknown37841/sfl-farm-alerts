package com.sfl.notifapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.sfl.notifapp.R
import com.sfl.notifapp.data.model.SFL_OVERVIEW_CATEGORIES
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.storage.PreferencesManager

class CategorySettingsAdapter(
    private val prefs: PreferencesManager,
    private val onToggleChanged: (TimerCategory, Boolean) -> Unit
) : RecyclerView.Adapter<CategorySettingsAdapter.ViewHolder>() {

    private val categories = SFL_OVERVIEW_CATEGORIES

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvCategoryEmoji)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvCategorySubtitle)
        val switchCategory: MaterialSwitch = view.findViewById(R.id.switchCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvEmoji.text = category.emoji
        holder.tvName.text = category.displayName
        holder.tvSubtitle.text = category.faName

        // Unbind listener before setting checked state to avoid recursion
        holder.switchCategory.setOnCheckedChangeListener(null)
        holder.switchCategory.isChecked = prefs.isCategoryEnabled(category)

        holder.switchCategory.setOnCheckedChangeListener { _, isChecked ->
            prefs.setCategoryEnabled(category, isChecked)
            onToggleChanged(category, isChecked)
        }
    }

    override fun getItemCount(): Int = categories.size
}
