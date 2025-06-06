package com.example.growagarden.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.card.MaterialCardView
import com.example.growagarden.R
import com.example.growagarden.data.StockItem
import com.example.growagarden.data.StockType

class StockItemAdapter : ListAdapter<StockItem, StockItemAdapter.StockItemViewHolder>(StockItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock, parent, false)
        return StockItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StockItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.stockCard)
        private val nameText: MaterialTextView = itemView.findViewById(R.id.stockName)
        private val valueText: MaterialTextView = itemView.findViewById(R.id.stockValue)
        private val priceText: MaterialTextView = itemView.findViewById(R.id.stockPrice)

        fun bind(item: StockItem) {
            nameText.text = item.name
            valueText.text = "x${item.value}"

            item.price?.let { price ->
                priceText.visibility = View.VISIBLE
                priceText.text = "💰 $price"
            } ?: run {
                priceText.visibility = View.GONE
            }

            item.rarity?.let { rarity ->
                card.strokeColor = when (rarity.lowercase()) {
                    "common" -> itemView.context.getColor(R.color.rarity_common)
                    "uncommon" -> itemView.context.getColor(R.color.rarity_uncommon)
                    "rare" -> itemView.context.getColor(R.color.rarity_rare)
                    "epic" -> itemView.context.getColor(R.color.rarity_epic)
                    "legendary" -> itemView.context.getColor(R.color.rarity_legendary)
                    else -> itemView.context.getColor(R.color.md_theme_outline)
                }
                card.strokeWidth = 2
            }
        }
    }

    private class StockItemDiffCallback : DiffUtil.ItemCallback<StockItem>() {
        override fun areItemsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: StockItem, newItem: StockItem): Boolean {
            return oldItem == newItem
        }
    }
}

class StockCategoryAdapter : ListAdapter<Pair<StockType, List<StockItem>>, StockCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: MaterialTextView = itemView.findViewById(R.id.categoryTitle)
        private val countText: MaterialTextView = itemView.findViewById(R.id.categoryCount)
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.categoryRecyclerView)
        private val emptyText: MaterialTextView = itemView.findViewById(R.id.emptyText)

        private val adapter = StockItemAdapter()

        init {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
        }

        fun bind(category: Pair<StockType, List<StockItem>>) {
            val (stockType, items) = category

            titleText.text = "${stockType.emoji} ${stockType.displayName}"
            countText.text = "${items.size} items"

            if (items.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                emptyText.text = "No ${stockType.displayName.lowercase()} available"
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
                adapter.submitList(items)
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<Pair<StockType, List<StockItem>>>() {
        override fun areItemsTheSame(
            oldItem: Pair<StockType, List<StockItem>>,
            newItem: Pair<StockType, List<StockItem>>
        ): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(
            oldItem: Pair<StockType, List<StockItem>>,
            newItem: Pair<StockType, List<StockItem>>
        ): Boolean {
            return oldItem.second == newItem.second
        }
    }
}