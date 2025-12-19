package com.yourteam.finwise.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourteam.finwise.R
import com.yourteam.finwise.data.entities.Category
import com.yourteam.finwise.data.entities.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    private var categories: List<Category> = emptyList()
    var onItemClick: ((Transaction) -> Unit)? = null

    // Method to update categories
    fun setCategories(categoryList: List<Category>) {
        this.categories = categoryList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction, categories)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(transaction)
        }
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconText: TextView = itemView.findViewById(R.id.transactionIcon)
        private val descriptionText: TextView = itemView.findViewById(R.id.transactionDescription)
        private val categoryText: TextView = itemView.findViewById(R.id.transactionCategory)
        private val dateText: TextView = itemView.findViewById(R.id.transactionDate)
        private val amountText: TextView = itemView.findViewById(R.id.transactionAmount)

        fun bind(transaction: Transaction, categories: List<Category>) {
            // Set icon based on category
            iconText.text = getIconForCategory(transaction.categoryId)

            // Set description
            descriptionText.text = transaction.description ?: "No description"

            // Set ACTUAL category name (FIXED!)
            val categoryName = getCategoryName(transaction.categoryId, categories)
            categoryText.text = categoryName

            // Format date
            dateText.text = formatDate(transaction.date)

            // Set amount with color
            if (transaction.type == "INCOME") {
                amountText.text = "+$${String.format("%.2f", transaction.amount)}"
                amountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
            } else {
                amountText.text = "-$${String.format("%.2f", transaction.amount)}"
                amountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
            }
        }

        private fun getCategoryName(categoryId: Long, categories: List<Category>): String {
            return categories.find { it.id == categoryId }?.name ?: "Unknown Category"
        }

        private fun getIconForCategory(categoryId: Long): String {
            // Enhanced icon mapping based on category ID
            return when (categoryId) {
                1L -> "💰" // Salary
                2L -> "💼" // Freelance
                3L -> "🍕" // Food & Dining
                4L -> "🚗" // Transportation
                5L -> "🛍️" // Shopping
                6L -> "🎬" // Entertainment
                7L -> "🏠" // Utilities
                8L -> "🏥" // Healthcare
                else -> "💰"
            }
        }

        private fun formatDate(timestamp: Long): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}