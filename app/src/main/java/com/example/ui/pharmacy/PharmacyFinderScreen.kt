package com.example.ui.pharmacy

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.ui.viewmodel.MedRemindViewModel
import com.example.data.api.PharmacyUiModel
import java.util.Locale

private val DarkSlateBg = Color(0xFF0F111A)
private val CardBg = Color(0xFF1C1E2D)
private val CardBorder = Color(0xFF2E324E)
private val PrimaryTeal = Color(0xFF10B981)
private val AccentOrange = Color(0xFFF59E0B)
private val RefillRed = Color(0xFFF87171)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF9CA3AF)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun PharmacyFinderScreen(
    viewModel: MedRemindViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pharmacies by viewModel.pharmacies.collectAsState()
    val isLoading by viewModel.isPharmacyLoading.collectAsState()
    val errorMsg by viewModel.pharmacyError.collectAsState()
    val isSimulated by viewModel.isPharmacySimulated.collectAsState()
    val medicines by viewModel.medicines.collectAsState()

    // 1. Red banner count of items needing refill
    val lowStockCount = remember(medicines) {
        medicines.count { it.pillCount <= it.refillThreshold }
    }

    // Location config
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberPermissionState(
        permission = android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    var hasRequestedLocation by remember { mutableStateOf(false) }

    val fetchLocationAndSearch: () -> Unit = {
        hasRequestedLocation = true
        if (permissionState.status.isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    try {
                        if (location != null) {
                            viewModel.searchNearbyPharmacies(location.latitude, location.longitude)
                        } else {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                CancellationTokenSource().token
                            ).addOnSuccessListener { curLocation ->
                                if (curLocation != null) {
                                    viewModel.searchNearbyPharmacies(curLocation.latitude, curLocation.longitude)
                                } else {
                                    // SF Fallback
                                    viewModel.searchNearbyPharmacies(37.7749, -122.4194)
                                }
                            }.addOnFailureListener {
                                viewModel.searchNearbyPharmacies(37.7749, -122.4194)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PharmacyFinderScreen", "Error query location inside success listener: ${e.message}", e)
                        viewModel.searchNearbyPharmacies(37.7749, -122.4194)
                    }
                }.addOnFailureListener {
                    viewModel.searchNearbyPharmacies(37.7749, -122.4194)
                }
            } catch (e: Exception) {
                Log.e("PharmacyFinderScreen", "Error querying FusedLocationProviderClient: ${e.message}", e)
                viewModel.searchNearbyPharmacies(37.7749, -122.4194)
            }
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            fetchLocationAndSearch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Refill Pharmacy Finder",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 20.sp,
                        modifier = Modifier.testTag("pharmacy_finder_title")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = "Pharmacy icon", tint = PrimaryTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        },
        containerColor = DarkSlateBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkSlateBg)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Red banner if any medicine pillCount <= refillThreshold
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
                        .padding(bottom = 12.dp)
                        .testTag("pharmacy_low_stock_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Refill warning alert",
                            tint = RefillRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$lowStockCount ${if (lowStockCount == 1) "medicine needs" else "medicines need"} refill!",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Contact nearby pharmacies below to replenish stock immediately.",
                                color = TextWhite.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Notice/instructions banner
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info icon",
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Powered by Free OpenStreetMap Overpass API (no API keys required). Shows dispensaries within 3km.",
                        fontSize = 12.sp,
                        color = TextGray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Errors notice / fallback link
            if (errorMsg != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Error notification", tint = AccentOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = errorMsg ?: "", color = TextWhite, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // Google Maps Fallback Button inside error box
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=pharmacy+near+me"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("fallback_gmaps_btn_error")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search on Google Maps", color = DarkSlateBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Permission Request Card Block
            if (!permissionState.status.isGranted) {
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CardBg)
                                .border(1.dp, CardBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Location Permission Required",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We require GPS access to map out pharmacy distances and verify locations near you.",
                            fontSize = 13.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { permissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("request_location_perm_btn")
                        ) {
                            Text("Grant Location Permission", fontWeight = FontWeight.Bold, color = DarkSlateBg)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadMockPharmacies(37.7749, -122.4194) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Simulate Local Offline Database", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Scanning OpenStreetMap database...", color = TextGray, fontSize = 14.sp)
                        }
                    }
                } else if (pharmacies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Nearby Pharmacies Found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No OSM pharmacies in the vicinity. Use the search link below to fallback.",
                                fontSize = 13.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=pharmacy+near+me"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("fallback_gmaps_btn_empty")
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search on Google Maps", fontWeight = FontWeight.Bold, color = DarkSlateBg)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = fetchLocationAndSearch,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                border = BorderStroke(1.dp, CardBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Scanning Area")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("pharmacy_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nearby Facilities (${pharmacies.size})",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                // Fallback maps launcher at header
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=pharmacy+near+me"))
                                        context.startActivity(intent)
                                    },
                                    border = BorderStroke(1.dp, CardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Maps Search", fontSize = 11.sp)
                                }
                            }
                        }
                        
                        items(pharmacies, key = { it.id }) { pharmacy ->
                            PharmacyCard(pharmacy = pharmacy, context = context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(pharmacy: PharmacyUiModel, context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pharmacy_item_card_${pharmacy.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pharmacy.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pharmacy.vicinity ?: "Address unavailable",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // formatted in Km directly
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryTeal.copy(alpha = 0.12f))
                        .border(1.dp, PrimaryTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f km", pharmacy.distanceKm),
                        color = PrimaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Open status tag / hours
                val hoursText = pharmacy.isOpenStr
                if (hoursText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Hours indicator",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hoursText,
                            fontSize = 11.sp,
                            color = TextWhite,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TextGray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hours Unspecified",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Call Action Button
                val hasPhone = !pharmacy.phoneNumber.isNullOrBlank()
                Button(
                    onClick = {
                        if (hasPhone) {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${pharmacy.phoneNumber}")
                            }
                            context.startActivity(dialIntent)
                        } else {
                            val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${pharmacy.latitude},${pharmacy.longitude}?q=${Uri.encode(pharmacy.name)}"))
                            context.startActivity(searchIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("call_pharmacy =_button_${pharmacy.id}")
                ) {
                    Icon(
                        imageVector = if (hasPhone) Icons.Default.Phone else Icons.Default.Map,
                        contentDescription = if (hasPhone) "Call Pharmacy" else "View on Map",
                        tint = DarkSlateBg,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasPhone) "Call" else "Directions",
                        fontWeight = FontWeight.Bold,
                        color = DarkSlateBg,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
