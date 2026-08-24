package com.kishorramani.statemachine.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishorramani.statemachine.domain.model.FinancialApplication

@Composable
fun PaymentDashboardCard(
    app: FinancialApplication?,
    modifier: Modifier = Modifier
) {
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
                        .width(4.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PAYMENT & IDEMPOTENCY TRACKER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF9E9EB2),
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Info rows grid
            InfoRow(label = "Application ID", value = app?.id ?: "-")
            InfoRow(label = "Applicant", value = app?.applicantName ?: "-")
            InfoRow(label = "Amount", value = "\$${app?.amount ?: 0.0}")
            InfoRow(label = "Payment ID", value = app?.paymentId ?: "(Not generated yet)")
            
            // Idempotency Key highlight
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF12121C))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Idempotency Key (UUID)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8E8EA8))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app?.idempotencyKey ?: "Will be generated on Start Payment",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (app?.idempotencyKey != null) Color(0xFF00E676) else Color(0xFF6C6C80),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            if (app?.retryCount ?: 0 > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Retry Attempt Count", value = "${app?.retryCount} retries executed")
            }

            if (app?.failureReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3B1E2B))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Last Failure Detail",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = app.failureReason,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8EA8))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
        )
    }
}
