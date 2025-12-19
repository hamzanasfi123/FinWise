package com.yourteam.finwise.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yourteam.finwise.R
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.data.entities.Debt
import com.yourteam.finwise.data.entities.Transaction
import com.yourteam.finwise.databinding.FragmentDashboardBinding
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseHelper = DatabaseHelper(requireContext())
        updateDashboard()
    }

    private fun updateDashboard() {
        val transactions = databaseHelper.getAllTransactions()
        val debts = databaseHelper.getAllDebts()

        val totalBalance = calculateTotalBalance(transactions, debts)
        val safeToSpend = calculateSafeToSpend(transactions, debts)
        val monthlyNet = calculateMonthlyNet(transactions)

        // Update UI with real data
        binding.safeToSpendAmount.text = "$${String.format("%.2f", safeToSpend)}"
        binding.totalBalanceAmount.text = "$${String.format("%.2f", totalBalance)}"

        // Set monthly net with proper color
        if (monthlyNet >= 0) {
            binding.monthlyNetAmount.text = "+$${String.format("%.2f", monthlyNet)}"
            binding.monthlyNetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            binding.monthlyNetAmount.text = "-$${String.format("%.2f", abs(monthlyNet))}"
            binding.monthlyNetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        }
    }

    private fun calculateTotalBalance(transactions: List<Transaction>, debts: List<Debt>): Double {
        var balance = 0.0

        // Calculate from transactions
        transactions.forEach { transaction ->
            if (transaction.type == "INCOME") {
                balance += transaction.amount
            } else {
                balance -= transaction.amount
            }
        }

        // Adjust for debts
        debts.forEach { debt ->
            if (!debt.isPaid) { // Only consider unpaid debts
                if (debt.debtType == "OWED_TO_ME") {
                    // Money owed to you - adds to your effective balance
                    balance += debt.amount
                } else { // "OWED_BY_ME"
                    // Money you owe - subtracts from your effective balance
                    balance -= debt.amount
                }
            }
        }

        return balance
    }

    private fun calculateSafeToSpend(transactions: List<Transaction>, debts: List<Debt>): Double {
        val balance = calculateTotalBalance(transactions, debts)

        if (balance <= 0) return 0.0

        // Calculate upcoming debt payments for the next 7 days
        val upcomingDebtPayments = calculateUpcomingDebtPayments(debts)

        // Safe to spend = (balance - buffer - upcoming payments) / 7
        val buffer = balance * 0.2 // 20% buffer for essentials
        val availableAfterBuffer = balance - buffer
        val availableAfterDebts = availableAfterBuffer - upcomingDebtPayments

        return maxOf(0.0, availableAfterDebts) / 7 // Divide by 7 days
    }

    private fun calculateUpcomingDebtPayments(debts: List<Debt>): Double {
        val nextWeek = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days from now
        var upcomingPayments = 0.0

        debts.forEach { debt ->
            if (!debt.isPaid && debt.debtType == "OWED_BY_ME" && debt.dueDate <= nextWeek) {
                // Only include debts you owe that are due in the next 7 days
                upcomingPayments += debt.amount
            }
        }

        return upcomingPayments
    }

    private fun calculateMonthlyNet(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var monthlyNet = 0.0

        transactions.forEach { transaction ->
            val transactionCalendar = Calendar.getInstance().apply {
                timeInMillis = transaction.date
            }

            val transactionMonth = transactionCalendar.get(Calendar.MONTH)
            val transactionYear = transactionCalendar.get(Calendar.YEAR)

            if (transactionMonth == currentMonth && transactionYear == currentYear) {
                if (transaction.type == "INCOME") {
                    monthlyNet += transaction.amount
                } else {
                    monthlyNet -= transaction.amount
                }
            }
        }
        return monthlyNet
    }

    private fun abs(value: Double): Double {
        return if (value < 0) -value else value
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateDashboard() // Refresh data when returning to this fragment
    }
}