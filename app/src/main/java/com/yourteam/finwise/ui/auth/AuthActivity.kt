package com.yourteam.finwise.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.yourteam.finwise.R
import com.yourteam.finwise.ui.main.MainActivity
import com.yourteam.finwise.utils.SecurityUtils

class AuthActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        sharedPreferences = SecurityUtils.getEncryptedPreferences(this)

        if (isUserLoggedIn()) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_auth)

        // Start with WelcomeFragment
        if (savedInstanceState == null) {
            showWelcomeFragment()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getLong("current_user_id", -1) != -1L
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun showWelcomeFragment() {
        replaceFragment(WelcomeFragment())
    }

    fun showLoginFragment() {
        replaceFragment(LoginFragment())
    }

    fun showSignUpFragment() {
        replaceFragment(SignUpFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun onAuthenticationSuccess() {
        startMainActivity()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed() // ✅ FIXED: Added super.onBackPressed()
        }
    }
}