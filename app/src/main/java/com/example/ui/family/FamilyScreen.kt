package com.example.ui.family

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.FamilyMemberEntity
import com.example.ui.viewmodel.MedRemindViewModel
import java.util.Locale

private val DarkSlateBg = Color(0xFFFFFFFF)
private val CardBg = Color(0xFFF8F9FA)
private val CardBorder = Color(0xFFE9ECEF)
private val PrimaryTeal = Color(0xFF10B981)
private val AccentOrange = Color(0xFF6366F1) // Secondary Indigo
private val RefillRed = Color(0xFFEF4444)   // Danger Red
private val TextWhite = Color(0xFF111827)   // Text Primary (near black)
private val TextGray = Color(0xFF6B7280)    // Text Secondary (grey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    viewModel: MedRemindViewModel,
    modifier: Modifier = Modifier
) {
    val familyMembers by viewModel.familyMembers.collectAsState()
    val generatedCode by viewModel.generatedInviteCode.collectAsState()
    val context = LocalContext.current

    // Local invite generation inputs
    var nameInput by remember { mutableStateOf("") }
    var relationshipInput by remember { mutableStateOf("Daughter") }
    var permissionInput by remember { mutableStateOf("VIEW") }
    var notifyMissedInput by remember { mutableStateOf(true) }

    // Local linking code input
    var linkCodeInput by remember { mutableStateOf("") }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    val relationshipOptions = listOf("Daughter", "Son", "Spouse", "Parent", "Caregiver", "Friend")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------------------------------------------------------------------------
        // Header
        // -------------------------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Family Care",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Family Sharing",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.testTag("family_screen_title")
                    )
                    Text(
                        text = "Co-manage therapies & track adherence together",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }
        }

        // -------------------------------------------------------------------------
        // Status Alerts / feedback
        // -------------------------------------------------------------------------
        if (feedbackMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFeedbackError) RefillRed.copy(alpha = 0.15f) else PrimaryTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isFeedbackError) RefillRed else PrimaryTeal,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFeedbackError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = "Feedback Status",
                            tint = if (isFeedbackError) RefillRed else PrimaryTeal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feedbackMessage ?: "",
                            color = TextWhite,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { feedbackMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Feedback",
                                tint = TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // Section: Linked Family Members
        // -------------------------------------------------------------------------
        item {
            Text(
                text = "Connected Family Members (${familyMembers.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (familyMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PeopleOutline,
                            contentDescription = "No family members linked",
                            tint = TextGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Linked Family Members Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate and share an invite code below to securely track a family member's treatment adherence.",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(familyMembers, key = { it.id }) { member ->
                FamilyMemberCard(
                    member = member,
                    onDelete = {
                        viewModel.deleteFamilyMember(member.id)
                        feedbackMessage = "Removed association with ${member.name}"
                        isFeedbackError = false
                    }
                )
            }
        }

        // -------------------------------------------------------------------------
        // Section: Link/Accept New Care Member
        // -------------------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter Invite Code",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Enter a 6-digit invitation code received from a family member to establish a secure, shared care link.",
                        fontSize = 12.sp,
                        color = TextGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = linkCodeInput,
                            onValueChange = {
                                if (it.length <= 6) linkCodeInput = it
                            },
                            placeholder = { Text("E.g. 529307", color = TextGray) },
                            textStyle = LocalTextStyle.current.copy(color = TextWhite),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryTeal,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = PrimaryTeal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("link_code_input")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (linkCodeInput.length == 6) {
                                    viewModel.acceptInviteCode(linkCodeInput) { success, msg ->
                                        isFeedbackError = !success
                                        feedbackMessage = msg
                                        if (success) linkCodeInput = ""
                                    }
                                } else {
                                    feedbackMessage = "Please enter a valid 6-digit invite code"
                                    isFeedbackError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            enabled = linkCodeInput.length == 6,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("link_code_submit")
                        ) {
                            Text("Link Link", fontWeight = FontWeight.Bold, color = DarkSlateBg)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // Section: Generate Sharing Invitation
        // -------------------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Generate Caregiver Invite",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Produce a shareable invitation code containing custom permissions.",
                        fontSize = 12.sp,
                        color = TextGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Relative's Full Name", color = TextGray) },
                        textStyle = LocalTextStyle.current.copy(color = TextWhite),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = CardBorder,
                            cursorColor = PrimaryTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Relationship Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relationshipOptions.take(3).forEach { option ->
                            val isSelected = relationshipInput == option
                            Surface(
                                onClick = { relationshipInput = option },
                                color = if (isSelected) PrimaryTeal.copy(alpha = 0.2f) else CardBorder.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryTeal else CardBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = option,
                                    color = if (isSelected) PrimaryTeal else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relationshipOptions.drop(3).forEach { option ->
                            val isSelected = relationshipInput == option
                            Surface(
                                onClick = { relationshipInput = option },
                                color = if (isSelected) PrimaryTeal.copy(alpha = 0.2f) else CardBorder.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryTeal else CardBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = option,
                                    color = if (isSelected) PrimaryTeal else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Level",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextGray
                            )
                            Text(
                                text = if (permissionInput == "VIEW") "Can only inspect progress" else "Can log doses on behalf of patient",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                        Row {
                            Button(
                                onClick = { permissionInput = "VIEW" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (permissionInput == "VIEW") PrimaryTeal else CardBorder.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                modifier = Modifier.testTag("perm_view_btn")
                            ) {
                                Text("VIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (permissionInput == "VIEW") DarkSlateBg else TextWhite)
                            }
                            Button(
                                onClick = { permissionInput = "MANAGE" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (permissionInput == "MANAGE") PrimaryTeal else CardBorder.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier.testTag("perm_manage_btn")
                            ) {
                                Text("MANAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (permissionInput == "MANAGE") DarkSlateBg else TextWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notify Missed Doses",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                            Text(
                                text = "Send automated local notifications if therapy milestones are missed",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                        Switch(
                            checked = notifyMissedInput,
                            onCheckedChange = { notifyMissedInput = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryTeal,
                                checkedTrackColor = PrimaryTeal.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = CardBorder
                            ),
                            modifier = Modifier.testTag("notify_missed_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                viewModel.generateInviteCode(
                                    relationship = relationshipInput,
                                    permission = permissionInput,
                                    notifyMissed = notifyMissedInput,
                                    name = nameInput
                                )
                                feedbackMessage = "Generative success! Shared invitation created for $nameInput"
                                isFeedbackError = false
                            } else {
                                feedbackMessage = "Please supply your relative's full name first"
                                isFeedbackError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_invite_btn"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = DarkSlateBg,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Invite Code",
                            fontWeight = FontWeight.Bold,
                            color = DarkSlateBg,
                            fontSize = 14.sp
                        )
                    }

                    // Bottom: Codigo Compartido Section
                    AnimatedVisibility(
                        visible = generatedCode != null,
                        enter = expandIn() + fadeIn(),
                        exit = shrinkOut() + fadeOut()
                    ) {
                        generatedCode?.let { code ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .border(1.dp, AccentOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(AccentOrange.copy(alpha = 0.05f))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SECURE 6-DIGIT CODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = code,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextWhite,
                                    letterSpacing = 4.sp,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .testTag("active_invite_code")
                                )
                                Text(
                                    text = "Ready to share with $nameInput",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TITLE, "MedRemind Family Invitation")
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Hi $nameInput, use my secure invite code: $code inside MedRemind to connect as my $relationshipInput with $permissionInput permission!"
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Caregiver Invitation"))
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                                        border = BorderStroke(1.dp, AccentOrange),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("share_sheet_btn")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FamilyMemberCard(
    member: FamilyMemberEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("family_member_${member.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Monogram Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryTeal, AccentOrange)
                            )
                        )
                ) {
                    val initials = member.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                    Text(
                        text = initials.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = DarkSlateBg,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = member.relationship,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentOrange,
                            modifier = Modifier
                                .border(1.dp, AccentOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Access: ${member.permission} • ${if (member.notifyMissed) "Alerts Active" else "No Alerts"}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_family_member_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Member",
                        tint = RefillRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Adherence Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Therapy Adherence",
                    fontSize = 13.sp,
                    color = TextGray
                )
                Text(
                    text = "${(member.adherence * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (member.adherence >= 0.85f) PrimaryTeal else if (member.adherence >= 0.7f) AccentOrange else RefillRed
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = member.adherence,
                color = if (member.adherence >= 0.85f) PrimaryTeal else if (member.adherence >= 0.7f) AccentOrange else RefillRed,
                trackColor = CardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            // Missed Dose badge representation
            if (member.hasMissedDose) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RefillRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .background(RefillRed.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Missed Dose",
                        tint = RefillRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Missed dose",
                        color = RefillRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("missed_dose_badge")
                    )
                }
            }
        }
    }
}
