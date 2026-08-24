package com.kishorramani.statemachine.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishorramani.statemachine.domain.model.ApplicationEvent
import com.kishorramani.statemachine.domain.repository.ChaosMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChaosSimulationCard(
    currentChaosMode: ChaosMode,
    onSelectChaosMode: (ChaosMode) -> Unit,
    onTriggerConcurrentEvents: () -> Unit,
    onAttemptInvalidTransition: (ApplicationEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        ChaosMode.Normal to "Normal API",
        ChaosMode.Timeout to "Simulate Timeout (Retryable)",
        ChaosMode.ServerError500 to "Simulate 503 Server Error",
        ChaosMode.NonRetryableError400 to "Simulate 400 Bad Request",
        ChaosMode.ServerAheadMismatch to "Server Ahead Mismatch"
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
                        .background(Color(0xFFFF9100))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHAOS ENGINEERING & TEST CONTROLS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF9E9EB2),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select Network / Server Mode:",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8EA8))
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                modes.forEach { (mode, title) ->
                    val isSelected = currentChaosMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFFFF9100) else Color(0xFF2A2A3D)
                            )
                            .clickable { onSelectChaosMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Special Fault Injections:",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8EA8))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTriggerConcurrentEvents,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9100)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Simulate Race Condition",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }

                OutlinedButton(
                    onClick = { onAttemptInvalidTransition(ApplicationEvent.PaymentSuccess) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Test Invalid Transition",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )
                }
            }
        }
    }
}
