package com.kishorramani.statemachine.domain.model

/**
 * Represents all possible states in the financial application lifecycle.
 */
sealed interface ApplicationState {

    data object Draft : ApplicationState

    data object Submitted : ApplicationState

    data object DocumentVerification : ApplicationState

    data object VerificationFailed : ApplicationState

    data object CreditChecking : ApplicationState

    data object Approved : ApplicationState

    data object Rejected : ApplicationState

    data object PaymentPending : ApplicationState

    data object PaymentProcessing : ApplicationState

    data object PaymentSuccess : ApplicationState

    data object PaymentFailed : ApplicationState

    data object Completed : ApplicationState

    /**
     * Helper to get a human-readable display name for each state.
     */
    fun displayName(): String = when (this) {
        Draft -> "Draft"
        Submitted -> "Submitted"
        DocumentVerification -> "Document Verification"
        VerificationFailed -> "Verification Failed"
        CreditChecking -> "Credit Checking"
        Approved -> "Approved"
        Rejected -> "Rejected"
        PaymentPending -> "Payment Pending"
        PaymentProcessing -> "Payment Processing"
        PaymentSuccess -> "Payment Success"
        PaymentFailed -> "Payment Failed"
        Completed -> "Completed"
    }

    /**
     * Indicates whether this state is a terminal state (cannot transition further).
     */
    val isTerminal: Boolean
        get() = this is VerificationFailed || this is Rejected || this is Completed
}
