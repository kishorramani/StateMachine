package com.kishorramani.statemachine.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val applicationId: String,
    val fromState: String,
    val event: String,
    val toState: String,
    val isValid: Boolean,
    val details: String,
    val timestamp: Long
)
