package com.example.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Theme color constants
private val DarkSlateBg = Color(0xFFFFFFFF)
private val CardBg = Color(0xFFF8F9FA)
private val CardBorder = Color(0xFFE9ECEF)
private val PrimaryTeal = Color(0xFF10B981)
private val AccentOrange = Color(0xFF6366F1) // Secondary Indigo
private val TextWhite = Color(0xFF111827)   // Text Primary (near black)
private val TextGray = Color(0xFF6B7280)    // Text Secondary (grey)
private val TextSecondary = Color(0xFF9CA3AF) // Text Tertiary (light grey)

@Composable
fun ProfileScreen(
    isCaregiverMode: Boolean,
    onToggleCaregiverMode: (Boolean) -> Unit,
    viewModel: com.example.ui.viewmodel.MedRemindViewModel
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe state from ViewModel
    val userName by viewModel.userName.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val reminderAdvanceTime by viewModel.reminderAdvanceTime.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Icon",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Your Profile",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Customize account settings, caregiver mode & notifications",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }

            // Editable Profile Info Section Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("profile_avatar"),
                            shape = CircleShape,
                            color = PrimaryTeal.copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, PrimaryTeal)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val firstLetter = if (userName.trim().isNotEmpty()) {
                                    userName.trim().take(1).uppercase()
                                } else {
                                    "M"
                                }
                                Text(
                                    text = firstLetter,
                                    color = PrimaryTeal,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Name Input
                            OutlinedTextField(
                                value = userName,
                                onValueChange = { viewModel.saveUserName(it) },
                                label = { Text("Your Name", color = TextGray, fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryTeal,
                                    unfocusedBorderColor = CardBorder,
                                    focusedLabelColor = PrimaryTeal,
                                    cursorColor = PrimaryTeal
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone Input
                    OutlinedTextField(
                        value = userPhone,
                        onValueChange = { viewModel.saveUserPhone(it) },
                        label = { Text("Phone Number", color = TextGray, fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor = PrimaryTeal,
                            cursorColor = PrimaryTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input")
                    )
                }
            }

            // Interactive Caregiver Switch Mode Controller Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("caregiver_toggle_card"),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Caregiver Viewpoint Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enables active third-party supervision of patient adherence trends, medication schedules, and refill tracks.",
                                fontSize = 11.sp,
                                color = TextGray,
                                lineHeight = 16.sp
                            )
                        }

                        Switch(
                            checked = isCaregiverMode,
                            onCheckedChange = { onToggleCaregiverMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentOrange,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = CardBorder
                            ),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .testTag("profile_caregiver_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isCaregiverMode) AccentOrange.copy(alpha = 0.08f) else CardBorder.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (isCaregiverMode) {
                                "Currently Active Mode: Caregiver View. You can oversee records, confirm medication dosage, trigger stock refills and manage compliance on behalf of the patient."
                            } else {
                                "Currently Active Mode: Patient Self-Care. Optimized layout with larger interaction tags for straightforward log confirmations."
                            },
                            fontSize = 11.sp,
                            color = if (isCaregiverMode) AccentOrange else TextGray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Settings Section (Sound, Vibration, Reminder Advance Time)
            Text(
                text = "Preferences",
                fontSize = 14.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Notification sound toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Notification Sounds",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Play alarm audio for dose alerts",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { viewModel.saveSoundEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryTeal,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = CardBorder
                            ),
                            modifier = Modifier.testTag("sound_toggle")
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Vibration toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vibration",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Vibrate device for dose alerts",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { viewModel.saveVibrationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryTeal,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = CardBorder
                            ),
                            modifier = Modifier.testTag("vibration_toggle")
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Reminder advance time dropdown row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Reminder Advance Time",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Receive alerts before dose times",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }

                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.testTag("advance_time_dropdown_button")
                            ) {
                                Text(
                                    text = "$reminderAdvanceTime min",
                                    color = PrimaryTeal,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = PrimaryTeal
                                )
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(CardBg).border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            ) {
                                listOf(5, 10, 15).forEach { minutes ->
                                    DropdownMenuItem(
                                        text = { Text("$minutes minutes", color = TextWhite) },
                                        onClick = {
                                            viewModel.saveReminderAdvanceTime(minutes)
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("advance_time_item_$minutes")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Adherence Report PDF Exporter Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adherence Metrics Export",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate a comprehensive clinical PDF report of $userName's medication compliance over the last 30 days. The PDF contains detailed metrics per medicine, overall compliance tracking, and a verification timestamp. It will be saved directly into your device's Downloads directory.",
                        fontSize = 11.sp,
                        color = TextGray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val logs = viewModel.getLogsWithMedicineLast30Days()
                                    if (logs.isEmpty()) {
                                        snackbarHostState.showSnackbar(
                                            message = "No database logs available to export.",
                                            duration = SnackbarDuration.Short
                                        )
                                        return@launch
                                    }
                                    val pdfBytes = PdfExporter.generateAdherencePdf(context, userName, logs)
                                    val fileName = "MedRemind_Adherence_Report_${System.currentTimeMillis()}.pdf"
                                    val uri = PdfExporter.savePdfToDownloads(context, pdfBytes, fileName)
                                    if (uri != null) {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = "Report saved to Downloads",
                                            actionLabel = "Open",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            PdfExporter.openPdf(context, uri)
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to export PDF report.")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_report_button")
                    ) {
                        Text(
                            text = "Export Adherence Report",
                            fontSize = 13.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // App Info Section
            Text(
                text = "App Information",
                fontSize = 14.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column {
                    // Rating option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val appId = context.packageName
                                val playStoreIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=$appId")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=$appId")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(playStoreIntent)
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(browserIntent)
                                    } catch (ex: Exception) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Could not open Play Store links")
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("rate_app_item"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Rate MedRemind",
                                fontSize = 13.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    // Share option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out MedRemind! It helps me track my medication perfectly.")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share MedRemind"))
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("share_app_item"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Share App",
                                fontSize = 13.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    // App Version option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("app_version_item"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "MedRemind Client App Version",
                                fontSize = 13.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "v1.4.1",
                            color = PrimaryTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
