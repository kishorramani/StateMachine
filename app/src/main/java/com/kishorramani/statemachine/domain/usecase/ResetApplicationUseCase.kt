package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.FinancialApplication
import com.kishorramani.statemachine.domain.repository.ApplicationRepository

class ResetApplicationUseCase(
    private val repository: ApplicationRepository
) {
    suspend operator fun invoke(appId: String, applicantName: String = "John Doe", amount: Double = 1500.0) {
        val initialApp = FinancialApplication(
            id = appId,
            applicantName = applicantName,
            amount = amount,
            state = ApplicationState.Draft,
            paymentId = null,
            idempotencyKey = null,
            failureReason = null,
            retryCount = 0,
            updatedAt = System.currentTimeMillis()
        )
        repository.clearAuditLogs(appId)
        repository.saveApplication(initialApp)
        repository.insertAuditLog(
            AuditLog(
                applicationId = appId,
                fromState = "N/A",
                event = "Reset Application",
                toState = ApplicationState.Draft.displayName(),
                isValid = true,
                details = "Application reset to initial Draft state"
            )
        )
    }
}
