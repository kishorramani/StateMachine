package com.kishorramani.statemachine

import com.kishorramani.statemachine.data.remote.FinancialApiImpl
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.FinancialApplication
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.repository.ChaosMode
import com.kishorramani.statemachine.domain.repository.PaymentRemoteResponse
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine
import com.kishorramani.statemachine.domain.usecase.ProcessPaymentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class ProcessPaymentUseCaseTest {

    private lateinit var repository: FakeApplicationRepository
    private lateinit var stateMachine: ApplicationStateMachine
    private lateinit var useCase: ProcessPaymentUseCase

    @Before
    fun setUp() {
        repository = FakeApplicationRepository()
        stateMachine = ApplicationStateMachine()
        useCase = ProcessPaymentUseCase(repository, stateMachine, maxRetries = 2, initialBackoffMs = 10L)
    }

    @Test
    fun `payment succeeds and application completes`() = runTest {
        val initialApp = FinancialApplication(
            id = "APP-TEST",
            applicantName = "Jane Doe",
            amount = 1000.0,
            state = ApplicationState.Approved
        )
        repository.saveApplication(initialApp)

        val result = useCase(appId = "APP-TEST")
        assertTrue(result is TransitionResult.Success)

        val updatedApp = repository.getApplication("APP-TEST")
        assertEquals(ApplicationState.Completed, updatedApp?.state)
    }

    @Test
    fun `idempotent payment retry reuses idempotency key`() = runTest {
        val key = "KEY-IDEMPOTENT-123"
        val initialApp = FinancialApplication(
            id = "APP-TEST",
            applicantName = "Jane Doe",
            amount = 1000.0,
            state = ApplicationState.Approved,
            paymentId = "PAY-123",
            idempotencyKey = key
        )
        repository.saveApplication(initialApp)

        // First call
        useCase(appId = "APP-TEST", existingPaymentId = "PAY-123", existingIdempotencyKey = key)
        val firstApp = repository.getApplication("APP-TEST")

        assertEquals(key, firstApp?.idempotencyKey)
        assertEquals(ApplicationState.Completed, firstApp?.state)
    }

    @Test
    fun `non-retryable 400 error fails immediately without retries`() = runTest {
        repository.setChaosMode(ChaosMode.NonRetryableError400)
        val initialApp = FinancialApplication(
            id = "APP-TEST",
            applicantName = "Jane Doe",
            amount = 1000.0,
            state = ApplicationState.Approved
        )
        repository.saveApplication(initialApp)

        val result = useCase(appId = "APP-TEST")
        assertTrue(result is TransitionResult.Invalid)

        val updatedApp = repository.getApplication("APP-TEST")
        assertEquals(ApplicationState.PaymentFailed, updatedApp?.state)
        assertEquals(1, updatedApp?.retryCount) // Failed on 1st attempt, non-retryable
    }

    // Fake in-memory repository for unit testing Use Cases without Room dependency
    private class FakeApplicationRepository : ApplicationRepository {
        private val apps = ConcurrentHashMap<String, FinancialApplication>()
        private val logs = mutableListOf<AuditLog>()
        private val api = FinancialApiImpl()

        override fun getApplicationFlow(id: String): Flow<FinancialApplication?> = flowOf(apps[id])

        override suspend fun getApplication(id: String): FinancialApplication? = apps[id]

        override suspend fun saveApplication(application: FinancialApplication) {
            apps[application.id] = application
        }

        override fun getAuditLogs(applicationId: String): Flow<List<AuditLog>> = flowOf(logs.filter { it.applicationId == applicationId })

        override suspend fun insertAuditLog(log: AuditLog) {
            logs.add(log)
        }

        override suspend fun clearAuditLogs(applicationId: String) {
            logs.removeAll { it.applicationId == applicationId }
        }

        override suspend fun processPaymentRemote(
            applicationId: String,
            paymentId: String,
            idempotencyKey: String,
            amount: Double
        ): Result<PaymentRemoteResponse> {
            return try {
                val res = api.processPayment(applicationId, paymentId, idempotencyKey, amount)
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun fetchRemotePaymentStatus(paymentId: String): Result<PaymentRemoteResponse> {
            return try {
                val res = api.getPaymentStatus(paymentId)
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override fun setChaosMode(mode: ChaosMode) {
            api.setChaosMode(mode)
        }

        override fun getChaosMode(): ChaosMode = api.getChaosMode()
    }
}
