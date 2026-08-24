# Financial Service State Machine Assessment

## 1. Overview

This project demonstrates a production-grade Financial Service application built with **Jetpack Compose**, **Clean Architecture**, and a **Finite State Machine (FSM)** to explicitly model and manage the lifecycle of customer financial applications and payment transactions.

### Key Goals & System Architecture
* **Explicit Business State Modeling**: All 12 states are represented via sealed interfaces.
* **Guaranteed Valid State Transitions**: Transitions are guarded by a pure Kotlin state machine (`ApplicationStateMachine.kt`).
* **Observable Invalid Transitions**: Illegal state transitions return explicit `TransitionResult.Invalid` with descriptive failure explanations rather than failing silently.
* **Idempotency & Double-Charge Prevention**: Unique UUID idempotency keys are generated per transaction flow and persisted locally to prevent duplicate charges upon retries.
* **Network Fault Tolerance & Exponential Backoff**: Automatic retry strategy for retryable network errors (Timeouts, HTTP 503) with exponential backoff while immediately rejecting non-retryable 4xx errors.
* **State Persistence & Crash Recovery**: State is persisted locally via Room database (`AppDatabase`) and automatically reconciled with the backend upon app restart.
* **Thread Safety & Mutex Serialization**: Concurrency hazards are prevented by serializing state transitions with Kotlin `Mutex`.

---

## 2. Business Lifecycle Diagram

```text
DRAFT
  │
  │ Submit
  ▼
SUBMITTED
  │
  │ Start Verification
  ▼
DOCUMENT_VERIFICATION
  │
  ├──────────────────┐
  │                  │
  │ Verified         │ Failed
  ▼                  ▼
CREDIT_CHECK    VERIFICATION_FAILED (Terminal)
  │                  │
  ├────────┬─────────┘
           │
     ┌─────┴──────┐
     │            │
 Approved       Rejected (Terminal)
     │
     │ Start Payment
     ▼
PAYMENT_PENDING
     │
     │ Process
     ▼
PAYMENT_PROCESSING
     │
     ├─────────────┐
     │             │
   Success       Failed
     │             │
     ▼             ▼
PAYMENT_SUCCESS PAYMENT_FAILED
     │             │
     │             │ Retry (reuses same idempotency key)
     │             │
     ▼             │
 COMPLETED <───────┘ (Terminal)
```

---

## 3. Layered Clean Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    Presentation UI                          │
│ Activity (MainActivity) / Compose (ApplicationScreen)       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       ViewModel                             │
│ ApplicationViewModel (StateFlow + Mutex Event Protection)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        UseCases                             │
│ SubmitApp | VerifyDocs | ProcessPayment | RefreshStatus     │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     State Machine                           │
│ ApplicationStateMachine (Pure Kotlin Single Source of Truth) │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
┌──────────────────────────────┐ ┌───────────────────────────┐
│        Room Database         │ │       Financial API       │
│ AppDatabase / ApplicationDao │ │ FinancialApiImpl (Remote) │
└──────────────────────────────┘ └─────────────────────────┘
```

---

## 4. Package Structure

```text
app/src/main/java/com/kishorramani/statemachine/
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          # Room Database definition
│   │   ├── ApplicationDao.kt       # Room Data Access Object
│   │   ├── ApplicationEntity.kt    # Persisted Application Entity
│   │   └── AuditLogEntity.kt       # Persisted Audit Log Entity
│   ├── remote/
│   │   ├── FinancialApi.kt         # Remote Payment API interface
│   │   └── FinancialApiImpl.kt     # Mock Remote API with Idempotency cache & Chaos simulation
│   └── repository/
│       └── ApplicationRepositoryImpl.kt # Repository implementation coordinating Room + API
│
├── domain/
│   ├── model/
│   │   ├── ApplicationState.kt     # Sealed interface for 12 business states
│   │   ├── ApplicationEvent.kt     # Sealed interface for 13 state events
│   │   ├── TransitionResult.kt     # Success vs Invalid result wrapper
│   │   ├── FinancialApplication.kt # Domain Application Model
│   │   └── AuditLog.kt             # Domain Audit Log Entry
│   ├── state/
│   │   └── ApplicationStateMachine.kt # Pure Kotlin FSM logic
│   ├── repository/
│   │   └── ApplicationRepository.kt   # Domain Repository interface & ChaosMode definition
│   └── usecase/
│       ├── SubmitApplicationUseCase.kt
│       ├── VerifyDocumentUseCase.kt
│       ├── StartCreditCheckUseCase.kt
│       ├── ProcessPaymentUseCase.kt   # Payment logic, Idempotency, & Exponential Backoff
│       ├── RetryPaymentUseCase.kt     # Payment retry with key reuse
│       ├── RefreshApplicationStatusUseCase.kt # Status reconciliation
│       └── ResetApplicationUseCase.kt
│
└── presentation/
    ├── ApplicationViewModel.kt    # ViewModel exposing StateFlow & Mutex synchronization
    └── ui/
        ├── ApplicationScreen.kt   # Main Compose screen & action controls
        └── components/
            ├── StateVisualizerCard.kt  # Visual lifecycle node map
            ├── PaymentDashboardCard.kt # Idempotency key & transaction monitor
            ├── ChaosSimulationCard.kt  # Fault injection & chaos testing controls
            └── AuditLogCard.kt         # Live stream of transition audit logs
