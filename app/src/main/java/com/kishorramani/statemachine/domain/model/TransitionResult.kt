package com.kishorramani.statemachine.domain.model

/**
 * Encapsulates the outcome of attempting a state transition.
 * Makes invalid state transitions explicit, observable, and testable.
 */
sealed interface TransitionResult {

    data class Success(
        val state: ApplicationState
    ) : TransitionResult

    data class Invalid(
        val currentState: ApplicationState,
        val event: ApplicationEvent,
        val reason: String
    ) : TransitionResult
}
