package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    COLLECTOR
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val mobile: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