```

---

## 5. Domain Modeling

### `ApplicationState.kt`
Defines 12 exhaustive business states using Kotlin sealed interfaces:
```kotlin
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
}
```

### `TransitionResult.kt`
Avoids silent failures by making invalid transition attempts observable:
```kotlin
sealed interface TransitionResult {
    data class Success(val state: ApplicationState) : TransitionResult
    data class Invalid(val currentState: ApplicationState, val event: ApplicationEvent, val reason: String) : TransitionResult
}
```

---

## 6. Payment Processing, Idempotency, & Retries

### Idempotency Key Strategy
When a payment is initiated, a unique UUID idempotency key is generated and persisted in Room:
```http
POST /payments
Idempotency-Key: 9b1deb4d-3b7d-41b3-93d3-1959828d5423
```
If network connectivity fails or a timeout occurs, subsequent retries (`RetryPaymentUseCase`) **reuse the exact same idempotency key**. The backend API (`FinancialApiImpl`) detects the duplicate key and returns the stored transaction result without processing a duplicate financial charge.

### Retry Strategy & Exponential Backoff
* **Max Attempts**: 3 attempts.
* **Backoff Schedule**: $500\text{ms} \times 2^{(\text{attempt}-1)}$ ($500\text{ms}$, $1000\text{ms}$, $2000\text{ms}$).
* **Retryable Errors**: Network Timeout, HTTP 503 Service Unavailable.
* **Non-Retryable Errors**: HTTP 400 Bad Request, HTTP 401 Unauthorized, HTTP 403 Forbidden.

---

## 7. Fault Tolerance & Chaos Testing Controls

The application includes an interactive **Chaos Engineering Panel** in the UI to allow reviewers to inject failures live:
1. **Normal API**: Happy path execution.
2. **Simulate Timeout (Retryable)**: Throws network timeout; demonstrates exponential backoff retries.
3. **Simulate 503 Server Error**: Throws server error; demonstrates retry policies.
4. **Simulate 400 Bad Request**: Throws non-retryable error; immediately transitions to `PaymentFailed` without retrying.
5. **Server Ahead Mismatch**: Simulates backend completing payment while client was offline; demonstrates status reconciliation (`RefreshApplicationStatusUseCase`).
6. **Simulate Race Condition**: Fires concurrent async events to demonstrate thread safety with Kotlin `Mutex`.

---

## 8. Unit Testing

Run unit tests using Gradle:
```bash
./gradlew test
```

### Unit Test Suites Included:
* **`ApplicationStateMachineTest.kt`**: Tests all 12 valid state transitions, invalid transition error handling, and terminal state immutability.
* **`ProcessPaymentUseCaseTest.kt`**: Tests idempotency key reuse, exponential backoff retries, non-retryable error handling, and server status reconciliation.

---

## 9. Interview Discussion Points

### Q1: Why use a State Machine instead of simple boolean flags?
> **Answer**: Boolean flags (like `isLoading`, `isPaymentSuccess`, `isPaymentFailed`) can create impossible states (e.g. `isPaymentSuccess = true` AND `isPaymentFailed = true`). A state machine guarantees that the application can only exist in exactly one valid state at any time and enforces explicit business rules for state transitions.

### Q2: How do you prevent duplicate payment charges on network timeouts?
> **Answer**: By attaching a client-generated UUID `Idempotency-Key` to the payment request and persisting it locally. If a response is lost due to a timeout, retries send the exact same `Idempotency-Key`. The backend identifies duplicate requests and safely returns the existing transaction result without re-charging the user.

### Q3: How does the application handle crashes during payment processing?
> **Answer**: Application state is persisted in Room database. Upon restart, if the state is `PaymentProcessing` or `PaymentFailed`, the app reconciles its state with the server by fetching `GET /payments/{paymentId}` instead of blindly re-submitting a new payment.

### Q4: How are race conditions from concurrent events prevented?
> **Answer**: Event processing in the ViewModel is serialized using Kotlin `Mutex.withLock {}`. This ensures that state transitions are evaluated atomically against the latest state.
