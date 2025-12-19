package com.yourteam.finwise.data.entities

data class Category(
    val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val color: String = "#666666",
    val icon: String = "default_icon",
    val createdAt: Long = System.currentTimeMillis()
)