package com.yourteam.finwise.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourteam.finwise.R
import com.yourteam.finwise.data.entities.Debt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebtAdapter : ListAdapter<Debt, DebtAdapter.DebtViewHolder>(DiffCallback) {

    var onItemClick: ((Debt) -> Unit)? = null
    var onAddToCalendar: ((Debt) -> Unit)? = null  // Add this callback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        val debt = getItem(position)
        holder.bind(debt)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(debt)
        }

        // Set calendar button click listener
        holder.calendarButton.setOnClickListener {
            onAddToCalendar?.invoke(debt)
        }

        // Debug logging
        println("DebtAdapter: Binding debt at position $position - ${debt.personName}, $${debt.amount}")
    }

    inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconText: TextView = itemView.findViewById(R.id.debtIcon)
        private val personNameText: TextView = itemView.findViewById(R.id.debtPersonName)
        private val descriptionText: TextView = itemView.findViewById(R.id.debtDescription)
        private val dueDateText: TextView = itemView.findViewById(R.id.debtDueDate)
        private val amountText: TextView = itemView.findViewById(R.id.debtAmount)
        private val statusText: TextView = itemView.findViewById(R.id.debtStatus)
        val calendarButton: ImageView = itemView.findViewById(R.id.btnCalendar)  // This should be here

        fun bind(debt: Debt) {
            // Set icon based on debt type
            val icon = if (debt.debtType == "OWED_TO_ME") "💸" else "📤"
            iconText.text = icon

            // Set person name and description
            personNameText.text = debt.personName
            descriptionText.text = debt.description ?: "No description"

            // Format due date
            dueDateText.text = "Due: ${formatDate(debt.dueDate)}"

            // Set amount with proper sign and color
            if (debt.debtType == "OWED_TO_ME") {
                amountText.text = "+$${String.format("%.2f", debt.amount)}"
                amountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
            } else {
                amountText.text = "-$${String.format("%.2f", debt.amount)}"
                amountText.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
            }

            // Set status and colors
            if (debt.isPaid) {
                statusText.text = "PAID"
                statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                statusText.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
            } else {
                statusText.text = "UNPAID"
                statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                statusText.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))

                // Check if overdue
                if (debt.dueDate < System.currentTimeMillis()) {
                    statusText.text = "OVERDUE"
                    statusText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    statusText.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.red))
                }
            }

            // Debug logging
            println("DebtViewHolder: Displaying debt - ${debt.personName}, $${debt.amount}, ${debt.debtType}")
        }

        private fun formatDate(timestamp: Long): String {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Debt>() {
        override fun areItemsTheSame(oldItem: Debt, newItem: Debt): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Debt, newItem: Debt): Boolean {
            return oldItem == newItem
        }
    }
}