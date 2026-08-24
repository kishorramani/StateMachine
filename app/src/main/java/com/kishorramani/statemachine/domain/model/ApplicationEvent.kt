package com.kishorramani.statemachine.domain.model

/**
 * Defines all business events that can trigger state transitions.
 */
sealed interface ApplicationEvent {

    data object Submit : ApplicationEvent

    data object StartVerification : ApplicationEvent

    data object DocumentsVerified : ApplicationEvent

    data object DocumentsRejected : ApplicationEvent

    data object StartCreditCheck : ApplicationEvent

    data object CreditApproved : ApplicationEvent

    data object CreditRejected : ApplicationEvent

    data object StartPayment : ApplicationEvent

    data object PaymentProcessing : ApplicationEvent

    data object PaymentSuccess : ApplicationEvent

    data object PaymentFailed : ApplicationEvent

    data object RetryPayment : ApplicationEvent

    data object Complete : ApplicationEvent

    fun displayName(): String = when (this) {
        Submit -> "Submit Application"
        StartVerification -> "Start Verification"
        DocumentsVerified -> "Documents Verified"
        DocumentsRejected -> "Documents Rejected"
        StartCreditCheck -> "Start Credit Check"
        CreditApproved -> "Credit Approved"
        CreditRejected -> "Credit Rejected"
        StartPayment -> "Start Payment"
        PaymentProcessing -> "Processing Payment"
        PaymentSuccess -> "Payment Succeeded"
        PaymentFailed -> "Payment Failed"
        RetryPayment -> "Retry Payment"
        Complete -> "Complete Application"
    }
}
