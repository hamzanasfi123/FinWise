package com.yourteam.finwise.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.databinding.FragmentLoginBinding
import com.yourteam.finwise.utils.SecurityUtils

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper(requireContext())

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvSignUp.setOnClickListener {
            (activity as? AuthActivity)?.showSignUpFragment()
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val user = databaseHelper.authenticateUser(email, password)
        if (user != null) {
            val sharedPreferences = SecurityUtils.getEncryptedPreferences(requireContext())
            sharedPreferences.edit().putLong("current_user_id", user.id).apply() // ← ADD THIS LINE

            Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
            (activity as? AuthActivity)?.onAuthenticationSuccess()
        } else {
            Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}