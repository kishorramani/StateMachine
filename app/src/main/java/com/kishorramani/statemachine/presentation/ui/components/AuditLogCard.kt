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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.kishorramani.statemachine.domain.model.AuditLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogCard(
    logs: List<AuditLog>,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDIT TRAIL LOGS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF9E9EB2),
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "${logs.size} Events Recorded",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8EA8))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "No audit log entries recorded yet.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6C6C80))
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { item ->
                        AuditLogItem(log = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogItem(log: AuditLog) {
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val formattedTime = formatter.format(Date(log.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141420))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (log.isValid) Color(0xFF00C853) else Color(0xFFFF1744))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (log.isValid) "VALID" else "INVALID",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.event,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8E8EA8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${log.fromState}  ➔  ${log.toState}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB0B0C3),
                    fontSize = 11.sp
                )
            )
        }
    }
}
