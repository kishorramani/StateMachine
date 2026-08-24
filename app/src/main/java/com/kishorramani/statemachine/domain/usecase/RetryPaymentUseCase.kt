package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine

class RetryPaymentUseCase(
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine,
    private val processPaymentUseCase: ProcessPaymentUseCase
) {
    suspend operator fun invoke(appId: String): TransitionResult {
        val app = repository.getApplication(appId) ?: return TransitionResult.Invalid(
            currentState = ApplicationState.Draft,
            event = ApplicationEvent.RetryPayment,
            reason = "Application not found"
        )

        val retryTransition = stateMachine.evaluateTransition(app.state, ApplicationEvent.RetryPayment)
        if (retryTransition is TransitionResult.Invalid) {
            repository.insertAuditLog(
                AuditLog(
                    applicationId = appId,
                    fromState = app.state.displayName(),
                    event = ApplicationEvent.RetryPayment.displayName(),
                    toState = app.state.displayName(),
                    isValid = false,
                    details = retryTransition.reason
                )
            )
            return retryTransition
        }

        // Reuse existing payment ID & Idempotency Key!
        val paymentId = app.paymentId ?: "PAY-${System.currentTimeMillis()}"
        val idempotencyKey = app.idempotencyKey

        repository.insertAuditLog(
            AuditLog(
                applicationId = appId,
                fromState = app.state.displayName(),
                event = ApplicationEvent.RetryPayment.displayName(),
                toState = ApplicationState.PaymentProcessing.displayName(),
                isValid = true,
                details = "Retrying payment with existing Idempotency Key: $idempotencyKey"
            )
        )

        val updatedApp = app.copy(
            state = ApplicationState.PaymentProcessing,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveApplication(updatedApp)

        // Process payment with existing keys
        return processPaymentUseCase(
            appId = appId,
            existingPaymentId = paymentId,
            existingIdempotencyKey = idempotencyKey
        )
    }
}
