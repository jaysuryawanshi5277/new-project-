package com.example.ui.schedule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.DoseLog
import com.example.ui.dashboard.DoseStatus

// Theme colors
val DarkSlateBg = Color(0xFFFFFFFF)
val CardBg = Color(0xFFF8F9FA)
val CardBorder = Color(0xFFE9ECEF)
val PrimaryTeal = Color(0xFF10B981)
val AccentOrange = Color(0xFF6366F1) // Secondary Indigo
val RefillRed = Color(0xFFEF4444)   // Danger Red
val TextWhite = Color(0xFF111827)   // Text Primary (near black)
val TextGray = Color(0xFF6B7280)    // Text Secondary (grey)
val TextSecondary = Color(0xFF9CA3AF) // Text Tertiary (light grey)

@Composable
fun ScheduleScreen(
    doseList: List<DoseLog>,
    onStatusChanged: (String, DoseStatus) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Taken", "Missed")

    // Filter dose lists based on interactive filters
    val filteredLogs = remember(doseList, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> doseList.filter { it.status == DoseStatus.SCHEDULED }
            "Taken" -> doseList.filter { it.status == DoseStatus.TAKEN }
            "Missed" -> doseList.filter { it.status == DoseStatus.MISSED }
            else -> doseList
        }
    }

    val totalDoses = doseList.size
    val takenDoses = doseList.count { it.status == DoseStatus.TAKEN }
    val adherenceFraction = if (totalDoses > 0) takenDoses.toFloat() / totalDoses else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header panel with date info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Schedule Icon",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Timeline",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Your dosage sequence scheduled for today",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }
                }

                // Small calendar chip representation
                Box(
                    modifier = Modifier
                        .background(CardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TODAY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
            }

            // Timeline Adherence Progress Overview matching premium aesthetics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adherence Progress",
                            fontSize = 13.sp,
                            color = TextGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$takenDoses of $totalDoses logged taken",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Thin beautiful progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(CardBorder, CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(adherenceFraction)
                                    .height(5.dp)
                                    .background(PrimaryTeal, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Large circular radial percentage
                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { adherenceFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = PrimaryTeal,
                            strokeWidth = 4.dp,
                            trackColor = CardBorder,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "${(adherenceFraction * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }

            // Quick Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) PrimaryTeal else CardBg,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else CardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_chip_$filter")
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Timeline Items
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Timeline Empty",
                            tint = CardBorder,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Doses In This Category",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try switching filters or add items in Medications.",
                            color = TextGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredLogs) { log ->
                        TimelineRowItem(
                            log = log,
                            onStatusChanged = { newStatus -> onStatusChanged(log.id, newStatus) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineRowItem(
    log: DoseLog,
    onStatusChanged: (DoseStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_row_${log.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical connecting visual line to compose a beautiful clinical dashboard timeline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Visual indicator dot depending on dose status
            val indicatorColor = when (log.status) {
                DoseStatus.TAKEN -> PrimaryTeal
                DoseStatus.MISSED -> RefillRed
                DoseStatus.SKIPPED -> TextGray
                DoseStatus.SCHEDULED -> AccentOrange
            }

            val indicatorIcon = when (log.status) {
                DoseStatus.TAKEN -> Icons.Default.Check
                DoseStatus.MISSED -> Icons.Default.PriorityHigh
                DoseStatus.SKIPPED -> Icons.Default.Remove
                DoseStatus.SCHEDULED -> Icons.Default.Schedule
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(indicatorColor.copy(alpha = 0.12f), CircleShape)
                    .border(2.dp, indicatorColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = indicatorIcon,
                    contentDescription = null,
                    tint = indicatorColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Connecting visual stem
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(90.dp)
                    .background(CardBorder)
            )
        }

        // Timeline item card details
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .testTag("schedule_card_${log.id}"),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.scheduledTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.medicineName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(CardBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = log.dosage,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Interactive state logger controls inside timeline
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (log.status) {
                        DoseStatus.SCHEDULED -> {
                            Button(
                                onClick = { onStatusChanged(DoseStatus.TAKEN) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("timeline_btn_take_${log.id}")
                            ) {
                                Text("Take", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Reject mark button
                            IconButton(
                                onClick = { onStatusChanged(DoseStatus.MISSED) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CardBorder, RoundedCornerShape(8.dp))
                                    .testTag("timeline_btn_miss_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Miss",
                                    tint = RefillRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DoseStatus.TAKEN -> {
                            Row(
                                modifier = Modifier
                                    .background(PrimaryTeal.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { onStatusChanged(DoseStatus.SCHEDULED) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Taken", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DoseStatus.MISSED -> {
                            Row(
                                modifier = Modifier
                                    .background(RefillRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { onStatusChanged(DoseStatus.SCHEDULED) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = RefillRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Missed", color = RefillRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        DoseStatus.SKIPPED -> {
                            Row(
                                modifier = Modifier
                                    .background(TextGray.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { onStatusChanged(DoseStatus.SCHEDULED) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Skipped", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
