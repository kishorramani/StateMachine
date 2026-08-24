package com.kishorramani.statemachine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kishorramani.statemachine.data.local.AppDatabase
import com.kishorramani.statemachine.data.remote.FinancialApiImpl
import com.kishorramani.statemachine.data.repository.ApplicationRepositoryImpl
import com.kishorramani.statemachine.domain.state.ApplicationStateMachine
import com.kishorramani.statemachine.domain.usecase.ProcessPaymentUseCase
import com.kishorramani.statemachine.domain.usecase.RefreshApplicationStatusUseCase
import com.kishorramani.statemachine.domain.usecase.ResetApplicationUseCase
import com.kishorramani.statemachine.domain.usecase.RetryPaymentUseCase
import com.kishorramani.statemachine.domain.usecase.StartCreditCheckUseCase
import com.kishorramani.statemachine.domain.usecase.SubmitApplicationUseCase
import com.kishorramani.statemachine.domain.usecase.VerifyDocumentUseCase
import com.kishorramani.statemachine.presentation.ApplicationViewModel
import com.kishorramani.statemachine.presentation.ui.ApplicationScreen
import com.kishorramani.statemachine.ui.theme.StateMachineTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clean Architecture Manual Dependency Container Injection
        val db = AppDatabase.getInstance(this)
        val api = FinancialApiImpl()
        val repository = ApplicationRepositoryImpl(db.applicationDao(), api)
        val stateMachine = ApplicationStateMachine()

        val submitUseCase = SubmitApplicationUseCase(repository, stateMachine)
        val verifyUseCase = VerifyDocumentUseCase(repository, stateMachine)
        val creditCheckUseCase = StartCreditCheckUseCase(repository, stateMachine)
        val processPaymentUseCase = ProcessPaymentUseCase(repository, stateMachine)
        val retryPaymentUseCase = RetryPaymentUseCase(repository, stateMachine, processPaymentUseCase)
        val refreshStatusUseCase = RefreshApplicationStatusUseCase(repository, stateMachine)
        val resetUseCase = ResetApplicationUseCase(repository)

        val viewModelFactory = ApplicationViewModel.Factory(
            appId = "APP-1001",
            repository = repository,
            stateMachine = stateMachine,
            submitUseCase = submitUseCase,
            verifyUseCase = verifyUseCase,
            creditCheckUseCase = creditCheckUseCase,
            processPaymentUseCase = processPaymentUseCase,
            retryPaymentUseCase = retryPaymentUseCase,
            refreshStatusUseCase = refreshStatusUseCase,
            resetUseCase = resetUseCase
        )

        val viewModel: ApplicationViewModel by viewModels { viewModelFactory }

        setContent {
            StateMachineTheme {
                ApplicationScreen(viewModel = viewModel)
            }
        }
    }
}