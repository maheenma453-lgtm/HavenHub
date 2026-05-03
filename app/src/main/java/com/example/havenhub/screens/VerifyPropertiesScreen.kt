package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.havenhub.data.Property
import com.example.havenhub.ui.theme.*
import com.example.havenhub.viewmodel.VerificationViewModel

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val NavyBlue    = Color(0xFF1B2A4A)
private val NavyLight   = Color(0xFF243658)
private val Gold        = Color(0xFFC9A227)
private val GoldDark    = Color(0xFFA07D10)
private val PageBg      = Color(0xFFF4F6FA)
private val GreenOk     = Color(0xFF27AE60)
private val OrangeWarn  = Color(0xFFE67E22)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPropertiesScreen(
    navController: NavController,
    viewModel    : VerificationViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            snackbarHostState.showSnackbar(uiState.successMessage ?: "Action completed successfully")
            viewModel.resetActionState()
        }
    }
    LaunchedEffect(Unit) { viewModel.loadPendingProperties() }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = PageBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NavyBlue, NavyLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Gold)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Verify Properties",
                            color         = Color.White,
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        if (!uiState.isLoading && uiState.pendingProperties.isNotEmpty()) {
                            Text(
                                "${uiState.pendingProperties.size} pending review",
                                color    = Gold.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // ── Loading ───────────────────────────────────────────────────
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Gold, strokeWidth = 3.dp)
                    }
                }

                // ── Empty State ───────────────────────────────────────────────
                uiState.pendingProperties.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(GreenOk.copy(0.18f), GreenOk.copy(0.04f))
                                        )
                                    )
                                    .border(1.5.dp, GreenOk.copy(0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint     = GreenOk,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Text(
                                "All caught up!",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = NavyBlue
                            )
                            Text(
                                "No properties pending verification",
                                fontSize = 13.sp,
                                color    = NavyBlue.copy(alpha = 0.45f)
                            )
                        }
                    }
                }

                // ── List ──────────────────────────────────────────────────────
                else -> {
                    // Pending Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(NavyBlue, NavyLight)))
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Gold.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty, null,
                                    tint     = Gold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "${uiState.pendingProperties.size} Properties Pending Review",
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 14.sp
                                )
                                Text(
                                    "Tap a property to review details",
                                    color    = Gold.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        // Gold shimmer line at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(0.5f), Color.Transparent))
                                )
                                .align(Alignment.BottomCenter)
                        )
                    }

                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.pendingProperties, key = { it.propertyId }) { property ->
                            PremiumPropertyVerifyCard(
                                property = property,
                                onClick  = {
                                    navController.navigate(
                                        "property_verification_detail/${property.propertyId}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Premium Property Card ──────────────────────────────────────────────────────
@Composable
fun PremiumPropertyVerifyCard(property: Property, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = NavyBlue.copy(0.08f),
                spotColor    = NavyBlue.copy(0.12f)
            )
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth(),
            onClick   = onClick,
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(NavyBlue, Gold)))
            )

            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(NavyBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home, null,
                        tint     = Gold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        property.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = NavyBlue,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        property.address.ifEmpty { property.city },
                        fontSize = 12.sp,
                        color    = NavyBlue.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Type badge
                        Surface(
                            color = NavyBlue.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, NavyBlue.copy(0.2f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                property.propertyType.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize   = 11.sp,
                                color      = NavyBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                        // Price badge
                        Surface(
                            color = Gold.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.border(1.dp, Gold.copy(0.3f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                property.formattedPrice,
                                fontSize   = 11.sp,
                                color      = GoldDark,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pending badge
                    Surface(
                        color = OrangeWarn.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, OrangeWarn.copy(0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(OrangeWarn)
                            )
                            Text(
                                "Pending",
                                fontSize   = 11.sp,
                                color      = OrangeWarn,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Arrow circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                            tint     = Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
