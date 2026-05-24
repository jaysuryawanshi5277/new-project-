package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Custom Dark Mode Color Palette
val DarkSlateBg = Color(0xFF0F111A)
val CardBg = Color(0xFF1C1E2D)
val CardBorder = Color(0xFF2E324E)
val PrimaryTeal = Color(0xFF10B981) // High visibility medical emerald green
val AccentOrange = Color(0xFFF59E0B) // Burning fire streak orange
val RefillRed = Color(0xFFF87171) // Critical warning red
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF9CA3AF)
val TextSecondary = Color(0xFFE5E7EB)

// Supabase-aligned data models
data class Medicine(
    val id: String,
    val name: String,
    val dosage: String,
    val form: String,
    var pillCount: Int,
    val refillThreshold: Int,
    val isActive: Boolean
)

enum class DoseStatus { TAKEN, MISSED, SKIPPED, SCHEDULED }

data class DoseLog(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String,
    var status: DoseStatus
)

// Main screen state wrapper
class MedRemindState(
    currentUserId: String,
    val userName: String,
    val isCaregiver: Boolean,
    initialMedicines: List<Medicine>,
    initialDoseLogs: List<DoseLog>
) {
    var userId by mutableStateOf(currentUserId)
    var medicines = mutableStateListOf<Medicine>().apply { addAll(initialMedicines) }
    var doseLogs = mutableStateListOf<DoseLog>().apply { addAll(initialDoseLogs) }
    
    // Calculates adherence dynamically based on logged-in state changes
    val todayAdherencePercent: Float
        get() {
            val totalScheduled = doseLogs.size
            if (totalScheduled == 0) return 1f
            val taken = doseLogs.count { it.status == DoseStatus.TAKEN }
            return taken.toFloat() / totalScheduled
        }

    // Calculates current streak logically base state + today's influence
    val currentStreak: Int
        get() = if (todayAdherencePercent >= 0.99f) 14 else 13

    // Low stock medicines tracker (pill_count <= refill_threshold)
    val lowStockCount: Int
        get() = medicines.count { it.pillCount <= it.refillThreshold }
}

