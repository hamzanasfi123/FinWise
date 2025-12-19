package com.yourteam.finwise.ui.main

import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yourteam.finwise.R
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.databinding.DialogChangePasswordBinding
import com.yourteam.finwise.databinding.FragmentProfileBinding
import com.yourteam.finwise.ui.auth.AuthActivity
import com.yourteam.finwise.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var profileImageView: ImageView
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { handleCameraPhoto(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper(requireContext())
        loadUserProfile()
        setupClickListeners()
        profileImageView = binding.ivProfilePicture
        setupProfilePictureClickListener()
    }

    private fun setupProfilePictureClickListener() {
        profileImageView.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle("Update Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Check and request camera permission
                        if (ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            // Permission already granted, launch camera
                            takePhoto.launch(null)
                        } else {
                            // Request permission
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> {
                        // For gallery, permission might also be needed on older Android versions
                        try {
                            pickImage.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Cannot access gallery", Toast.LENGTH_SHORT).show()
                            Log.e("ProfileFragment", "Gallery error", e)
                        }
                    }
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Save and display the image
            saveProfilePicture(bitmap)
            profileImageView.setImageBitmap(bitmap)

            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            Log.e("ProfileFragment", "Image loading error", e)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch camera
            takePhoto.launch(null)
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCameraPhoto(bitmap: Bitmap) {
        // Save and display the image
        saveProfilePicture(bitmap)
        profileImageView.setImageBitmap(bitmap)

        Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
    }

    private fun saveProfilePicture(bitmap: Bitmap) {
        try {
            // Save to internal storage
            val file = File(requireContext().filesDir, "profile_picture.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            // Save file path to SharedPreferences
            val sharedPreferences = SecurityUtils.getEncryptedPreferences(requireContext())
            sharedPreferences.edit()
                .putString("profile_picture_path", file.absolutePath)
                .apply()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Failed to save profile picture", e)
        }
    }

    private fun loadProfilePicture() {
        try {
            val sharedPreferences = SecurityUtils.getEncryptedPreferences(requireContext())
            val filePath = sharedPreferences.getString("profile_picture_path", null)

            if (!filePath.isNullOrEmpty()) {
                val file = File(filePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    profileImageView.setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Failed to load profile picture", e)
        }
    }

    private fun loadUserProfile() {
        val userId = databaseHelper.getCurrentUserId()
        if (userId != -1L) {
            // Show loading state (optional)
            binding.tvUserEmail.text = "Loading..."

            // Load user details on background thread
            lifecycleScope.launch {
                try {
                    val user = withContext(Dispatchers.IO) {
                        databaseHelper.getUserById(userId)
                    }

                    val transactionCount = withContext(Dispatchers.IO) {
                        databaseHelper.getTransactionCount() // Use count instead
                    }

                    val debtCount = withContext(Dispatchers.IO) {
                        databaseHelper.getDebtCount() // Use count instead
                    }

                    withContext(Dispatchers.Main) {
                        // Update user info
                        user?.let {
                            binding.tvUserEmail.text = it.email
                            binding.tvMemberSince.text = "Member since ${formatMemberSinceDate(it.createdAt)}"
                        }

                        // Update counts directly (much faster!)
                        binding.tvTransactionCount.text = transactionCount.toString()
                        binding.tvDebtCount.text = debtCount.toString()
                    }

                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error loading user profile", e)
                    withContext(Dispatchers.Main) {
                        binding.tvUserEmail.text = "Unable to load email"
                        binding.tvMemberSince.text = "Member since Unknown"
                    }
                }
            }
        } else {
            // No user logged in
            binding.tvUserEmail.text = "Not logged in"
            binding.tvMemberSince.text = "Please log in"
        }
    }

    private fun formatMemberSinceDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun calculateFinancialStats(transactions: List<com.yourteam.finwise.data.entities.Transaction>, debts: List<com.yourteam.finwise.data.entities.Debt>) {
        // Calculate total income
        val totalIncome = transactions
            .filter { it.type == "INCOME" }
            .sumOf { it.amount }

        // Calculate total expenses
        val totalExpenses = transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { it.amount }

        // Calculate net balance
        val netBalance = totalIncome - totalExpenses

        // Calculate money owed to you
        val moneyOwedToYou = debts
            .filter { it.debtType == "OWED_TO_ME" && !it.isPaid }
            .sumOf { it.amount }

        // Calculate money you owe
        val moneyYouOwe = debts
            .filter { it.debtType == "OWED_BY_ME" && !it.isPaid }
            .sumOf { it.amount }

        // You can display these stats in the UI if you add more TextViews
        println("Financial Stats:")
        println("Total Income: $$totalIncome")
        println("Total Expenses: $$totalExpenses")
        println("Net Balance: $$netBalance")
        println("Money Owed to You: $$moneyOwedToYou")
        println("Money You Owe: $$moneyYouOwe")
    }

    private fun setupClickListeners() {
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Remove the Settings button click listener - handled by menu
        // binding.btnSettings.setOnClickListener {
        //     showSettings()
        // }
    }

    private fun showChangePasswordDialog() {
        // Inflate using ViewBinding
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogBinding.root)
            .setPositiveButton("Change Password") { dialog, which ->
                // We'll handle this in onShow to prevent auto-dismiss
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                attemptPasswordChange(
                    dialogBinding.etCurrentPassword.text.toString(),
                    dialogBinding.etNewPassword.text.toString(),
                    dialogBinding.etConfirmNewPassword.text.toString(),
                    dialog
                )
            }
        }

        dialog.show()
    }

    private fun attemptPasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        dialog: AlertDialog
    ) {
        // Validation
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "New passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPassword == newPassword) {
            Toast.makeText(requireContext(), "New password must be different", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user ID
        val userId = databaseHelper.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        Toast.makeText(requireContext(), "Changing password...", Toast.LENGTH_SHORT).show()

        // Attempt password change
        val success = databaseHelper.changePassword(userId, currentPassword, newPassword)

        if (success) {
            Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            // Optional: Log user out for security
            showLogoutConfirmation()
        } else {
            Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Security Notice")
            .setMessage("For security reasons, it's recommended to log out and log back in after changing your password. Would you like to log out now?")
            .setPositiveButton("Log Out") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Stay Logged In") { dialog, which ->
                // User chooses to stay logged in
            }
            .show()
    }

    private fun performLogout() {
        val sharedPreferences = SecurityUtils.getEncryptedPreferences(requireContext())
        sharedPreferences.edit().clear().apply()

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate to AuthActivity
        val intent = Intent(requireContext(), AuthActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun exportUserData() {
        // TODO: Implement data export functionality
        Toast.makeText(requireContext(), "Export Data feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    // Remove showSettings method - handled by MainActivity menu
    // private fun showSettings() { ... }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadUserProfile()
        loadProfilePicture()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}