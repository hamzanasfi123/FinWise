package com.yourteam.finwise.ui.main

import android.R
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Color
import android.os.Bundle
import android.provider.CalendarContract
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourteam.finwise.data.database.DatabaseHelper
import com.yourteam.finwise.data.entities.Debt
import com.yourteam.finwise.databinding.FragmentDebtsBinding
import com.yourteam.finwise.ui.adapters.DebtAdapter
import com.yourteam.finwise.utils.NotificationHelper
import android.Manifest
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DebtsFragment : Fragment() {

    private var _binding: FragmentDebtsBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: DebtAdapter

    // Calendar permission launcher
    private val requestCalendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(requireContext(), "Calendar permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Calendar permission denied", Toast.LENGTH_LONG).show()
        }
    }

    // ========== PERMISSION CHECK METHODS ==========
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCalendarPermission() {
        requestCalendarPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }
    // ==============================================

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper(requireContext())
        setupRecyclerView()
        loadDebts()

        binding.addDebtButton.setOnClickListener {
            showAddDebtDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = DebtAdapter()
        binding.debtsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.debtsRecyclerView.adapter = adapter

        // Add item decoration for spacing
        binding.debtsRecyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        )

        adapter.onItemClick = { debt ->
            // Toggle paid status when clicked
            toggleDebtStatus(debt)
        }
        adapter.onAddToCalendar = { debt ->
            addDueDateToCalendar(debt)
        }

        Log.d("DebtsFragment", "RecyclerView setup complete")
    }

    private fun loadDebts() {
        val debts = databaseHelper.getAllDebts()
        adapter.submitList(debts)

        // Show how many debts loaded
        if (debts.isNotEmpty()) {
            Toast.makeText(requireContext(), "Loaded ${debts.size} debts - Scroll to see all", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAddDebtDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Add New Debt")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Person Name Input
        val personNameInput = EditText(requireContext())
        personNameInput.hint = "Person Name"
        personNameInput.setSingleLine(true)
        layout.addView(personNameInput)

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

        // Debt Type Selection
        val debtTypeLabel = TextView(requireContext())
        debtTypeLabel.text = "Debt Type:"
        debtTypeLabel.setTextColor(requireContext().getColor(R.color.darker_gray))
        layout.addView(debtTypeLabel)

        val debtTypeSpinner = Spinner(requireContext())
        val debtTypes = arrayOf("I Owe Money", "Money Owed To Me")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, debtTypes)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        debtTypeSpinner.adapter = adapter
        layout.addView(debtTypeSpinner)

        // Add some space
        val space3 = View(requireContext())
        space3.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            30
        )
        layout.addView(space3)

        // DUE DATE Button
        val dueDateLabel = TextView(requireContext())
        dueDateLabel.text = "Due Date:"
        dueDateLabel.setTextColor(requireContext().getColor(R.color.darker_gray))
        layout.addView(dueDateLabel)

        val dueDateButton = Button(requireContext())
        dueDateButton.text = "📅 Select Due Date"
        dueDateButton.setBackgroundColor(Color.TRANSPARENT)
        dueDateButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.darker_gray))

        var selectedDueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // Default: 7 days from now
        dueDateButton.setOnClickListener {
            showDebtDatePickerDialog(selectedDueDate) { selectedDate ->
                selectedDueDate = selectedDate
                dueDateButton.text = "📅 Due: ${formatDate(selectedDate)}"
            }
        }
        layout.addView(dueDateButton)

        // Add space
        val space4 = View(requireContext())
        space4.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20)
        layout.addView(space4)

        // PAYMENT DATE Button (Optional)
        val payDateLabel = TextView(requireContext())
        payDateLabel.text = "Payment Date (Optional):"
        payDateLabel.setTextColor(requireContext().getColor(R.color.darker_gray))
        layout.addView(payDateLabel)

        val payDateButton = Button(requireContext())
        payDateButton.text = "📅 Set Payment Date (Optional)"
        payDateButton.setBackgroundColor(Color.TRANSPARENT)
        payDateButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.darker_gray))

        var selectedPayDate: Long? = null
        payDateButton.setOnClickListener {
            showDebtDatePickerDialog(selectedPayDate ?: System.currentTimeMillis()) { selectedDate ->
                selectedPayDate = selectedDate
                payDateButton.text = "📅 Payment: ${formatDate(selectedDate)}"
            }
        }
        layout.addView(payDateButton)

        // Add space
        val space5 = View(requireContext())
        space5.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20)
        layout.addView(space5)

        // Description Input
        val descriptionInput = EditText(requireContext())
        descriptionInput.hint = "Description (Optional)"
        layout.addView(descriptionInput)

        // Add to Calendar Checkbox (Optional)
        val addToCalendarLabel = TextView(requireContext())
        addToCalendarLabel.text = "Add to Android Calendar:"
        addToCalendarLabel.setTextColor(requireContext().getColor(R.color.darker_gray))
        layout.addView(addToCalendarLabel)

        val addToCalendarCheckbox = android.widget.CheckBox(requireContext())
        addToCalendarCheckbox.text = "Add due date to calendar"
        addToCalendarCheckbox.isChecked = true // Default checked
        layout.addView(addToCalendarCheckbox)

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
                println("DEBUG: Save button clicked!")

                val personName = personNameInput.text.toString().trim()
                val amountText = amountInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val selectedDebtType = debtTypeSpinner.selectedItemPosition
                val addToCalendar = addToCalendarCheckbox.isChecked

                println("DEBUG: Input values - Name: '$personName', Amount: '$amountText', Type: $selectedDebtType")

                // Validation
                if (personName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a person name", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Validation failed - empty name")
                    return@setOnClickListener
                }

                if (amountText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Validation failed - empty amount")
                    return@setOnClickListener
                }

                val amount = try {
                    amountText.toDouble()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Validation failed - invalid amount: $amountText")
                    return@setOnClickListener
                }

                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Validation failed - amount <= 0: $amount")
                    return@setOnClickListener
                }

                // Determine debt type
                val debtType = if (selectedDebtType == 0) "OWED_BY_ME" else "OWED_TO_ME"

                println("DEBUG: Creating debt object...")

                // Create and save debt WITH due date and optional pay date
                val newDebt = Debt(
                    personName = personName,
                    amount = amount,
                    debtType = debtType,
                    dueDate = selectedDueDate, // Use selected due date
                    payDate = selectedPayDate, // Use selected payment date (can be null)
                    description = if (description.isEmpty()) null else description,
                    isPaid = selectedPayDate != null // Auto-mark as paid if payment date is set
                )

                println("DEBUG: Debt object created: ${newDebt.personName}, $${newDebt.amount}, ${newDebt.debtType}, Due: ${formatDate(newDebt.dueDate)}, Pay: ${newDebt.payDate?.let { formatDate(it) }}")

                println("DEBUG: Calling databaseHelper.addDebt()...")
                val debtId = databaseHelper.addDebt(newDebt)
                println("DEBUG: databaseHelper.addDebt() returned: $debtId")

                if (debtId != -1L) {
                    println("DEBUG: Debt saved successfully with ID: $debtId")

                    // Schedule notification if payDate is set and debt is not paid
                    if (newDebt.payDate != null && !newDebt.isPaid) {
                        schedulePaymentNotification(newDebt.copy(id = debtId))
                    }

                    // Add to calendar if checkbox is checked
                    if (addToCalendar) {
                        addDueDateToCalendar(newDebt.copy(id = debtId))
                    }

                    val message = if (newDebt.isPaid) {
                        "Debt added and marked as paid!"
                    } else {
                        "Debt added successfully!"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                    loadDebts()
                    dialog.dismiss()
                } else {
                    println("DEBUG: Debt save FAILED - returned -1")
                    Toast.makeText(requireContext(), "Failed to add debt", Toast.LENGTH_SHORT).show()
                }
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
        println("DEBUG: Dialog shown successfully")
    }

    private fun toggleDebtStatus(debt: Debt) {
        if (!debt.isPaid) {
            // Marking as paid - show date picker for payment date
            showPaymentDatePicker(debt) { selectedDate ->
                val updatedDebt = debt.copy(
                    isPaid = true,
                    payDate = selectedDate
                )

                // Schedule notification for payment date
                schedulePaymentNotification(updatedDebt)

                updateDebtInDatabase(updatedDebt)
            }
        } else {
            // Marking as unpaid - clear payment date
            val updatedDebt = debt.copy(
                isPaid = false,
                payDate = null
            )

            // Cancel any existing notification
            cancelPaymentNotification(debt.id)

            updateDebtInDatabase(updatedDebt)
        }
    }

    private fun showPaymentDatePicker(debt: Debt, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                onDateSelected(selectedCalendar.timeInMillis)

                Toast.makeText(requireContext(),
                    "Debt marked as paid on ${formatDate(selectedCalendar.timeInMillis)}",
                    Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showDebtDatePickerDialog(currentDate: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDate
        }

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                onDateSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun updateDebtInDatabase(debt: Debt) {
        val rowsUpdated = databaseHelper.updateDebt(debt)
        if (rowsUpdated > 0) {
            // If debt has payDate and is not paid, schedule notification
            if (debt.payDate != null && !debt.isPaid) {
                schedulePaymentNotification(debt)
            }
            // If debt is paid or has no payDate, cancel notification
            else {
                cancelPaymentNotification(debt.id)
            }

            loadDebts()
        } else {
            Toast.makeText(requireContext(), "Failed to update debt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    // ==================== CALENDAR METHODS ====================

    private fun addDueDateToCalendar(debt: Debt) {
        // Check permission first
        if (!hasCalendarPermission()) {
            Toast.makeText(
                requireContext(),
                "Calendar permission needed to add due dates. Please allow calendar access.",
                Toast.LENGTH_LONG
            ).show()
            requestCalendarPermission()
            return
        }

        try {
            val contentResolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, debt.dueDate)
                put(CalendarContract.Events.DTEND, debt.dueDate + (60 * 60 * 1000)) // 1 hour later
                put(CalendarContract.Events.TITLE, "Debt Payment: ${debt.personName}")
                put(CalendarContract.Events.DESCRIPTION,
                    "Amount: $${String.format("%.2f", debt.amount)}\n" +
                            "Type: ${if (debt.debtType == "OWED_BY_ME") "You Owe" else "Owed to You"}\n" +
                            "${debt.description ?: "No description"}")
                put(CalendarContract.Events.CALENDAR_ID, 1) // Default calendar
                put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().timeZone.id)
                put(CalendarContract.Events.EVENT_LOCATION, "FinWise App")
                put(CalendarContract.Events.HAS_ALARM, 1) // Enable reminders
            }

            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

            if (uri != null) {
                // Add reminder (1 day before)
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(uri))
                    put(CalendarContract.Reminders.MINUTES, 24 * 60) // 1 day before (24 hours * 60 minutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)

                Toast.makeText(
                    requireContext(),
                    "✅ Due date added to calendar with reminder",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("DebtsFragment", "Added debt to calendar: ${debt.personName}, ${formatDate(debt.dueDate)}")
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to add to calendar. Make sure you have a calendar app installed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Calendar permission denied. Please enable in settings",
                Toast.LENGTH_LONG
            ).show()
            Log.e("DebtsFragment", "Calendar permission error", e)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error adding to calendar: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("DebtsFragment", "Calendar error", e)
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    private fun schedulePaymentNotification(debt: Debt) {
        debt.payDate?.let { payDate ->
            if (!debt.isPaid && payDate > System.currentTimeMillis()) {
                try {
                    val notificationHelper = NotificationHelper(requireContext())
                    notificationHelper.schedulePaymentNotification(
                        debt.id,
                        debt.personName,
                        debt.amount,
                        payDate
                    )
                    Log.d("DebtsFragment", "Scheduled notification for debt: ${debt.personName} at ${formatDate(payDate)}")
                } catch (e: Exception) {
                    Log.e("DebtsFragment", "Failed to schedule notification", e)
                }
            }
        }
    }

    private fun cancelPaymentNotification(debtId: Long) {
        try {
            val notificationHelper = NotificationHelper(requireContext())
            notificationHelper.cancelPaymentNotification(debtId)
            Log.d("DebtsFragment", "Cancelled notification for debt ID: $debtId")
        } catch (e: Exception) {
            Log.e("DebtsFragment", "Failed to cancel notification", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}