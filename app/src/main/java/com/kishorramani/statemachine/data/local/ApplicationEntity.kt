package com.kishorramani.statemachine.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kishorramani.statemachine.domain.model.ApplicationState

@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey
    val id: String,
    val applicantName: String,
    val amount: Double,
    val state: String,
    val paymentId: String?,
    val idempotencyKey: String?,
    val failureReason: String?,
    val retryCount: Int,
    val updatedAt: Long
)
