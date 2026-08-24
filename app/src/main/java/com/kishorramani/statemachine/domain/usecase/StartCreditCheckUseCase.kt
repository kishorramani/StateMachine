package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine

class StartCreditCheckUseCase(
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine
) {
    suspend fun executeCreditCheck(appId: String, approved: Boolean): TransitionResult {
        val app = repository.getApplication(appId) ?: return TransitionResult.Invalid(
            currentState = ApplicationState.Draft,
            event = if (approved) ApplicationEvent.CreditApproved else ApplicationEvent.CreditRejected,
            reason = "Application not found"
        )

        val event = if (approved) ApplicationEvent.CreditApproved else ApplicationEvent.CreditRejected
        val result = stateMachine.evaluateTransition(app.state, event)

        if (result is TransitionResult.Success) {
            val updated = app.copy(
                state = result.state,
                failureReason = if (!approved) "Credit score requirements not met" else null,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveApplication(updated)
            log(appId, app.state, event, updated.state, true, if (approved) "Credit check approved" else "Credit check rejected")
        } else if (result is TransitionResult.Invalid) {
            log(appId, app.state, event, app.state, false, result.reason)
        }

        return result
    }

    private suspend fun log(appId: String, fromState: ApplicationState, event: ApplicationEvent, toState: ApplicationState, isValid: Boolean, details: String) {
        repository.insertAuditLog(
            AuditLog(
                applicationId = appId,
                fromState = fromState.displayName(),
                event = event.displayName(),
                toState = toState.displayName(),
                isValid = isValid,
                details = details
            )
        )
    }
}
