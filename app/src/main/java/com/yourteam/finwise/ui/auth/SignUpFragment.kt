package com.yourteam.finwise.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.data.entities.User
import com.yourteam.finwise.databinding.FragmentSignUpBinding
import at.favre.lib.crypto.bcrypt.BCrypt
import com.yourteam.finwise.utils.SecurityUtils
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper(requireContext())

        binding.btnSignUp.setOnClickListener {
            attemptSignUp()
        }

        binding.tvSignUp.setOnClickListener {
            (activity as? AuthActivity)?.showLoginFragment()
        }
    }

    private fun attemptSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user already exists
        if (databaseHelper.doesUserExist(email)) {
            Toast.makeText(requireContext(), "User already exists", Toast.LENGTH_SHORT).show()
            return
        }

        // Create new user
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = User(email = email, password = hashedPassword)
        val userId = databaseHelper.addUser(user)

        if (userId != -1L) {
            // Save user session
            val sharedPreferences = SecurityUtils.getEncryptedPreferences(requireContext()) // ← FIXED LINE
            sharedPreferences.edit().putLong("current_user_id", userId).apply()

            Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
            (activity as? AuthActivity)?.onAuthenticationSuccess()
        } else {
            Toast.makeText(requireContext(), "Failed to create account", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}