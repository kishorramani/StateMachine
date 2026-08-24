package com.kishorramani.statemachine.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.domain.model.AuditLog
import com.kishorramani.statemachine.domain.model.FinancialApplication
import com.kishorramani.statemachine.domain.model.TransitionResult
import com.kishorramani.statemachine.domain.repository.ApplicationRepository
import com.kishorramani.statemachine.domain.repository.ChaosMode
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine
import com.kishorramani.statemachine.domain.usecase.ProcessPaymentUseCase
import com.kishorramani.statemachine.domain.usecase.RefreshApplicationStatusUseCase
import com.kishorramani.statemachine.domain.usecase.ResetApplicationUseCase
import com.kishorramani.statemachine.domain.usecase.RetryPaymentUseCase
import com.kishorramani.statemachine.domain.usecase.StartCreditCheckUseCase
import com.kishorramani.statemachine.domain.usecase.SubmitApplicationUseCase
import com.kishorramani.statemachine.domain.usecase.VerifyDocumentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ApplicationUiState(
    val application: FinancialApplication? = null,
    val auditLogs: List<AuditLog> = emptyList(),
    val isProcessing: Boolean = false,
    val chaosMode: ChaosMode = ChaosMode.Normal,
    val userNotification: String? = null,
    val isErrorNotification: Boolean = false
)

class ApplicationViewModel(
    val appId: String = "APP-1001",
    private val repository: ApplicationRepository,
    private val stateMachine: ApplicationStateMachine,
    private val submitUseCase: SubmitApplicationUseCase,
    private val verifyUseCase: VerifyDocumentUseCase,
    private val creditCheckUseCase: StartCreditCheckUseCase,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val retryPaymentUseCase: RetryPaymentUseCase,
    private val refreshStatusUseCase: RefreshApplicationStatusUseCase,
    private val resetUseCase: ResetApplicationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    // Mutex protects state transitions from concurrent thread race conditions
    private val eventMutex = Mutex()

    init {
        // Initialize application if missing
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getApplication(appId) == null) {
                resetUseCase(appId)
            }
            // Observe application & audit logs in real-time
            launch {
                repository.getApplicationFlow(appId).collectLatest { app ->
                    _uiState.value = _uiState.value.copy(application = app)
                }
            }
            launch {
                repository.getAuditLogs(appId).collectLatest { logs ->
                    _uiState.value = _uiState.value.copy(auditLogs = logs)
                }
            }
        }
    }

    fun submitApplication() {
        executeGuarded {
            submitUseCase(appId)
        }
    }

    fun startDocumentVerification() {
        executeGuarded {
            verifyUseCase.startVerification(appId)
        }
    }

    fun completeDocumentVerification(approved: Boolean) {
        executeGuarded {
            verifyUseCase.completeVerification(appId, approved)
        }
    }

    fun performCreditCheck(approved: Boolean) {
        executeGuarded {
            creditCheckUseCase.executeCreditCheck(appId, approved)
        }
    }

    fun startPayment() {
        executeGuarded {
            processPaymentUseCase(appId)
        }
    }

    fun retryPayment() {
        executeGuarded {
            retryPaymentUseCase(appId)
        }
    }

    fun refreshRemoteStatus() {
        executeGuarded {
            refreshStatusUseCase(appId)
        }
    }

    fun resetApplication() {
        viewModelScope.launch(Dispatchers.IO) {
            eventMutex.withLock {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                resetUseCase(appId)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    userNotification = "Application reset to Draft",
                    isErrorNotification = false
                )
            }
        }
    }

    /**
     * Attempts an illegal transition explicitly to demonstrate invalid state handling.
     */
    fun attemptInvalidTransition(invalidEvent: ApplicationEvent) {
        executeGuarded {
            val app = repository.getApplication(appId) ?: return@executeGuarded TransitionResult.Invalid(
                ApplicationState.Draft, invalidEvent, "App not found"
            )
            val result = stateMachine.evaluateTransition(app.state, invalidEvent)
            if (result is TransitionResult.Invalid) {
                repository.insertAuditLog(
                    AuditLog(
                        applicationId = appId,
                        fromState = app.state.displayName(),
                        event = invalidEvent.displayName(),
                        toState = app.state.displayName(),
                        isValid = false,
                        details = "Invalid Transition Rejected: ${result.reason}"
                    )
                )
            }
            result
        }
    }

    fun setChaosMode(mode: ChaosMode) {
        repository.setChaosMode(mode)
        _uiState.value = _uiState.value.copy(chaosMode = mode)
    }

    /**
     * Simulates rapid concurrent events to demonstrate thread safety with Mutex lock.
     */
    fun triggerConcurrentEventsSimulation() {
        viewModelScope.launch(Dispatchers.IO) {
            launch { submitApplication() }
            launch { startPayment() }
            launch { performCreditCheck(true) }
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    private fun executeGuarded(block: suspend () -> TransitionResult) {
        viewModelScope.launch(Dispatchers.IO) {
            eventMutex.withLock {
                _uiState.value = _uiState.value.copy(isProcessing = true)
                val result = block()
                val (msg, isErr) = when (result) {
                    is TransitionResult.Success -> "State transitioned to: ${result.state.displayName()}" to false
                    is TransitionResult.Invalid -> "Invalid Operation: ${result.reason}" to true
                }
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    userNotification = msg,
                    isErrorNotification = isErr
                )
            }
        }
    }

    class Factory(
        private val appId: String,
        private val repository: ApplicationRepository,
        private val stateMachine: ApplicationStateMachine,
        private val submitUseCase: SubmitApplicationUseCase,
        private val verifyUseCase: VerifyDocumentUseCase,
        private val creditCheckUseCase: StartCreditCheckUseCase,
        private val processPaymentUseCase: ProcessPaymentUseCase,
        private val retryPaymentUseCase: RetryPaymentUseCase,
        private val refreshStatusUseCase: RefreshApplicationStatusUseCase,
        private val resetUseCase: ResetApplicationUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApplicationViewModel(
                appId, repository, stateMachine, submitUseCase, verifyUseCase,
                creditCheckUseCase, processPaymentUseCase, retryPaymentUseCase,
                refreshStatusUseCase, resetUseCase
            ) as T
        }
    }
}
