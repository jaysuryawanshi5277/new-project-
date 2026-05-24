package com.example.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DoseLog
import com.example.ui.dashboard.DoseStatus
import com.example.ui.dashboard.Medicine
import com.example.ui.medicines.MedicinesScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.schedule.ScheduleScreen

// Custom Theme Colors mirroring DashboardScreen
val DarkSlateBg = Color(0xFF0F111A)
val CardBg = Color(0xFF1C1E2D)
val CardBorder = Color(0xFF2E324E)
val PrimaryTeal = Color(0xFF10B981)
val AccentOrange = Color(0xFFF59E0B)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF9CA3AF)

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.SpaceDashboard)
    object Medicines : Screen("medicines", "Medicines", Icons.Default.MedicalServices)
    object Schedule : Screen("schedule", "Schedule", Icons.Default.Timeline)
    object Family : Screen("family", "Family", Icons.Default.People)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountCircle)
}

@Composable
fun MainScreen(
    viewModel: com.example.ui.viewmodel.MedRemindViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.viewmodel.MedRemindViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val navController = rememberNavController()

    val sharedMedicines by viewModel.medicines.collectAsState()
    val sharedDoseLogs by viewModel.todayDoseLogs.collectAsState()
    val isCaregiverModeGlobal by viewModel.isCaregiverModeGlobal.collectAsState()
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()

    if (!isOnboardingComplete) {
        com.example.ui.onboarding.OnboardingScreen(
            onFinished = { viewModel.completeOnboarding() }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = CardBg,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val navigationItems = listOf(
                        Screen.Dashboard,
                        Screen.Medicines,
                        Screen.Schedule,
                        Screen.Family,
                        Screen.Profile
                    )

                    navigationItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryTeal,
                                selectedTextColor = PrimaryTeal,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray,
                                indicatorColor = CardBorder
                            ),
                            modifier = Modifier.testTag("tab_${item.route}")
                        )
                    }
                }
            },
            containerColor = DarkSlateBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkSlateBg)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToPharmacyFinder = {
                                navController.navigate("pharmacy_finder")
                            }
                        )
                    }

                    composable("pharmacy_finder") {
                        com.example.ui.pharmacy.PharmacyFinderScreen(viewModel = viewModel)
                    }

                    composable(Screen.Medicines.route) {
                        MedicinesScreen(
                            medicinesList = sharedMedicines,
                            onAddMedicine = { newMed ->
                                viewModel.addMedicine(newMed)
                            },
                            onAdjustStock = { medId, delta ->
                                viewModel.adjustStock(medId, delta)
                            },
                            onToggleActive = { medId ->
                                viewModel.toggleMedicineActive(medId)
                            }
                        )
                    }

                    composable(Screen.Schedule.route) {
                        ScheduleScreen(
                            doseList = sharedDoseLogs,
                            onStatusChanged = { logId, newStatus ->
                                viewModel.updateDoseStatus(logId, newStatus)
                            }
                        )
                    }

                    composable(Screen.Family.route) {
                        com.example.ui.family.FamilyScreen(viewModel = viewModel)
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            isCaregiverMode = isCaregiverModeGlobal,
                            onToggleCaregiverMode = { viewModel.toggleCaregiverMode(it) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
