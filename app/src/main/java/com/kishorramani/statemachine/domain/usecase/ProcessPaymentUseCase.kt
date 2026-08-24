package com.kishorramani.statemachine.domain.usecase

import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine
import kotlinx.coroutines.delay
import java.util.UUID

class ProcessPaymentUseCase(
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine,
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 500L
) {
    suspend operator fun invoke(
        appId: String,
        existingPaymentId: String? = null,
        existingIdempotencyKey: String? = null
    ): TransitionResult {
        val app = repository.getApplication(appId) ?: return invalid(
            ApplicationState.Draft,
            ApplicationEvent.StartPayment,
            "Application not found"
        )

        // 1. Validate initial start payment transition
        val startResult = stateMachine.evaluateTransition(app.state, ApplicationEvent.StartPayment)
        if (startResult is TransitionResult.Invalid) {
            log(appId, app.state, ApplicationEvent.StartPayment, app.state, false, startResult.reason)
            return startResult
        }

        // 2. Prepare Payment ID and Idempotency Key
        val paymentId = existingPaymentId ?: app.paymentId ?: "PAY-${System.currentTimeMillis()}"
        val idempotencyKey = existingIdempotencyKey ?: app.idempotencyKey ?: UUID.randomUUID().toString()

        var currentApp = app.copy(
            state = ApplicationState.PaymentPending,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            failureReason = null,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveApplication(currentApp)
        log(appId, app.state, ApplicationEvent.StartPayment, ApplicationState.PaymentPending, true, "Payment initiated. Key: $idempotencyKey")

        // 3. Transition to PaymentProcessing
        val processingResult = stateMachine.evaluateTransition(currentApp.state, ApplicationEvent.PaymentProcessing)
        if (processingResult is TransitionResult.Success) {
            currentApp = currentApp.copy(state = ApplicationState.PaymentProcessing, updatedAt = System.currentTimeMillis())
            repository.saveApplication(currentApp)
            log(appId, ApplicationState.PaymentPending, ApplicationEvent.PaymentProcessing, ApplicationState.PaymentProcessing, true, "Processing payment remotely...")
        }

        // 4. Remote call loop with exponential backoff & idempotency
        var attempt = 0
        var isSuccess = false
        var lastErrorMessage = "Unknown payment error"
        var isRetryable = true

        while (attempt < maxRetries && !isSuccess) {
            attempt++
            log(appId, ApplicationState.PaymentProcessing, ApplicationEvent.PaymentProcessing, ApplicationState.PaymentProcessing, true, "Payment Attempt #$attempt with Idempotency-Key: $idempotencyKey")

            val response = repository.processPaymentRemote(
                applicationId = appId,
                paymentId = paymentId,
                idempotencyKey = idempotencyKey,
                amount = currentApp.amount
            )

            if (response.isSuccess) {
                val data = response.getOrThrow()
                if (data.status == "SUCCESS") {
                    isSuccess = true
                    lastErrorMessage = if (data.isDuplicate) "Payment completed (Idempotent response)" else "Payment processing successful"
                } else {
                    lastErrorMessage = data.errorMessage ?: "Payment rejected by server"
                    isRetryable = false
                    break
                }
            } else {
                val error = response.exceptionOrNull()
                lastErrorMessage = error?.message ?: "Network failure"
                // Check if non-retryable 4xx
                if (lastErrorMessage.contains("400") || lastErrorMessage.contains("401") || lastErrorMessage.contains("403")) {
                    isRetryable = false
                    break
                }
                if (attempt < maxRetries && isRetryable) {
                    val backoff = initialBackoffMs * (1 shl (attempt - 1))
                    log(appId, ApplicationState.PaymentProcessing, ApplicationEvent.PaymentProcessing, ApplicationState.PaymentProcessing, true, "Retryable error ($lastErrorMessage). Exponential backoff: ${backoff}ms")
                    delay(backoff)
                }
            }
        }

        // 5. Finalize state transition based on payment outcome
        return if (isSuccess) {
            val successResult = stateMachine.evaluateTransition(currentApp.state, ApplicationEvent.PaymentSuccess)
            val successState = if (successResult is TransitionResult.Success) successResult.state else ApplicationState.PaymentSuccess
            currentApp = currentApp.copy(state = successState, failureReason = null, retryCount = attempt, updatedAt = System.currentTimeMillis())
            repository.saveApplication(currentApp)
            log(appId, ApplicationState.PaymentProcessing, ApplicationEvent.PaymentSuccess, successState, true, lastErrorMessage)

            // Auto-complete application
            val completeResult = stateMachine.evaluateTransition(currentApp.state, ApplicationEvent.Complete)
            if (completeResult is TransitionResult.Success) {
                currentApp = currentApp.copy(state = completeResult.state, updatedAt = System.currentTimeMillis())
                repository.saveApplication(currentApp)
                log(appId, successState, ApplicationEvent.Complete, completeResult.state, true, "Application completed successfully")
            }
            TransitionResult.Success(currentApp.state)
        } else {
            val failResult = stateMachine.evaluateTransition(currentApp.state, ApplicationEvent.PaymentFailed)
            val failState = if (failResult is TransitionResult.Success) failResult.state else ApplicationState.PaymentFailed
            currentApp = currentApp.copy(
                state = failState,
                failureReason = "$lastErrorMessage (Attempts: $attempt, Retryable: $isRetryable)",
                retryCount = attempt,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveApplication(currentApp)
            log(appId, ApplicationState.PaymentProcessing, ApplicationEvent.PaymentFailed, failState, false, "Payment failed: $lastErrorMessage")
            TransitionResult.Invalid(currentApp.state, ApplicationEvent.PaymentFailed, lastErrorMessage)
        }
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

    private fun invalid(state: ApplicationState, event: ApplicationEvent, reason: String): TransitionResult.Invalid {
        return TransitionResult.Invalid(state, event, reason)
    }
}
