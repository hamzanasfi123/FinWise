// User.kt
package com.yourteam.finwise.data.entities

data class User(
    val id: Long = 0,
    val email: String,
    val password: String, // In real app, this should be hashed
    val createdAt: Long = System.currentTimeMillis()
)