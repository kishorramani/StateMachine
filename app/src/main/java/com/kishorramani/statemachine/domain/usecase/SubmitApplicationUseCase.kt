package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.FinancialApplication
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine

class SubmitApplicationUseCase(
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine
) {
    suspend operator fun invoke(appId: String): TransitionResult {
        val currentApp = repository.getApplication(appId) ?: return TransitionResult.Invalid(
            currentState = com.kishorramani.statemachine.domain.model.ApplicationState.Draft,
            event = ApplicationEvent.Submit,
            reason = "Application not found"
        )

        val result = stateMachine.evaluateTransition(currentApp.state, ApplicationEvent.Submit)
        val isValid = result is TransitionResult.Success
        val newApp = if (result is TransitionResult.Success) {
            currentApp.copy(state = result.state, updatedAt = System.currentTimeMillis())
        } else currentApp

        if (isValid) {
            repository.saveApplication(newApp)
        }

        repository.insertAuditLog(
            AuditLog(
                applicationId = appId,
                fromState = currentApp.state.displayName(),
                event = ApplicationEvent.Submit.displayName(),
                toState = newApp.state.displayName(),
                isValid = isValid,
                details = if (result is TransitionResult.Invalid) result.reason else "Application submitted successfully"
            )
        )

        return result
    }
}
