package com.example.ui.medicines

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.dashboard.Medicine
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.accompanist.permissions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// NOTE: We do not declare DarkSlateBg, CardBg, CardBorder, PrimaryTeal, AccentOrange, TextWhite, TextGray here
// as they are already defined at the package level in MedicinesScreen.kt.

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    onNavigateBack: () -> Unit,
    onSaveMedicine: (Medicine, List<String>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Form inputs states
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("Tablet") }
    var pillCount by remember { mutableStateOf("") }
    var refillThreshold by remember { mutableStateOf("") }
    
    // Multiple daily dose times state list
    val doseTimes = remember { mutableStateListOf("08:00 AM") }

    // Dropdown state
    var formExpanded by remember { mutableStateOf(false) }
    val forms = listOf("Tablet", "Capsule", "Syrup", "Liquid", "Injection", "Other")

    // Camera and scanner states
    var isCameraActive by remember { mutableStateOf(false) }
    var isScanningImage by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Time picker dialog state
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        if (isCameraActive) {
            CameraPreviewScreen(
                onClose = { isCameraActive = false },
                onImageCaptured = { bitmap ->
                    isCameraActive = false
                    isScanningImage = true
                    coroutineScope.launch {
                        val details = extractMedicineDetailsFromImage(context, bitmap)
                        isScanningImage = false
                        if (details != null) {
                            medName = details.first
                            dosage = details.second
                            Toast.makeText(context, "Medicine details auto-filled!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Couldn't read medicine strip. Please fill manually.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Return row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("add_med_back_button")
                            .background(CardBg, CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Add Medicine",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Prescription details & alarm routines",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Scanner section banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan icon",
                                tint = AccentOrange,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scan Medicine Strip",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Auto-extract fields via Vertex AI",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                            Button(
                                onClick = {
                                    if (cameraPermissionState.status.isGranted) {
                                        isCameraActive = true
                                    } else {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("scan_strip_button")
                            ) {
                                Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Basic Details Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Prescription Information",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )

                        // Name Text input
                        OutlinedTextField(
                            value = medName,
                            onValueChange = { medName = it },
                            label = { Text("Medicine Name", color = TextGray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = PrimaryTeal,
                                focusedIndicatorColor = PrimaryTeal,
                                unfocusedIndicatorColor = CardBorder
                            ),
                            placeholder = { Text("e.g., Lisinopril", color = CardBorder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_med_input_name"),
                            singleLine = true
                        )

                        // Dosage Text input
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dosage Strength", color = TextGray) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = PrimaryTeal,
                                focusedIndicatorColor = PrimaryTeal,
                                unfocusedIndicatorColor = CardBorder
                            ),
                            placeholder = { Text("e.g., 20mg, 10ml", color = CardBorder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_med_input_dosage"),
                            singleLine = true
                        )

                        // Form Selector Dropdown List
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = formExpanded,
                                onExpandedChange = { formExpanded = !formExpanded }
                            ) {
                                OutlinedTextField(
                                    value = form,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Form", color = TextGray) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedLabelColor = PrimaryTeal,
                                        focusedIndicatorColor = PrimaryTeal,
                                        unfocusedIndicatorColor = CardBorder
                                    ),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("add_med_input_form")
                                )
                                ExposedDropdownMenu(
                                    expanded = formExpanded,
                                    onDismissRequest = { formExpanded = false },
                                    modifier = Modifier.background(CardBg)
                                ) {
                                    forms.forEach { formatOpt ->
                                        DropdownMenuItem(
                                            text = { Text(text = formatOpt, color = TextWhite) },
                                            onClick = {
                                                form = formatOpt
                                                formExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Pill stock levels and Refill alarms
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = pillCount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) pillCount = it },
                                label = { Text("Pill/Dose Count", color = TextGray) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = PrimaryTeal,
                                    focusedIndicatorColor = PrimaryTeal,
                                    unfocusedIndicatorColor = CardBorder
                                ),
                                placeholder = { Text("30", color = CardBorder) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_med_input_stock"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = refillThreshold,
                                onValueChange = { if (it.all { char -> char.isDigit() }) refillThreshold = it },
                                label = { Text("Alert Threshold", color = TextGray) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedLabelColor = PrimaryTeal,
                                    focusedIndicatorColor = PrimaryTeal,
                                    unfocusedIndicatorColor = CardBorder
                                ),
                                placeholder = { Text("7", color = CardBorder) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_med_input_refill"),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Daily Dose Alarm Scheduler Core Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "Dose Times (Set Multiple)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                            IconButton(
                                onClick = { showTimePickerDialog = true },
                                modifier = Modifier
                                    .testTag("add_time_button")
                                    .background(PrimaryTeal.copy(alpha = 0.1f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAlarm,
                                    contentDescription = "Add Alarm Time",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (doseTimes.isEmpty()) {
                            Text(
                                text = "No dose times added yet. Add at least one schedule.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                doseTimes.forEach { time ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSlateBg, RoundedCornerShape(10.dp))
                                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Clock",
                                                tint = AccentOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = time,
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = { doseTimes.remove(time) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete alarm time",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Primary Save Action
                Button(
                    onClick = {
                        val count = pillCount.toIntOrNull() ?: 30
                        val threshold = refillThreshold.toIntOrNull() ?: 7
                        val finalTimes = if (doseTimes.isEmpty()) listOf("08:00 AM") else doseTimes.toList()
                        
                        val newMedObj = Medicine(
                            id = "med_${System.currentTimeMillis()}",
                            name = medName,
                            dosage = dosage,
                            form = form,
                            pillCount = count,
                            refillThreshold = threshold,
                            isActive = true
                        )
                        onSaveMedicine(newMedObj, finalTimes)
                    },
                    enabled = medName.isNotBlank() && dosage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        disabledContainerColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_medicine_button")
                ) {
                    Text(
                        text = "Save Medicine & Set Alarms",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Processing Overlay Dialog
        if (isScanningImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = PrimaryTeal)
                        Text(
                            text = "Analyzing Package...",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Extracting medicine name and strength via Firebase AI",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }

        // Custom selector dial picker dialog
        if (showTimePickerDialog) {
            CustomTimePickerDialog(
                onDismiss = { showTimePickerDialog = false },
                onTimeSelected = { formattedTime ->
                    if (!doseTimes.contains(formattedTime)) {
                        doseTimes.add(formattedTime)
                    }
                    showTimePickerDialog = false
                }
            )
        }
    }
}

@Composable
fun CameraPreviewScreen(
    onClose: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    LaunchedEffect(Unit) {
        val cameraProviderProvider = ProcessCameraProvider.getInstance(context)
        cameraProviderProvider.addListener({
            val cameraProvider = cameraProviderProvider.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera building failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Align package strip inside frame",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(200.dp)
                    .border(2.dp, PrimaryTeal, RoundedCornerShape(12.dp))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
                    .clickable {
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            executor = cameraExecutor,
                            onPhotoCaptured = onImageCaptured,
                            onError = { err ->
                                Log.e("CameraPreview", "Capture error: ${err.message}", err)
                                Toast.makeText(context, "Error capturing image", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .padding(4.dp)
                    .border(3.dp, DarkSlateBg, CircleShape)
            )

            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

// Custom Picker Dialog with simple Compose states
@Composable
fun CustomTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }
    var isPm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Set Alarm Time",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedHour = if (selectedHour == 12) 1 else selectedHour + 1 }) {
                            Icon(Icons.Default.ArrowDropUp, contentDescription = "Hour Up", tint = PrimaryTeal)
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        IconButton(onClick = { selectedHour = if (selectedHour == 1) 12 else selectedHour - 1 }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Hour Down", tint = PrimaryTeal)
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                            Icon(Icons.Default.ArrowDropUp, contentDescription = "Minute Up", tint = PrimaryTeal)
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        IconButton(onClick = { selectedMinute = if (selectedMinute == 0) 55 else (selectedMinute - 5 + 60) % 60 }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Minute Down", tint = PrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isPm = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isPm) PrimaryTeal else CardBorder
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("AM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isPm) Color.White else TextGray)
                        }

                        Button(
                            onClick = { isPm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPm) PrimaryTeal else CardBorder
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("PM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isPm) Color.White else TextGray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format("%02d:%02d %s", selectedHour, selectedMinute, if (isPm) "PM" else "AM")
                    onTimeSelected(formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}

fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    val file = File(context.cacheDir, "medicine_strip_capture.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val rotatedBitmap = rotateImageIfRequired(file.absolutePath, bitmap)
                        onPhotoCaptured(rotatedBitmap)
                    } else {
                        onError(Exception("Failed to decode bitmap"))
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private fun rotateImageIfRequired(path: String, img: Bitmap): Bitmap {
    return try {
        val exif = android.media.ExifInterface(path)
        val orientation = exif.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_NORMAL
        )
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    } catch (e: Exception) {
        img
    }
}

private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
    val matrix = android.graphics.Matrix()
    matrix.postRotate(degree)
    val rotated = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    return rotated
}

fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val newWidth: Int
    val newHeight: Int
    if (width > height) {
        newWidth = maxDimension
        newHeight = (height * (maxDimension.toFloat() / width.toFloat())).toInt()
    } else {
        newHeight = maxDimension
        newWidth = (width * (maxDimension.toFloat() / height.toFloat())).toInt()
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

fun Bitmap.toBase64(quality: Int = 80): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
}

suspend fun extractMedicineDetailsFromImage(
    context: Context,
    bitmap: Bitmap
): Pair<String, String>? = withContext(Dispatchers.IO) {
    val scaledBitmap = scaleBitmapDown(bitmap, 800)
    val promptText = "Analyze this image of a medicine strip/package. " +
            "Extract the medicine name and dosage (e.g. 10mg, 500mg, 5ml). " +
            "Return response STRICTLY in JSON format with exactly two keys: 'name' and 'dosage'. " +
            "Do not include any extra text formatting, backticks, or explanatory descriptions. " +
            "Example: {\"name\": \"Lisinopril\", \"dosage\": \"10mg\"}"

    // Try Firebase AI First
    try {
        Log.d("AddMedicineScreen", "Attempting extraction via Firebase Vertex AI...")
        val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
        
        val response = model.generateContent(
            com.google.firebase.vertexai.type.content {
                image(scaledBitmap)
                text(promptText)
            }
        )
        
        val textResult = response.text?.toString()
        if (!textResult.isNullOrBlank()) {
            val parsed = parseJsonResult(textResult)
            if (parsed != null) {
                return@withContext parsed
            }
        }
    } catch (e: Exception) {
        Log.w("AddMedicineScreen", "Firebase AI extraction failed: ${e.message}. Trying REST API fallback...")
    }

    // Fallback to Direct REST API using BuildConfig.GEMINI_API_KEY
    try {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY") {
            Log.e("AddMedicineScreen", "No valid Gemini API Key in BuildConfig")
            return@withContext null
        }

        val base64Image = scaledBitmap.toBase64()
        
        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("AddMedicineScreen", "REST API Error code: ${response.code}")
                return@withContext null
            }
            
            val responseBody = response.body?.string() ?: return@withContext null
            Log.d("AddMedicineScreen", "REST API Response: $responseBody")
            
            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val text = firstCandidate.getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).optString("text")
                
                if (!text.isNullOrBlank()) {
                    return@withContext parseJsonResult(text)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("AddMedicineScreen", "REST API Exception: ${e.message}", e)
    }

    return@withContext null
}

private fun parseJsonResult(textResult: String): Pair<String, String>? {
    try {
        var cleanText = textResult.trim()
        if (cleanText.startsWith("```")) {
            cleanText = cleanText.removePrefix("```json")
                .removePrefix("```")
                .substringBeforeLast("```")
                .trim()
        }
        val json = JSONObject(cleanText)
        val name = json.optString("name", "")
        val dosage = json.optString("dosage", "")
        if (name.isNotBlank()) {
            return Pair(name, dosage)
        }
    } catch (e: Exception) {
        Log.e("AddMedicineScreen", "Failed to parse JSON result: ${e.message}, input was: $textResult")
        try {
            val nameRegex = """"name"\s*:\s*"([^"]+)"""".toRegex()
            val dosageRegex = """"dosage"\s*:\s*"([^"]+)"""".toRegex()
            
            val nameMatch = nameRegex.find(textResult)?.groupValues?.get(1)
            val dosageMatch = dosageRegex.find(textResult)?.groupValues?.get(1)
            
            if (!nameMatch.isNullOrBlank()) {
                return Pair(nameMatch, dosageMatch ?: "")
            }
        } catch (re: Exception) {
            // ignore
        }
    }
    return null
}
