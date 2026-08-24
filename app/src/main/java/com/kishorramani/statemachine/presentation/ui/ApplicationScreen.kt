package com.kishorramani.statemachine.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishorramani.statemachine.domain.model.ApplicationState
import com.kishorramani.statemachine.presentation.ApplicationViewModel
import com.kishorramani.statemachine.presentation.ui.components.AuditLogCard
import com.kishorramani.statemachine.presentation.ui.components.ChaosSimulationCard
import com.kishorramani.statemachine.presentation.ui.components.PaymentDashboardCard
import com.kishorramani.statemachine.presentation.ui.components.StateVisualizerCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApplicationScreen(
    viewModel: ApplicationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF12121D),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FINANCIAL STATE MACHINE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Clean Architecture + Room + Idempotency",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8E8EA8))
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.resetApplication() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6C63FF))
                    ) {
                        Text("Reset App", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B1B28)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Notification Snackbar if present
                AnimatedVisibility(
                    visible = uiState.userNotification != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.userNotification?.let { notif ->
                        Snackbar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = if (uiState.isErrorNotification) Color(0xFFD32F2F) else Color(0xFF388E3C),
                            action = {
                                TextButton(onClick = { viewModel.dismissNotification() }) {
                                    Text("DISMISS", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Text(text = notif, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 1. State Machine Visualization Card
                StateVisualizerCard(currentState = uiState.application?.state)

                // 2. Dynamic Action Control Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6C63FF))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AVAILABLE BUSINESS ACTIONS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF9E9EB2),
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val state = uiState.application?.state

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (state) {
                                ApplicationState.Draft -> {
                                    Button(
                                        onClick = { viewModel.submitApplication() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Submit Application")
                                    }
                                }
                                ApplicationState.Submitted -> {
                                    Button(
                                        onClick = { viewModel.startDocumentVerification() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Start Document Verification")
                                    }
                                }
                                ApplicationState.DocumentVerification -> {
                                    Button(
                                        onClick = { viewModel.completeDocumentVerification(approved = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Approve Documents")
                                    }
                                    Button(
                                        onClick = { viewModel.completeDocumentVerification(approved = false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Reject Documents")
                                    }
                                }
                                ApplicationState.CreditChecking -> {
                                    Button(
                                        onClick = { viewModel.performCreditCheck(approved = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Approve Credit Check")
                                    }
                                    Button(
                                        onClick = { viewModel.performCreditCheck(approved = false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Reject Credit Check")
                                    }
                                }
                                ApplicationState.Approved -> {
                                    Button(
                                        onClick = { viewModel.startPayment() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Start Payment ($1,500.00)")
                                    }
                                }
                                ApplicationState.PaymentFailed -> {
                                    Button(
                                        onClick = { viewModel.retryPayment() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Retry Payment (Same Idempotency Key)")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.refreshRemoteStatus() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Reconcile with Server")
                                    }
                                }
                                ApplicationState.PaymentProcessing -> {
                                    OutlinedButton(
                                        onClick = { viewModel.refreshRemoteStatus() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Reconcile Status with Server")
                                    }
                                }
                                else -> {
                                    Text(
                                        text = if (state?.isTerminal == true) "Application has reached a terminal state." else "Processing state...",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8EA8))
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Payment & Idempotency Tracker
                PaymentDashboardCard(app = uiState.application)

                // 4. Chaos Simulation Controls
                ChaosSimulationCard(
                    currentChaosMode = uiState.chaosMode,
                    onSelectChaosMode = { mode -> viewModel.setChaosMode(mode) },
                    onTriggerConcurrentEvents = { viewModel.triggerConcurrentEventsSimulation() },
                    onAttemptInvalidTransition = { event -> viewModel.attemptInvalidTransition(event) }
                )

                // 5. Audit Log Stream
                AuditLogCard(logs = uiState.auditLogs)
            }

            // Global Loading Indicator Overlay
            if (uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF6C63FF),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Evaluating State Transition...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
