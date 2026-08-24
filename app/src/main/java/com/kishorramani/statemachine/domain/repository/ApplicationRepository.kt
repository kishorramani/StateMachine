package com.kishorramani.statemachine.domain.repository

import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.FinancialApplication
import kotlinx.coroutines.flow.Flow

sealed interface ChaosMode {
    data object Normal : ChaosMode
    data object Timeout : ChaosMode
    data object ServerError500 : ChaosMode
    data object NonRetryableError400 : ChaosMode
    data object ServerAheadMismatch : ChaosMode
}

data class PaymentRemoteResponse(
    val paymentId: String,
    val idempotencyKey: String,
    val status: String,
    val isDuplicate: Boolean = false,
    val errorMessage: String? = null,
    val errorCode: Int = 200
)

interface ApplicationRepository {
    fun getApplicationFlow(id: String): Flow<FinancialApplication?>
    suspend fun getApplication(id: String): FinancialApplication?
    suspend fun saveApplication(application: FinancialApplication)
    fun getAuditLogs(applicationId: String): Flow<List<AuditLog>>
    suspend fun insertAuditLog(log: AuditLog)
    suspend fun clearAuditLogs(applicationId: String)
    
    suspend fun processPaymentRemote(
        applicationId: String,
        paymentId: String,
        idempotencyKey: String,
        amount: Double
    ): Result<PaymentRemoteResponse>

    suspend fun fetchRemotePaymentStatus(paymentId: String): Result<PaymentRemoteResponse>

    fun setChaosMode(mode: ChaosMode)
    fun getChaosMode(): ChaosMode
}
