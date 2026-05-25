package com.example.ui.medicines

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.Medicine

// Light Professional Theme Color palette matching design spec
val DarkSlateBg = Color(0xFFFFFFFF)
val CardBg = Color(0xFFF8F9FA)
val CardBorder = Color(0xFFE9ECEF)
val PrimaryTeal = Color(0xFF10B981)
val AccentOrange = Color(0xFF6366F1) // Secondary Indigo
val RefillRed = Color(0xFFEF4444)   // Danger Red
val TextWhite = Color(0xFF111827)   // Text Primary (near black)
val TextGray = Color(0xFF6B7280)    // Text Secondary (grey)
val TextSecondary = Color(0xFF9CA3AF) // Text Tertiary (light grey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    medicinesList: List<Medicine>,
    onAddMedicine: (Medicine) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    onToggleActive: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = "Medicines Icon",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Medicines",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Manage your prescriptions and stock levels",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }

            if (medicinesList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "No Medicines",
                            tint = CardBorder,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Medicines Added",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the '+' floating button below to track your first medicine prescription.",
                            color = TextGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(medicinesList) { medicine ->
                        MedicineItemCard(
                            medicine = medicine,
                            onAdjustStock = { qty -> onAdjustStock(medicine.id, qty) },
                            onToggleActive = { onToggleActive(medicine.id) }
                        )
                    }
                }
            }
        }

        // Add Medicine FAB at the bottom right
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_medicine_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Medicine",
                modifier = Modifier.size(28.dp)
            )
        }

        if (showAddDialog) {
            AddMedicineDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newMed ->
                    onAddMedicine(newMed)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MedicineItemCard(
    medicine: Medicine,
    onAdjustStock: (Int) -> Unit,
    onToggleActive: () -> Unit
) {
    val isLowStock = medicine.pillCount <= medicine.refillThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medicines_tab_item_${medicine.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLowStock) RefillRed.copy(alpha = 0.6f) else CardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (medicine.isActive) PrimaryTeal.copy(alpha = 0.12f) else CardBorder,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (medicine.form.lowercase()) {
                                "capsule" -> Icons.Default.Task
                                "liquid" -> Icons.Default.WaterDrop
                                "injection" -> Icons.Default.MedicalServices
                                else -> Icons.Default.Cookie // Pill shape representation
                            },
                            contentDescription = null,
                            tint = if (medicine.isActive) PrimaryTeal else TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = medicine.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (medicine.isActive) TextWhite else TextGray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${medicine.dosage}  •  ${medicine.form}",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }
                }

                // Switch to quickly toggle active status
                Switch(
                    checked = medicine.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryTeal,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = CardBorder
                    ),
                    modifier = Modifier.testTag("switch_active_${medicine.id}")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info rows & Counter control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STOCK LEVEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${medicine.pillCount} remaining",
                            fontSize = 15.sp,
                            color = if (isLowStock) RefillRed else PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLowStock) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(RefillRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LOW",
                                    color = RefillRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Interactive increment / decrement buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (medicine.pillCount > 0) onAdjustStock(-1) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_dec_${medicine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Stock",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = medicine.pillCount.toString(),
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    IconButton(
                        onClick = { onAdjustStock(1) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_inc_${medicine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Stock",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-text alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Refill Alert: Set for ≤ ${medicine.refillThreshold} remaining",
                    fontSize = 11.sp,
                    color = TextGray
                )
                
                // Active status label
                Text(
                    text = if (medicine.isActive) "Monitoring Scheduled" else "Paused Reminders",
                    fontSize = 10.sp,
                    color = if (medicine.isActive) PrimaryTeal else TextGray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onAdd: (Medicine) -> Unit
) {
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("Tablet") }
    var pillCountInput by remember { mutableStateOf("") }
    var refillThresholdInput by remember { mutableStateOf("") }

    val forms = listOf("Tablet", "Capsule", "Liquid", "Injection", "Other")
    var formExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = {
            Text(
                text = "Add Medicine Track",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("Medicine Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = TextGray,
                        focusedIndicatorColor = PrimaryTeal,
                        unfocusedIndicatorColor = CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_name"),
                    singleLine = true
                )

                // Dosage Input
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g., 10mg, 5ml)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = TextGray,
                        focusedIndicatorColor = PrimaryTeal,
                        unfocusedIndicatorColor = CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_dosage"),
                    singleLine = true
                )

                // Form Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = formExpanded,
                        onExpandedChange = { formExpanded = !formExpanded }
                    ) {
                        OutlinedTextField(
                            value = form,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Form") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = PrimaryTeal,
                                unfocusedLabelColor = TextGray,
                                focusedIndicatorColor = PrimaryTeal,
                                unfocusedIndicatorColor = CardBorder
                            ),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dialog_input_form")
                        )
                        ExposedDropdownMenu(
                            expanded = formExpanded,
                            onDismissRequest = { formExpanded = false },
                            modifier = Modifier.background(CardBg)
                        ) {
                            forms.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(text = selection, color = TextWhite) },
                                    onClick = {
                                        form = selection
                                        formExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Initial Pill Count
                OutlinedTextField(
                    value = pillCountInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            pillCountInput = newValue
                        }
                    },
                    label = { Text("Starting Stock Pill/Dose Count") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = TextGray,
                        focusedIndicatorColor = PrimaryTeal,
                        unfocusedIndicatorColor = CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_pill_count"),
                    singleLine = true
                )

                // Refill Warning Threshold
                OutlinedTextField(
                    value = refillThresholdInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            refillThresholdInput = newValue
                        }
                    },
                    label = { Text("Refill LowStock Threshold") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = TextGray,
                        focusedIndicatorColor = PrimaryTeal,
                        unfocusedIndicatorColor = CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_refill_threshold"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = pillCountInput.toIntOrNull() ?: 30
                    val threshold = refillThresholdInput.toIntOrNull() ?: 10
                    if (medName.isNotBlank() && dosage.isNotBlank()) {
                        val newMed = Medicine(
                            id = "med_${System.currentTimeMillis()}",
                            name = medName,
                            dosage = dosage,
                            form = form,
                            pillCount = count,
                            refillThreshold = threshold,
                            isActive = true
                        )
                        onAdd(newMed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                enabled = medName.isNotBlank() && dosage.isNotBlank(),
                modifier = Modifier.testTag("dialog_btn_add")
            ) {
                Text("Add Track", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_btn_cancel")
            ) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}
