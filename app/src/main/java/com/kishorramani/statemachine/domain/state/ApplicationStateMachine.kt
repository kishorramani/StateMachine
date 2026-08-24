package com.kishorramani.statemachine.domain.state

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.TransitionResult

/**
 * Finite State Machine for Financial Application Lifecycle.
 *
 * Single Source of Truth for valid state transitions.
 * Independent of Android SDK framework to allow 100% unit testing capability.
 */
class ApplicationStateMachine {

    /**
     * Evaluates whether an event is valid from the current state.
     * Returns explicit TransitionResult (Success or Invalid with reason).
     */
    fun evaluateTransition(
        currentState: ApplicationState,
        event: ApplicationEvent
    ): TransitionResult {
        return when (currentState) {
            ApplicationState.Draft -> when (event) {
                ApplicationEvent.Submit -> TransitionResult.Success(ApplicationState.Submitted)
                else -> invalid(currentState, event, "Draft application can only be submitted.")
            }

            ApplicationState.Submitted -> when (event) {
                ApplicationEvent.StartVerification -> TransitionResult.Success(ApplicationState.DocumentVerification)
                else -> invalid(currentState, event, "Submitted application must start document verification.")
            }

            ApplicationState.DocumentVerification -> when (event) {
                ApplicationEvent.DocumentsVerified -> TransitionResult.Success(ApplicationState.CreditChecking)
                ApplicationEvent.DocumentsRejected -> TransitionResult.Success(ApplicationState.VerificationFailed)
                else -> invalid(currentState, event, "Verification requires DocumentsVerified or DocumentsRejected.")
            }

            ApplicationState.CreditChecking -> when (event) {
                ApplicationEvent.CreditApproved -> TransitionResult.Success(ApplicationState.Approved)
                ApplicationEvent.CreditRejected -> TransitionResult.Success(ApplicationState.Rejected)
                else -> invalid(currentState, event, "Credit check requires CreditApproved or CreditRejected.")
            }

            ApplicationState.Approved -> when (event) {
                ApplicationEvent.StartPayment -> TransitionResult.Success(ApplicationState.PaymentPending)
                else -> invalid(currentState, event, "Approved application can only start payment.")
            }

            ApplicationState.PaymentPending -> when (event) {
                ApplicationEvent.PaymentProcessing -> TransitionResult.Success(ApplicationState.PaymentProcessing)
                else -> invalid(currentState, event, "PaymentPending must transition to PaymentProcessing.")
            }

            ApplicationState.PaymentProcessing -> when (event) {
                ApplicationEvent.PaymentSuccess -> TransitionResult.Success(ApplicationState.PaymentSuccess)
                ApplicationEvent.PaymentFailed -> TransitionResult.Success(ApplicationState.PaymentFailed)
                else -> invalid(currentState, event, "PaymentProcessing must receive PaymentSuccess or PaymentFailed.")
            }

            ApplicationState.PaymentFailed -> when (event) {
                ApplicationEvent.RetryPayment -> TransitionResult.Success(ApplicationState.PaymentProcessing)
                ApplicationEvent.PaymentSuccess -> TransitionResult.Success(ApplicationState.Completed)
                else -> invalid(currentState, event, "PaymentFailed allows RetryPayment or status reconciliation to PaymentSuccess.")
            }

            ApplicationState.PaymentSuccess -> when (event) {
                ApplicationEvent.Complete -> TransitionResult.Success(ApplicationState.Completed)
                else -> invalid(currentState, event, "PaymentSuccess can only transition to Completed.")
            }

            ApplicationState.VerificationFailed -> invalid(
                currentState, event, "VerificationFailed is a terminal state. Application cannot be modified."
            )

            ApplicationState.Rejected -> invalid(
                currentState, event, "Rejected is a terminal state. Application cannot be modified."
            )

            ApplicationState.Completed -> invalid(
                currentState, event, "Completed is a terminal state. Application lifecycle has finished."
            )
        }
    }

    /**
     * Legacy helper transition returning next state or current state if invalid.
     */
    fun transition(
        state: ApplicationState,
        event: ApplicationEvent
    ): ApplicationState {
        return when (val result = evaluateTransition(state, event)) {
            is TransitionResult.Success -> result.state
            is TransitionResult.Invalid -> state
        }
    }

    private fun invalid(
        state: ApplicationState,
        event: ApplicationEvent,
        reason: String
    ): TransitionResult.Invalid {
        return TransitionResult.Invalid(
            currentState = state,
            event = event,
            reason = reason
        )
    }
}
