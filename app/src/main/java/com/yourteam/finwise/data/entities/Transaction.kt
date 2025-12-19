package com.yourteam.finwise.data.entities

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val categoryId: Long,
    val description: String? = null,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)