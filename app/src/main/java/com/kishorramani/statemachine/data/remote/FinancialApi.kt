package com.kishorramani.statemachine.data.remote

import com.kishorramani.statemachine.domain.repository.ChaosMode
import com.kishorramani.statemachine.domain.repository.PaymentRemoteResponse

interface FinancialApi {
    suspend fun processPayment(
        applicationId: String,
        paymentId: String,
        idempotencyKey: String,
        amount: Double
    ): PaymentRemoteResponse

    suspend fun getPaymentStatus(paymentId: String): PaymentRemoteResponse

    fun setChaosMode(mode: ChaosMode)
    fun getChaosMode(): ChaosMode
}