@Composable
fun DashboardScreen(
    viewModel: com.example.ui.viewmodel.MedRemindViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.viewmodel.MedRemindViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    ),
    onNavigateToPharmacyFinder: () -> Unit = {}
) {
    val isCaregiverExpandedMode by viewModel.isCaregiverModeGlobal.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val doseLogs by viewModel.todayDoseLogs.collectAsState()
    val todayAdherencePercent by viewModel.todayAdherencePercent.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()

    val userName = if (isCaregiverExpandedMode) "Charles Vance" else "Margaret Vance"

    // Premium dark-slate theme background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header bar
            HeaderSection(
                userName = userName,
                isCaregiver = isCaregiverExpandedMode,
                onToggleUser = { viewModel.toggleCaregiverMode(!isCaregiverExpandedMode) }
            )

            // Low Stock Refill Reminder Banner
            AnimatedVisibility(
                visible = lowStockCount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RefillRed.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, RefillRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("low_stock_refill_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low Stock Alert",
                                tint = RefillRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$lowStockCount ${if (lowStockCount == 1) "medicine needs" else "medicines need"} refill — find pharmacy nearby",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onNavigateToPharmacyFinder,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("banner_find_pharmacy_button")
                        ) {
                            Text(
                                text = "Find Pharmacy",
                                color = DarkSlateBg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Modern vertical scroll containing KPIs, Line chart, and interactive cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Task 1: Three KPI cards at the top
                item {
                    KpiCardsSection(
                        adherence = todayAdherencePercent,
                        streak = currentStreak,
                        lowStockAmount = lowStockCount
                    )
                }

                // Task 1 (Section 2): 30-Day Adherence line chart
                item {
                    InteractiveLineChartCard(
                        todayAdherenceValue = todayAdherencePercent
                    )
                }

                // Interactive Demo Actions for Dose Logger & Medicine Stock refiller
                item {
                    Text(
                        text = if (isCaregiverExpandedMode) "Caregiver Logging Control (Active)" else "Today's Dose Logs (Patient Self-Care)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Map today's dose log items
                items(doseLogs) { log ->
                    DoseLogItemCard(
                        log = log,
                        isCaregiver = isCaregiverExpandedMode,
                        onStatusChanged = { newStatus ->
                            viewModel.updateDoseStatus(log.id, newStatus)
                        }
                    )
                }

                // List of Low Stock Medicines + Quick Refill actions
                item {
                    Text(
                        text = "Medicine Stock Inventory",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(medicines) { medicine ->
                    MedicineInventoryCard(
                        medicine = medicine,
                        onRefill = {
                            viewModel.refillMedicine(medicine.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    userName: String,
    isCaregiver: Boolean,
    onToggleUser: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "MedRemind Heart",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MedRemind",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
            Text(
                text = if (isCaregiver) "Monitoring Care for Mother" else "Patient Dashboard",
                fontSize = 13.sp,
                color = TextGray
            )
        }

        // Toggle user Switch to test both perspectives visually & reactively
        Row(
            modifier = Modifier
                .background(CardBg, RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .clickable { onToggleUser() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCaregiver) Icons.Default.Face else Icons.Default.Person,
                contentDescription = "Switch Role",
                tint = if (isCaregiver) AccentOrange else PrimaryTeal,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isCaregiver) "Caregiver Mode" else "Margaret Vance",
                fontSize = 11.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun KpiCardsSection(
    adherence: Float,
    streak: Int,
    lowStockAmount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Today's Adherence % Card
        KpiCard(
            modifier = Modifier
                .weight(1f)
                .testTag("kpi_adherence"),
            title = "Adherence",
            value = "${(adherence * 100).roundToInt()}%",
            subtext = "Goal: 100%",
            accentColor = PrimaryTeal,
            icon = Icons.Default.CheckCircle,
            fraction = adherence
        )

        // Streak Card
        KpiCard(
            modifier = Modifier
                .weight(1f)
                .testTag("kpi_streak"),
            title = "Current Streak",
            value = "$streak days",
            subtext = "Personal Best: 21d",
            accentColor = AccentOrange,
            icon = Icons.Default.Star,
            fraction = 1f
        )

        // Low stock Pill Counter card
        KpiCard(
            modifier = Modifier
                .weight(1f)
                .testTag("kpi_low_stock"),
            title = "Low On Stock",
            value = if (lowStockAmount == 0) "All Safe" else "$lowStockAmount meds",
            subtext = if (lowStockAmount == 0) "Inventory OK" else "Refills needed",
            accentColor = if (lowStockAmount == 0) PrimaryTeal else RefillRed,
            icon = Icons.Default.Warning,
            fraction = if (lowStockAmount == 0) 1f else 0.3f
        )
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fraction: Float
) {
    Box(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Tiny visual progress accent bar matching frontend design guidelines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun InteractiveLineChartCard(todayAdherenceValue: Float) {
    // Generate simulated adherence trend data for past 30 days
    val originalTrendPoints = remember {
        listOf(
            0.80f, 0.90f, 1.00f, 0.75f, 0.80f, 1.00f, 1.00f,
            0.90f, 0.80f, 1.00f, 1.00f, 0.95f, 0.90f, 1.00f,
            0.80f, 0.85f, 0.90f, 1.00f, 0.70f, 0.80f, 1.00f,
            1.00f, 0.90f, 0.80f, 1.00f, 1.00f, 0.90f, 0.95f,
            1.00f
        )
    }

    // Dynamic appending of today's live percentage so the chart reflects log state updates instantly!
    val points = remember(todayAdherenceValue) {
        originalTrendPoints + todayAdherenceValue
    }

    var selectedPointIdx by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adherence_line_chart_card"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "30-Day Adherence Trend",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Drag or tap on chart to inspect daily statistics",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                if (selectedPointIdx != null) {
                    val idx = selectedPointIdx!!
                    val percentage = (points[idx] * 100).roundToInt()
                    Text(
                        text = "Day ${idx + 1}: $percentage%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        modifier = Modifier
                            .background(PrimaryTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "30d Avg: 91%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val density = LocalDensity.current
            var width by remember { mutableStateOf(0f) }
            val heightDp = 160.dp
            val heightPx = with(density) { heightDp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .onSizeChanged { size ->
                        width = size.width.toFloat()
                    }
                    .pointerInput(points) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (width > 0 && points.size > 1) {
                                    val sectionWidth = width / (points.size - 1)
                                    val draggedIndex = (offset.x / sectionWidth).roundToInt()
                                    selectedPointIdx = draggedIndex.coerceIn(0, points.size - 1)
                                }
                            },
                            onDragEnd = { selectedPointIdx = null },
                            onDragCancel = { selectedPointIdx = null },
                            onDrag = { change, _ ->
                                if (width > 0 && points.size > 1) {
                                    val sectionWidth = width / (points.size - 1)
                                    val draggedIndex = (change.position.x / sectionWidth).roundToInt()
                                    selectedPointIdx = draggedIndex.coerceIn(0, points.size - 1)
                                    change.consume()
                                }
                            }
                        )
                    }
                    .drawBehind {
                        val canvasHeight = size.height
                        val totalPoints = points.size
                        val spacingX = if (totalPoints > 1) size.width / (totalPoints - 1) else 0f

                        // Draw clean background horizontal target guidelines grid (0%, 50%, 100%)
                        val lines = listOf(0f, 0.5f, 1f)
                        lines.forEach { fraction ->
                            val y = canvasHeight * (1f - fraction)
                            drawLine(
                                color = CardBorder.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Build smooth Bezier path to map the trend curve smoothly
                        val linePath = Path()
                        val fillPath = Path()

                        if (totalPoints > 1) {
                            val startX = 0f
                            val startY = canvasHeight * (1f - points[0])
                            linePath.moveTo(startX, startY)
                            fillPath.moveTo(startX, canvasHeight)
                            fillPath.lineTo(startX, startY)

                            for (i in 1 until totalPoints) {
                                val currentX = i * spacingX
                                val currentY = canvasHeight * (1f - points[i])

                                val prevX = (i - 1) * spacingX
                                val prevY = canvasHeight * (1f - points[i - 1])

                                // Curve approximation
                                val controlPointX1 = prevX + (currentX - prevX) / 2f
                                val controlPointY1 = prevY
                                val controlPointX2 = prevX + (currentX - prevX) / 2f
                                val controlPointY2 = currentY

                                linePath.cubicTo(
                                    controlPointX1, controlPointY1,
                                    controlPointX2, controlPointY2,
                                    currentX, currentY
                                )

                                fillPath.cubicTo(
                                    controlPointX1, controlPointY1,
                                    controlPointX2, controlPointY2,
                                    currentX, currentY
                                )
                            }
                            fillPath.lineTo(size.width, canvasHeight)
                            fillPath.close()

                            // Draw continuous smooth gradient fill region under adherence path line
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        PrimaryTeal.copy(alpha = 0.35f),
                                        PrimaryTeal.copy(alpha = 0.00f)
                                    ),
                                    startY = 0f,
                                    endY = canvasHeight
                                )
                            )

                            // Draw high contrast outer curve path line
                            drawPath(
                                path = linePath,
                                color = PrimaryTeal,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Highlight currently touched daily index details
                            selectedPointIdx?.let { idx ->
                                val selectedX = idx * spacingX
                                val selectedY = canvasHeight * (1f - points[idx])

                                // Vertical guideline bar
                                drawLine(
                                    color = PrimaryTeal.copy(alpha = 0.4f),
                                    start = Offset(selectedX, 0f),
                                    end = Offset(selectedX, canvasHeight),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )

                                // Dual concentric glow rings indicating precision tracking spot
                                drawCircle(
                                    color = PrimaryTeal.copy(alpha = 0.3f),
                                    radius = 12.dp.toPx(),
                                    center = Offset(selectedX, selectedY)
                                )
                                drawCircle(
                                    color = PrimaryTeal,
                                    radius = 6.dp.toPx(),
                                    center = Offset(selectedX, selectedY)
                                )
                            }
                        }
                    }
            ) {
                // Keep Canvas pure for custom drawings
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis bounds label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "30 Days Ago", fontSize = 10.sp, color = TextGray)
                Text(text = "15 Days Ago", fontSize = 10.sp, color = TextGray)
                Text(text = "Today", fontSize = 10.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DoseLogItemCard(
    log: DoseLog,
    isCaregiver: Boolean,
    onStatusChanged: (DoseStatus) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dose_log_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (log.status == DoseStatus.SCHEDULED) 1.dp else 0.dp,
            color = if (log.status == DoseStatus.SCHEDULED) CardBorder else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Prescription meta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.medicineName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(CardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.dosage,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Schedule details badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Scheduled: ${log.scheduledTime}",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            // Quick interactive status actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (log.status) {
                    DoseStatus.SCHEDULED -> {
                        // Action buttons to log dose
                        Button(
                            onClick = { onStatusChanged(DoseStatus.TAKEN) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_take_${log.id}")
                        ) {
                            Text("Take", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        IconButton(
                            onClick = { onStatusChanged(DoseStatus.MISSED) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(CardBorder, RoundedCornerShape(8.dp))
                                .testTag("btn_miss_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Mark Missed",
                                tint = RefillRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    DoseStatus.TAKEN -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(PrimaryTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable { onStatusChanged(DoseStatus.SCHEDULED) } // Undo-able
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Taken",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Taken", fontSize = 11.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                        }
                    }

                    DoseStatus.MISSED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(RefillRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable { onStatusChanged(DoseStatus.SCHEDULED) } // Undo-able
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Missed",
                                tint = RefillRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Missed", fontSize = 11.sp, color = RefillRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    DoseStatus.SKIPPED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(TextGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable { onStatusChanged(DoseStatus.SCHEDULED) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Skipped", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineInventoryCard(
    medicine: Medicine,
    onRefill: () -> Unit
) {
    val isLowStock = medicine.pillCount <= medicine.refillThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("med_inv_${medicine.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isLowStock) 1.dp else 0.dp,
            color = if (isLowStock) RefillRed.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = medicine.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${medicine.dosage} (${medicine.form})",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Quantity stock levels
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isLowStock) RefillRed.copy(alpha = 0.15f) else CardBorder,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${medicine.pillCount} remaining",
                            fontSize = 10.sp,
                            color = if (isLowStock) RefillRed else PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refill alert ≤ ${medicine.refillThreshold}",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }

            // Quick refill triggers
            Button(
                onClick = onRefill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLowStock) RefillRed else CardBorder,
                    contentColor = if (isLowStock) Color.White else TextSecondary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("btn_refill_${medicine.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refill icon",
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refill", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
