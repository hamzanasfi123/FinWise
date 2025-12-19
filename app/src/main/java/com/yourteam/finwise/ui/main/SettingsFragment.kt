package com.yourteam.finwise.ui.main

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.yourteam.finwise.databinding.FragmentSettingsBinding
import com.yourteam.finwise.utils.SecurityUtils
import com.yourteam.finwise.utils.ThemeUtils
import com.yourteam.finwise.data.database.DatabaseHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = SecurityUtils.getEncryptedPreferences(requireContext())
        setupClickListeners()
        loadCurrentSettings()
    }

    private fun setupClickListeners() {
        // App Preferences
        binding.btnTheme.setOnClickListener { showThemeDialog() }
        binding.btnCurrency.setOnClickListener { showCurrencyDialog() }

        // Notifications - Switch listeners
        binding.switchTransactionAlerts.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference("transaction_alerts", isChecked)
        }
        binding.switchDebtReminders.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference("debt_reminders", isChecked)
        }

        // Data Management
        binding.btnClearData.setOnClickListener { showClearDataConfirmation() }
    }

    private fun loadCurrentSettings() {
        // Load theme setting
        val currentTheme = sharedPreferences.getString("theme", "Light") ?: "Light"
        binding.tvCurrentTheme.text = currentTheme

        // Load currency setting
        val currentCurrency = sharedPreferences.getString("currency", "USD") ?: "USD"
        binding.tvCurrentCurrency.text = currentCurrency

        // Load notification settings
        binding.switchTransactionAlerts.isChecked = sharedPreferences.getBoolean("transaction_alerts", true)
        binding.switchDebtReminders.isChecked = sharedPreferences.getBoolean("debt_reminders", true)

        // Load app version
        binding.tvAppVersion.text = "1.0.0"
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Light", "Dark", "Auto")
        val currentTheme = sharedPreferences.getString("theme", "Auto") ?: "Auto"
        var selectedIndex = themes.indexOf(currentTheme)
        if (selectedIndex == -1) selectedIndex = 2

        AlertDialog.Builder(requireContext())
            .setTitle("Select Theme")
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val selectedTheme = themes[which]
                binding.tvCurrentTheme.text = selectedTheme
                saveSetting("theme", selectedTheme)

                // Apply the theme
                ThemeUtils.applyTheme(selectedTheme)

                // Dismiss dialog and recreate activity immediately
                dialog.dismiss()
                requireActivity().recreate()

                Toast.makeText(requireContext(), "Theme changed to $selectedTheme", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Apply") { dialog, which ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf("USD - US Dollar", "EUR - Euro", "GBP - British Pound", "JPY - Japanese Yen")
        val currentCurrency = sharedPreferences.getString("currency", "USD") ?: "USD"
        var selectedIndex = currencies.indexOfFirst { it.startsWith(currentCurrency) }
        if (selectedIndex == -1) selectedIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencies, selectedIndex) { dialog, which ->
                val selectedCurrency = currencies[which].substring(0, 3)
                binding.tvCurrentCurrency.text = selectedCurrency
                saveSetting("currency", selectedCurrency)
                Toast.makeText(requireContext(), "Currency changed to $selectedCurrency", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("Apply") { dialog, which ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showClearDataConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all your transactions, debts, and categories. This action cannot be undone. Are you sure you want to continue?")
            .setPositiveButton("Delete Everything") { dialog, which ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all your transactions, debts, and categories. This action cannot be undone. Are you sure you want to continue?")
            .setPositiveButton("Delete Everything") { dialog, which ->
                performDataClearance()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDataClearance() {
        try {
            // Get the current logged-in user ID
            val databaseHelper = DatabaseHelper(requireContext())
            val userId = databaseHelper.getCurrentUserId()

            if (userId == -1L) {
                Toast.makeText(requireContext(), "Error: No user logged in", Toast.LENGTH_SHORT).show()
                return
            }

            // Show loading
            Toast.makeText(requireContext(), "Clearing data...", Toast.LENGTH_SHORT).show()

            // Clear user data from database
            val success = databaseHelper.clearUserData(userId)

            if (success) {
                Toast.makeText(requireContext(), "✅ All data has been cleared successfully!", Toast.LENGTH_SHORT).show()

                // Optional: Navigate to dashboard to see empty state
                // Or refresh the current fragment if needed

            } else {
                Toast.makeText(requireContext(), "❌ Failed to clear data. Please try again.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun saveSetting(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun saveNotificationPreference(key: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean(key, enabled).apply()

        // Show feedback for important changes
        when (key) {
            "transaction_alerts" -> {
                val message = if (enabled) "Transaction alerts enabled" else "Transaction alerts disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            "debt_reminders" -> {
                val message = if (enabled) "Debt reminders enabled" else "Debt reminders disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}