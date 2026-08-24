package com.kishorramani.statemachine.domain.model

/**
 * Domain entity for the Financial Application lifecycle.
 */
data class FinancialApplication(
    val id: String,
    val applicantName: String,
    val amount: Double,
    val state: ApplicationState,
    val paymentId: String? = null,
    val idempotencyKey: String? = null,
    val failureReason: String? = null,
    val retryCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
