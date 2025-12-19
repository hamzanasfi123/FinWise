package com.yourteam.finwise.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yourteam.finwise.R
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.data.entities.Transaction
import com.yourteam.finwise.databinding.FragmentForecastBinding
import java.util.Calendar
import kotlin.math.abs

class ForecastFragment : Fragment() {

    private var _binding: FragmentForecastBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseHelper = DatabaseHelper(requireContext())
        updateForecast()
    }

    private fun updateForecast() {
        val transactions = databaseHelper.getAllTransactions()
        val sevenDayForecast = calculateSevenDayForecast(transactions)

        // Update main projection card
        binding.projectedBalanceAmount.text = "$${String.format("%.2f", sevenDayForecast.projectedBalance)}"

        val difference = sevenDayForecast.projectedBalance - sevenDayForecast.currentBalance
        if (difference >= 0) {
            binding.balanceDifference.text = "+$${String.format("%.2f", abs(difference))} from current balance"
            binding.balanceDifference.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            binding.balanceDifference.text = "-$${String.format("%.2f", abs(difference))} from current balance"
            binding.balanceDifference.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        }

        // Update insights
        binding.forecastInsights.text = generateForecastInsights(sevenDayForecast, transactions)

        // Update breakdown section
        binding.projectedIncomeAmount.text = "+$${String.format("%.2f", sevenDayForecast.projectedIncome)}"
        binding.projectedExpensesAmount.text = "-$${String.format("%.2f", sevenDayForecast.projectedExpenses)}"
    }

    private fun calculateSevenDayForecast(transactions: List<Transaction>): SevenDayForecast {
        val currentBalance = calculateCurrentBalance(transactions)

        // Calculate average daily income and expenses from last 30 days
        val dailyAverages = calculateDailyAverages(transactions)
        val avgDailyIncome = dailyAverages.first
        val avgDailyExpense = dailyAverages.second

        // Project next 7 days
        val daysToProject = 7
        val projectedIncome = avgDailyIncome * daysToProject
        val projectedExpenses = avgDailyExpense * daysToProject
        val projectedNet = projectedIncome - projectedExpenses
        val projectedBalance = currentBalance + projectedNet

        return SevenDayForecast(
            currentBalance = currentBalance,
            projectedBalance = projectedBalance,
            projectedIncome = projectedIncome,
            projectedExpenses = projectedExpenses,
            projectedNet = projectedNet,
            avgDailyIncome = avgDailyIncome,
            avgDailyExpense = avgDailyExpense
        )
    }

    private fun calculateCurrentBalance(transactions: List<Transaction>): Double {
        var balance = 0.0
        transactions.forEach { transaction ->
            if (transaction.type == "INCOME") {
                balance += transaction.amount
            } else {
                balance -= transaction.amount
            }
        }
        return balance
    }

    private fun calculateDailyAverages(transactions: List<Transaction>): Pair<Double, Double> {
        val calendar = Calendar.getInstance()
        val thirtyDaysAgo = calendar.apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis

        // Filter transactions from last 30 days
        val recentTransactions = transactions.filter { it.date >= thirtyDaysAgo }

        var totalIncome = 0.0
        var totalExpenses = 0.0

        recentTransactions.forEach { transaction ->
            if (transaction.type == "INCOME") {
                totalIncome += transaction.amount
            } else {
                totalExpenses += transaction.amount
            }
        }

        // Calculate 30-day averages
        val avgDailyIncome = totalIncome / 30
        val avgDailyExpense = totalExpenses / 30

        return Pair(avgDailyIncome, avgDailyExpense)
    }

    private fun generateForecastInsights(forecast: SevenDayForecast, transactions: List<Transaction>): String {
        return when {
            forecast.projectedNet > 100 -> {
                "Great job! You're on track to save $${String.format("%.2f", forecast.projectedNet)} this week. " +
                        "Consider putting some into savings!"
            }
            forecast.projectedNet > 0 -> {
                "You're on track to save $${String.format("%.2f", forecast.projectedNet)} this week. " +
                        "Keep up the good financial habits!"
            }
            forecast.projectedNet < -50 -> {
                "Watch out! You're projected to spend $${String.format("%.2f",
                    abs(forecast.projectedNet)
                )} more than you earn this week. " +
                        "Consider reviewing your expenses."
            }
            else -> {
                "Based on your spending patterns, you're breaking even this week. " +
                        "Look for small ways to save more!"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateForecast()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class SevenDayForecast(
        val currentBalance: Double,
        val projectedBalance: Double,
        val projectedIncome: Double,
        val projectedExpenses: Double,
        val projectedNet: Double,
        val avgDailyIncome: Double,
        val avgDailyExpense: Double
    )
}