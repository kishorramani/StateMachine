package com.kishorramani.statemachine.domain.model

/**
 * Domain model representing an immutable audit log entry of a state transition or transition attempt.
 */
data class AuditLog(
    val id: Long = 0,
    val applicationId: String,
    val fromState: String,
    val event: String,
    val toState: String,
    val isValid: Boolean,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
