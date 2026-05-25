package com.example.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Theme Colors matching dashboard
private val DarkSlateBg = Color(0xFFFFFFFF)
private val CardBg = Color(0xFFF8F9FA)
private val CardBorder = Color(0xFFE9ECEF)
private val PrimaryTeal = Color(0xFF10B981)
private val AccentOrange = Color(0xFF6366F1) // Secondary Indigo
private val TextWhite = Color(0xFF111827)   // Text Primary (near black)
private val TextGray = Color(0xFF6B7280)    // Text Secondary (grey)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Branding Title Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MedRemind",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryTeal,
                    modifier = Modifier.testTag("onboarding_brand")
                )
            }

            // Main Core Page content using AnimatedContent
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn() with
                                slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn() with
                                slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "OnboardingScreenTransition"
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icon placeholder
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryTeal.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (page) {
                            0 -> PillArtwork()
                            1 -> Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Family Caregiver Icon",
                                tint = PrimaryTeal,
                                modifier = Modifier
                                    .size(88.dp)
                                    .testTag("icon_page_1")
                            )
                            2 -> Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Pharmacy Finder Icon",
                                tint = AccentOrange,
                                modifier = Modifier
                                    .size(88.dp)
                                    .testTag("icon_page_2")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    val (title, subtitle) = when (page) {
                        0 -> "Never Miss a Dose 💊" to "MedRemind reminds you at the right time every day"
                        1 -> "Track Your Family ❤️" to "Monitor medicines for your elderly parents as a caregiver"
                        else -> "Find Pharmacies Nearby 📍" to "Get refill alerts and find open pharmacies instantly"
                    }

                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("onboarding_title_$page")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.testTag("onboarding_subtitle_$page")
                    )
                }
            }

            // Bottom elements
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryTeal else CardBorder)
                                .testTag("dot_indicator_$index")
                        )
                    }
                }

                // Action primary button
                Button(
                    onClick = {
                        if (currentPage < 2) {
                            currentPage++
                        } else {
                            onFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_action_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentPage == 2) "Get Started" else "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        if (currentPage < 2) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = TextWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PillArtwork() {
    Box(
        modifier = Modifier
            .rotate(-45f)
            .size(width = 48.dp, height = 96.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.5.dp, CardBorder, RoundedCornerShape(24.dp))
            .testTag("pill_artwork")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PrimaryTeal)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            )
        }
    }
}
