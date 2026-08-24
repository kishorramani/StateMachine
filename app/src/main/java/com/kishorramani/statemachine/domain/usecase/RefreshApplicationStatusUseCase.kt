package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine

class RefreshApplicationStatusUseCase(
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine
) {
    suspend operator fun invoke(appId: String): TransitionResult {
        val app = repository.getApplication(appId) ?: return TransitionResult.Invalid(
            currentState = ApplicationState.Draft,
            event = ApplicationEvent.Complete,
            reason = "Application not found"
        )

        val paymentId = app.paymentId ?: return TransitionResult.Invalid(
            currentState = app.state,
            event = ApplicationEvent.Complete,
            reason = "No payment ID recorded to reconcile"
        )

        val result = repository.fetchRemotePaymentStatus(paymentId)
        if (result.isFailure) {
            val err = result.exceptionOrNull()?.message ?: "Backend fetch error"
            repository.insertAuditLog(
                AuditLog(
                    applicationId = appId,
                    fromState = app.state.displayName(),
                    event = "Reconcile Status",
                    toState = app.state.displayName(),
                    isValid = false,
                    details = "Reconciliation failed: $err"
                )
            )
            return TransitionResult.Invalid(app.state, ApplicationEvent.Complete, err)
        }

        val remoteData = result.getOrThrow()
        if (remoteData.status == "SUCCESS") {
            // Server reports PAYMENT_SUCCESS
            val eval = stateMachine.evaluateTransition(app.state, ApplicationEvent.PaymentSuccess)
            val nextState = if (eval is TransitionResult.Success) eval.state else ApplicationState.Completed
            val updated = app.copy(
                state = nextState,
                failureReason = null,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveApplication(updated)

            repository.insertAuditLog(
                AuditLog(
                    applicationId = appId,
                    fromState = app.state.displayName(),
                    event = "Reconcile Status (Success)",
                    toState = updated.state.displayName(),
                    isValid = true,
                    details = "Server state reconciled to SUCCESS. Local state updated."
                )
            )

            // Auto complete if in PaymentSuccess
            val completeEval = stateMachine.evaluateTransition(updated.state, ApplicationEvent.Complete)
            if (completeEval is TransitionResult.Success) {
                val finalApp = updated.copy(state = completeEval.state, updatedAt = System.currentTimeMillis())
                repository.saveApplication(finalApp)
                repository.insertAuditLog(
                    AuditLog(
                        applicationId = appId,
                        fromState = updated.state.displayName(),
                        event = ApplicationEvent.Complete.displayName(),
                        toState = finalApp.state.displayName(),
                        isValid = true,
                        details = "Application completed after server reconciliation"
                    )
                )
                return TransitionResult.Success(finalApp.state)
            }
            return TransitionResult.Success(updated.state)
        } else {
            repository.insertAuditLog(
                AuditLog(
                    applicationId = appId,
                    fromState = app.state.displayName(),
                    event = "Reconcile Status",
                    toState = app.state.displayName(),
                    isValid = true,
                    details = "Server status matches local: ${remoteData.status}"
                )
            )
            return TransitionResult.Success(app.state)
        }
    }
}
