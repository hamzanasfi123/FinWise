package com.yourteam.finwise.ui.main

import android.R
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.data.entities.Transaction
import com.yourteam.finwise.databinding.FragmentTransactionsBinding
import com.yourteam.finwise.ui.adapters.TransactionAdapter

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("DEBUG: TransactionsFragment - onCreate()")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        println("DEBUG: TransactionsFragment - onCreateView()")
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("DEBUG: TransactionsFragment - onViewCreated()")

        databaseHelper = DatabaseHelper(requireContext())
        setupRecyclerView()
        loadTransactions()

        binding.addTransactionButton.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        println("DEBUG: TransactionsFragment - onResume()")
        loadTransactions()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecyclerView.adapter = adapter

        // Load categories and set them in the adapter
        val categories = databaseHelper.getAllCategories()
        adapter.setCategories(categories)

        adapter.onItemClick = { transaction ->
            Toast.makeText(requireContext(), "Clicked: ${transaction.description}", Toast.LENGTH_SHORT).show()
        }

        println("DEBUG: RecyclerView setup complete")
    }

    private fun loadTransactions() {
        println("DEBUG: loadTransactions() called")

        val transactions = databaseHelper.getAllTransactions()
        println("DEBUG: Found ${transactions.size} transactions in database")

        adapter.submitList(transactions)
        adapter.notifyDataSetChanged()

        if (transactions.isNotEmpty()) {
            Toast.makeText(requireContext(), "Loaded ${transactions.size} transactions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddTransactionDialog() {
        // Create a simple input dialog
        val alertDialog = AlertDialog.Builder(requireContext())

        // Set title
        alertDialog.setTitle("Add New Transaction")

        // Create input fields programmatically
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Description Input
        val descriptionInput = EditText(requireContext())
        descriptionInput.hint = "Description"
        descriptionInput.setSingleLine(true)
        layout.addView(descriptionInput)

        // Add some space
        val space1 = View(requireContext())
        space1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            30
        )
        layout.addView(space1)

        // Amount Input
        val amountInput = EditText(requireContext())
        amountInput.hint = "Amount"
        amountInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(amountInput)

        // Add some space
        val space2 = View(requireContext())
        space2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            30
        )
        layout.addView(space2)

        // Transaction Type Selection
        val transactionTypeLabel = TextView(requireContext())
        transactionTypeLabel.text = "Transaction Type:"
        transactionTypeLabel.setTextColor(requireContext().getColor(R.color.darker_gray))
        layout.addView(transactionTypeLabel)

        val transactionTypeSpinner = Spinner(requireContext())
        val transactionTypes = arrayOf("💸 Send (Expense)", "💰 Receive (Income)")
        val typeAdapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, transactionTypes)
        typeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        transactionTypeSpinner.adapter = typeAdapter
        layout.addView(transactionTypeSpinner)

        // Add some space
        val space3 = View(requireContext())
        space3.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            30
        )
        layout.addView(space3)

        // Category Selection
        val categoryLabel = TextView(requireContext())
        categoryLabel.text = "Category:"
        categoryLabel.setTextColor(requireContext().getColor(R.color.darker_gray))
        layout.addView(categoryLabel)

        val categorySpinner = Spinner(requireContext())
        val categories = databaseHelper.getAllCategories()
        val categoryNames = categories.map { it.name }.toTypedArray()
        val categoryAdapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        layout.addView(categorySpinner)

        // Set the custom layout
        alertDialog.setView(layout)

        // Set up the buttons - don't auto-dismiss
        alertDialog.setPositiveButton("Save", null)
        alertDialog.setNegativeButton("Cancel", null)

        val dialog = alertDialog.create()

        // Set up button click listeners AFTER creating the dialog
        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            saveButton.setOnClickListener {
                val description = descriptionInput.text.toString().trim()
                val amountText = amountInput.text.toString().trim()
                val selectedTransactionType = transactionTypeSpinner.selectedItemPosition
                val selectedCategoryIndex = categorySpinner.selectedItemPosition

                // Validation
                if (description.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (amountText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = try {
                    amountText.toDouble()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (categories.isEmpty()) {
                    Toast.makeText(requireContext(), "No categories available", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Determine transaction type and category
                val transactionType = if (selectedTransactionType == 0) "EXPENSE" else "INCOME"
                val selectedCategory = categories[selectedCategoryIndex]

                // Create and save transaction
                val newTransaction = Transaction(
                    amount = amount,
                    type = transactionType,
                    categoryId = selectedCategory.id,
                    description = description,
                    date = System.currentTimeMillis()
                )

                val transactionId = databaseHelper.addTransaction(newTransaction)
                if (transactionId != -1L) {
                    Toast.makeText(requireContext(), "Transaction added successfully!", Toast.LENGTH_SHORT).show()
                    loadTransactions()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to add transaction", Toast.LENGTH_SHORT).show()
                }
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}