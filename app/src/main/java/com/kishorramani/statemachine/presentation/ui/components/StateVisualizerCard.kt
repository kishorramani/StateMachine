package com.kishorramani.statemachine.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StateVisualizerCard(
    currentState: ApplicationState?,
    modifier: Modifier = Modifier
) {
    val allStates = listOf(
        ApplicationState.Draft,
        ApplicationState.Submitted,
        ApplicationState.DocumentVerification,
        ApplicationState.VerificationFailed,
        ApplicationState.CreditChecking,
        ApplicationState.Approved,
        ApplicationState.Rejected,
        ApplicationState.PaymentPending,
        ApplicationState.PaymentProcessing,
        ApplicationState.PaymentFailed,
        ApplicationState.PaymentSuccess,
        ApplicationState.Completed
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2C)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C63FF))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIFECYCLE STATE MACHINE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF9E9EB2),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active state banner
            val bannerBg = when (currentState) {
                ApplicationState.Completed, ApplicationState.PaymentSuccess -> Brush.horizontalGradient(
                    listOf(Color(0xFF00C853), Color(0xFF00E676))
                )
                ApplicationState.VerificationFailed, ApplicationState.Rejected, ApplicationState.PaymentFailed -> Brush.horizontalGradient(
                    listOf(Color(0xFFFF1744), Color(0xFFFF5252))
                )
                else -> Brush.horizontalGradient(
                    listOf(Color(0xFF6C63FF), Color(0xFF8A2BE2))
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(bannerBg)
                        .padding(vertical = 14.dp, horizontal = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current State",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                            Text(
                                text = currentState?.displayName() ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (currentState?.isTerminal == true) "TERMINAL" else "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "State Nodes Map",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8E8EA8),
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // State node chips flow layout
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allStates.forEach { state ->
                    val isActive = state == currentState
                    val chipBg by animateColorAsState(
                        targetValue = when {
                            isActive && (state is ApplicationState.Completed || state is ApplicationState.PaymentSuccess) -> Color(0xFF00C853)
                            isActive && (state is ApplicationState.VerificationFailed || state is ApplicationState.Rejected || state is ApplicationState.PaymentFailed) -> Color(0xFFFF1744)
                            isActive -> Color(0xFF6C63FF)
                            else -> Color(0xFF2A2A3D)
                        },
                        label = "chipBg"
                    )

                    val textColor = if (isActive) Color.White else Color(0xFF9E9EB2)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipBg)
                            .border(
                                width = if (isActive) 1.5.dp else 0.dp,
                                color = if (isActive) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = state.displayName(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}
