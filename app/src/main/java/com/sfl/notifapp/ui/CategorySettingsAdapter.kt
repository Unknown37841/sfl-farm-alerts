package com.sfl.notifapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.sfl.notifapp.R
import com.sfl.notifapp.data.model.SFL_OVERVIEW_CATEGORIES
import com.sfl.notifapp.data.model.TimerCategory
import com.sfl.notifapp.data.model.ResourceType
import com.sfl.notifapp.data.storage.PreferencesManager

class CategorySettingsAdapter(
    private val prefs: PreferencesManager,
    private val onToggleChanged: (TimerCategory, Boolean) -> Unit,
    private val onResourceToggleChanged: (ResourceType, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val categories = SFL_OVERVIEW_CATEGORIES
    private var resourcesExpanded = false

    private val viewTypeCategory = 0
    private val viewTypeResource = 1

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvCategorySubtitle)
        val switchCategory: MaterialSwitch = view.findViewById(R.id.switchCategory)
    }

    class ResourceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivResourceIcon)
        val tvName: TextView = view.findViewById(R.id.tvResourceName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvResourceSubtitle)
        val switchResource: MaterialSwitch = view.findViewById(R.id.switchResource)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSubResourceRow(position)) viewTypeResource else viewTypeCategory
    }

    /** True when this position is one of the indented sub-rows under Resources. */
    private fun isSubResourceRow(position: Int): Boolean {
        if (!resourcesExpanded) return false
        val resIndex = plainIndexOf(TimerCategory.RESOURCES) ?: return false
        return position > resIndex && position <= resIndex + ResourceType.entries.size
    }

    private fun plainIndexOf(target: TimerCategory): Int? =
        categories.indexOfFirst { it == target }.takeIf { it >= 0 }

    private fun resourceAt(position: Int): ResourceType {
        val resIndex = plainIndexOf(TimerCategory.RESOURCES) ?: return ResourceType.entries.last()
        return ResourceType.entries[position - resIndex - 1]
    }

    /** Flat position -> category, accounting for the expanded sub-rows. */
    private fun categoryAt(position: Int): TimerCategory {
        var remaining = position
        for (cat in categories) {
            if (remaining == 0) return cat
            remaining--
            if (cat == TimerCategory.RESOURCES && resourcesExpanded) {
                if (remaining < ResourceType.entries.size) return cat
                remaining -= ResourceType.entries.size
            }
        }
        return categories.last()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == viewTypeResource) {
            ResourceViewHolder(inflater.inflate(R.layout.item_resource_setting, parent, false))
        } else {
            CategoryViewHolder(inflater.inflate(R.layout.item_category_setting, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ResourceViewHolder -> bindResourceRow(holder, resourceAt(position))
            is CategoryViewHolder -> bindCategoryRow(holder, categoryAt(position))
        }
    }

    private fun bindCategoryRow(holder: CategoryViewHolder, category: TimerCategory) {
        holder.ivIcon.setImageResource(category.iconResId)
        holder.tvName.text = if (category == TimerCategory.RESOURCES) {
            val arrow = if (resourcesExpanded) "▾" else "▸"
            "$arrow Resources"
        } else {
            category.displayName
        }
        val subtitle = if (category == TimerCategory.RESOURCES) {
            val enabled = prefs.getEnabledResourcesCount()
            val total = ResourceType.entries.size
            "${category.faName} · $enabled/$total فعال"
        } else {
            category.faName
        }
        holder.tvSubtitle.text = subtitle

        // Unbind listener before setting checked state to avoid recursion
        holder.switchCategory.setOnCheckedChangeListener(null)
        holder.switchCategory.isChecked = prefs.isCategoryEnabled(category)

        holder.switchCategory.setOnCheckedChangeListener { _, isChecked ->
            prefs.setCategoryEnabled(category, isChecked)
            onToggleChanged(category, isChecked)
        }

        // Row tap toggles the sub-list expansion
        holder.itemView.setOnClickListener {
            if (category == TimerCategory.RESOURCES) {
                resourcesExpanded = !resourcesExpanded
                notifyDataSetChanged()
            }
        }
    }

    private fun bindResourceRow(holder: ResourceViewHolder, resource: ResourceType) {
        holder.ivIcon.setImageResource(resource.iconResId)
        holder.tvName.text = resource.displayName
        holder.tvSubtitle.text = resource.faName

        holder.switchResource.setOnCheckedChangeListener(null)
        holder.switchResource.isChecked = prefs.isResourceEnabled(resource)

        holder.switchResource.setOnCheckedChangeListener { _, isChecked ->
            prefs.setResourceEnabled(resource, isChecked)
            // The parent row shows an "enabled/total" counter — refresh it too
            notifyItemRangeChangedAroundResources()
            onResourceToggleChanged(resource, isChecked)
        }
    }

    /** Minimal refresh around the Resources block (parent row + sub rows). */
    private fun notifyItemRangeChangedAroundResources() {
        val resIndex = flatIndexOf(TimerCategory.RESOURCES) ?: return
        if (resourcesExpanded) {
            notifyItemRangeChanged(resIndex, 1 + ResourceType.entries.size)
        } else {
            notifyItemRangeChanged(resIndex, 1)
        }
    }

    /** Flat adapter position of a category row (parent rows only). */
    private fun flatIndexOf(target: TimerCategory): Int? {
        var index = 0
        for (cat in categories) {
            if (cat == target) return index
            index++
            if (cat == TimerCategory.RESOURCES && resourcesExpanded) {
                index += ResourceType.entries.size
            }
        }
        return null
    }

    override fun getItemCount(): Int {
        val extra = if (resourcesExpanded) ResourceType.entries.size else 0
        return categories.size + extra
    }
}
