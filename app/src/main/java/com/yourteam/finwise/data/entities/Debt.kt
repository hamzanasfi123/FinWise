package com.yourteam.finwise.data.entities

// Update your Debt.kt data class
data class Debt(
    val id: Long = 0,
    val personName: String,
    val amount: Double,
    val debtType: String, // "OWED_TO_ME" or "OWED_BY_ME"
    val dueDate: Long,    // Original due date
    val payDate: Long? = null, // ADD THIS - actual payment date
    val description: String? = null,
    val isPaid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)