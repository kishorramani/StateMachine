package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine

class VerifyDocumentUseCase(
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine
) {
    suspend fun startVerification(appId: String): TransitionResult {
        val app = repository.getApplication(appId) ?: return invalid(appId, "Application not found", ApplicationEvent.StartVerification)
        val result = stateMachine.evaluateTransition(app.state, ApplicationEvent.StartVerification)
        if (result is TransitionResult.Success) {
            val updated = app.copy(state = result.state, updatedAt = System.currentTimeMillis())
            repository.saveApplication(updated)
            log(appId, app.state, ApplicationEvent.StartVerification, updated.state, true, "Verification process started")
        } else if (result is TransitionResult.Invalid) {
            log(appId, app.state, ApplicationEvent.StartVerification, app.state, false, result.reason)
        }
        return result
    }

    suspend fun completeVerification(appId: String, approved: Boolean): TransitionResult {
        val app = repository.getApplication(appId) ?: return invalid(appId, "Application not found", if (approved) ApplicationEvent.DocumentsVerified else ApplicationEvent.DocumentsRejected)
        val event = if (approved) ApplicationEvent.DocumentsVerified else ApplicationEvent.DocumentsRejected
        val result = stateMachine.evaluateTransition(app.state, event)
        if (result is TransitionResult.Success) {
            val updated = app.copy(
                state = result.state,
                failureReason = if (!approved) "Document verification failed" else null,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveApplication(updated)
            log(appId, app.state, event, updated.state, true, if (approved) "Documents verified" else "Documents rejected")
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

    private fun invalid(appId: String, reason: String, event: ApplicationEvent): TransitionResult.Invalid {
        return TransitionResult.Invalid(ApplicationState.Draft, event, reason)
    }
}
