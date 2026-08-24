package com.kishorramani.statemachine.data.remote

import com.kishorramani.statemachine.domain.repository.ChaosMode
import com.kishorramani.statemachine.domain.repository.PaymentRemoteResponse
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Robust mock implementation of Financial Server API.
 * Simulates idempotency checks, network latency, and configurable failure modes.
 */
class FinancialApiImpl : FinancialApi {

    private val idempotencyStore = ConcurrentHashMap<String, PaymentRemoteResponse>()
    private val paymentStatusStore = ConcurrentHashMap<String, PaymentRemoteResponse>()
    @Volatile private var currentChaosMode: ChaosMode = ChaosMode.Normal

    override fun setChaosMode(mode: ChaosMode) {
        currentChaosMode = mode
    }

    override fun getChaosMode(): ChaosMode = currentChaosMode

    override suspend fun processPayment(
        applicationId: String,
        paymentId: String,
        idempotencyKey: String,
        amount: Double
    ): PaymentRemoteResponse {
        // Simulate network latency
        delay(400)

        // 1. Idempotency Check: Reuse cached result if idempotencyKey was already processed
        idempotencyStore[idempotencyKey]?.let { cachedResponse ->
            return cachedResponse.copy(isDuplicate = true)
        }

        // 2. Evaluate Chaos Simulation Modes
        when (currentChaosMode) {
            ChaosMode.Timeout -> {
                throw IOException("Network connection timeout (HTTP 504 Simulated)")
            }
            ChaosMode.ServerError500 -> {
                throw IOException("HTTP 503 Gateway Unavailable (Simulated Server Error)")
            }
            ChaosMode.NonRetryableError400 -> {
                throw IllegalArgumentException("HTTP 400 Bad Request: Invalid Payment Payload")
            }
            ChaosMode.ServerAheadMismatch -> {
                // Server processes payment to SUCCESS in background but client local state isn't updated yet
                val successResponse = PaymentRemoteResponse(
                    paymentId = paymentId,
                    idempotencyKey = idempotencyKey,
                    status = "SUCCESS",
                    isDuplicate = false,
                    errorCode = 200
                )
                idempotencyStore[idempotencyKey] = successResponse
                paymentStatusStore[paymentId] = successResponse
                return successResponse
            }
            ChaosMode.Normal -> {
                val successResponse = PaymentRemoteResponse(
                    paymentId = paymentId,
                    idempotencyKey = idempotencyKey,
                    status = "SUCCESS",
                    isDuplicate = false,
                    errorCode = 200
                )
                idempotencyStore[idempotencyKey] = successResponse
                paymentStatusStore[paymentId] = successResponse
                return successResponse
            }
        }
    }

    override suspend fun getPaymentStatus(paymentId: String): PaymentRemoteResponse {
        delay(200)
        return paymentStatusStore[paymentId] ?: PaymentRemoteResponse(
            paymentId = paymentId,
            idempotencyKey = "",
            status = "NOT_FOUND",
            errorMessage = "No record found on remote server",
            errorCode = 404
        )
    }
}
