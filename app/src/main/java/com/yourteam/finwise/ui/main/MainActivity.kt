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
import androidx.navigation.fragment.NavHostFragment
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
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

            // Get NavHostFragment and NavController
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            // Connect BottomNavigationView with NavController
            bottomNavigationView.setupWithNavController(navController)

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
            navController.navigate(R.id.profileFragment)
            Log.d(TAG, "📍 Navigating to ProfileFragment")
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
            navController.navigate(R.id.settingsFragment)
            Log.d(TAG, "📍 Navigating to SettingsFragment")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to navigate to SettingsFragment: ${e.message}")
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
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

        if (navController?.currentDestination?.id != R.id.dashboardFragment) {
            super.onBackPressed()
        } else {
            showExitConfirmation()
        }
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