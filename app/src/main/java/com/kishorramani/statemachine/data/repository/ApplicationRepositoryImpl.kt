package com.kishorramani.statemachine.data.repository

import com.kishorramani.statemachine.data.local.ApplicationDao
import com.kishorramani.statemachine.data.local.ApplicationEntity
import com.kishorramani.statemachine.data.local.AuditLogEntity
import com.kishorramani.statemachine.data.remote.FinancialApi
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.FinancialApplication
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.repository.ChaosMode
import com.kishorramani.statemachine.domain.repository.PaymentRemoteResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ApplicationRepositoryImpl(
    private val dao: ApplicationDao,
    private val api: FinancialApi
) : ApplicationRepository {

    override fun getApplicationFlow(id: String): Flow<FinancialApplication?> {
        return dao.getApplicationFlow(id).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun getApplication(id: String): FinancialApplication? {
        return dao.getApplication(id)?.toDomainModel()
    }

    override suspend fun saveApplication(application: FinancialApplication) {
        dao.insertOrUpdateApplication(application.toEntity())
    }

    override fun getAuditLogs(applicationId: String): Flow<List<AuditLog>> {
        return dao.getAuditLogsFlow(applicationId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun insertAuditLog(log: AuditLog) {
        dao.insertAuditLog(log.toEntity())
    }

    override suspend fun clearAuditLogs(applicationId: String) {
        dao.clearAuditLogs(applicationId)
    }

    override suspend fun processPaymentRemote(
        applicationId: String,
        paymentId: String,
        idempotencyKey: String,
        amount: Double
    ): Result<PaymentRemoteResponse> {
        return try {
            val response = api.processPayment(applicationId, paymentId, idempotencyKey, amount)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchRemotePaymentStatus(paymentId: String): Result<PaymentRemoteResponse> {
        return try {
            val response = api.getPaymentStatus(paymentId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setChaosMode(mode: ChaosMode) {
        api.setChaosMode(mode)
    }

    override fun getChaosMode(): ChaosMode = api.getChaosMode()

    private fun ApplicationEntity.toDomainModel(): FinancialApplication {
        return FinancialApplication(
            id = id,
            applicantName = applicantName,
            amount = amount,
            state = state.toApplicationState(),
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            failureReason = failureReason,
            retryCount = retryCount,
            updatedAt = updatedAt
        )
    }

    private fun FinancialApplication.toEntity(): ApplicationEntity {
        return ApplicationEntity(
            id = id,
            applicantName = applicantName,
            amount = amount,
            state = state.displayName(),
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            failureReason = failureReason,
            retryCount = retryCount,
            updatedAt = updatedAt
        )
    }

    private fun AuditLogEntity.toDomainModel(): AuditLog {
        return AuditLog(
            id = id,
            applicationId = applicationId,
            fromState = fromState,
            event = event,
            toState = toState,
            isValid = isValid,
            details = details,
            timestamp = timestamp
        )
    }

    private fun AuditLog.toEntity(): AuditLogEntity {
        return AuditLogEntity(
            id = id,
            applicationId = applicationId,
            fromState = fromState,
            event = event,
            toState = toState,
            isValid = isValid,
            details = details,
            timestamp = timestamp
        )
    }

    private fun String.toApplicationState(): ApplicationState = when (this) {
        "Draft" -> ApplicationState.Draft
        "Submitted" -> ApplicationState.Submitted
        "Document Verification" -> ApplicationState.DocumentVerification
        "Verification Failed" -> ApplicationState.VerificationFailed
        "Credit Checking" -> ApplicationState.CreditChecking
        "Approved" -> ApplicationState.Approved
        "Rejected" -> ApplicationState.Rejected
        "Payment Pending" -> ApplicationState.PaymentPending
        "Payment Processing" -> ApplicationState.PaymentProcessing
        "Payment Success" -> ApplicationState.PaymentSuccess
        "Payment Failed" -> ApplicationState.PaymentFailed
        "Completed" -> ApplicationState.Completed
        else -> ApplicationState.Draft
    }
}
