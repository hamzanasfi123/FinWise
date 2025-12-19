package com.yourteam.finwise.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourteam.finwise.R
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.ui.auth.AuthActivity
import com.yourteam.finwise.utils.ThemeUtils
import com.yourteam.finwise.utils.SecurityUtils

class MainActivity : AppCompatActivity() {

    private val TAG = "FinWise"
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView

    // Notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "✅ Notification permission granted")
        } else {
            Log.d(TAG, "❌ Notification permission denied")
            // Don't show warning - user can enable later if they want
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme
        ThemeUtils.initializeTheme(this)

        super.onCreate(savedInstanceState)

        // Check if user is logged in
        sharedPreferences = SecurityUtils.getEncryptedPreferences(this)
        val currentUserId = sharedPreferences.getLong("current_user_id", -1)

        if (currentUserId == -1L) {
            redirectToAuthScreen()
            return
        }

        setContentView(R.layout.activity_main)

        Log.d(TAG, "🚀 MainActivity started! User ID: $currentUserId")

        // Initialize database
        dbHelper = DatabaseHelper(this)

        // Setup bottom navigation
        setupNavigation()

        // Request notification permission if needed (only for Android 13+)
        requestNotificationPermissionIfNeeded()

        Log.d(TAG, "✅ MainActivity setup complete!")
    }

    private fun requestNotificationPermissionIfNeeded() {
        // Only request permission for Android 13 (Tiramisu) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                // Request notification permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                Log.d(TAG, "📢 Requesting notification permission for Android 13+")
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting notification permission", e)
            }
        } else {
            Log.d(TAG, "Android ${Build.VERSION.SDK_INT} doesn't require runtime notification permission")
        }
    }

    private fun redirectToAuthScreen() {
        Log.d(TAG, "⚠️ No user logged in, redirecting to auth screen")
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupNavigation() {
        try {
            bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

            // Get NavHostFragment and NavController
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            // Define which destinations should be considered top-level
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.dashboardFragment,
                    R.id.transactionsFragment,
                    R.id.debtsFragment,
                    R.id.forecastFragment
                )
            )

            // Connect BottomNavigationView with NavController
            bottomNavigationView.setupWithNavController(navController)

            // Set up ActionBar with NavController
            setupActionBarWithNavController(navController, appBarConfiguration)

            Log.d(TAG, "📍 Navigation setup successful")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Navigation setup failed: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showUserProfile()
                true
            }
            R.id.action_settings -> {
                showSettings()
                true
            }
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserProfile() {
        try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            // Create navigation options to clear back stack and set launch mode
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.dashboardFragment, false)
                .build()

            // Navigate to Profile
            navController.navigate(R.id.profileFragment, null, navOptions)

            // Clear bottom navigation selection
            clearBottomNavigationSelection()

            Log.d(TAG, "📍 Navigating to ProfileFragment from menu")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to navigate to ProfileFragment: ${e.message}")
            val userId = sharedPreferences.getLong("current_user_id", -1)
            Toast.makeText(this, "User Profile (ID: $userId)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettings() {
        try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            // Create navigation options to clear back stack and set launch mode
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.dashboardFragment, false)
                .build()

            // Navigate to Settings
            navController.navigate(R.id.settingsFragment, null, navOptions)

            // Clear bottom navigation selection
            clearBottomNavigationSelection()

            Log.d(TAG, "📍 Navigating to SettingsFragment from menu")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to navigate to SettingsFragment: ${e.message}")
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearBottomNavigationSelection() {
        // Temporarily remove the listener to avoid conflicts
        bottomNavigationView.setOnItemSelectedListener(null)

        // Clear the selection
        bottomNavigationView.selectedItemId = -1

        // Restore the listener after a short delay
        bottomNavigationView.postDelayed({
            setupBottomNavigationListener()
        }, 100)
    }

    private fun setupBottomNavigationListener() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboardFragment -> {
                    // Navigate to Dashboard with proper back stack clearing
                    val navOptions = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.dashboardFragment, false)
                        .build()
                    navController.navigate(R.id.dashboardFragment, null, navOptions)
                    true
                }
                R.id.transactionsFragment -> {
                    navController.navigate(R.id.transactionsFragment)
                    true
                }
                R.id.debtsFragment -> {
                    navController.navigate(R.id.debtsFragment)
                    true
                }
                R.id.forecastFragment -> {
                    navController.navigate(R.id.forecastFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        sharedPreferences.edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Double-check user is still logged in when returning to app
        if (!sharedPreferences.getLong("current_user_id", -1).let { it != -1L }) {
            redirectToAuthScreen()
        }
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        val currentDestination = navController?.currentDestination?.id

        when (currentDestination) {
            // If on Profile or Settings (menu destinations)
            R.id.profileFragment, R.id.settingsFragment -> {
                // Navigate back to Dashboard
                navController?.navigate(R.id.dashboardFragment)

                // Restore bottom navigation selection to Dashboard
                bottomNavigationView.selectedItemId = R.id.dashboardFragment
            }
            // If on Dashboard
            R.id.dashboardFragment -> {
                showExitConfirmation()
            }
            // If on any other bottom nav fragment
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        return navController?.navigateUp() ?: false
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit FinWise?")
            .setPositiveButton("Exit") { dialog, which ->
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}