package com.example.havenhub.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPropertiesScreen(
    navController: NavController,
    viewModel    : VerificationViewModel = hiltViewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ FIX: errorMessage aaye to snackbar dikhao, phir reset karo
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetActionState()
        }
    }

    // ✅ FIX: actionSuccess true ho to snackbar dikhao (successMessage ke saath), phir reset karo
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            val msg = uiState.successMessage ?: "Action completed successfully"
            snackbarHostState.showSnackbar(msg)
            viewModel.resetActionState()
        }
    }

    // ✅ Screen open hone pe pending properties load karo
    LaunchedEffect(Unit) {
        viewModel.loadPendingProperties()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verify Properties",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        containerColor = Color(0xFFF4F6FB)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }

                uiState.pendingProperties.isEmpty() -> {
                    // ── Empty State ────────────────────────────
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SuccessGreen.copy(.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint     = SuccessGreen,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Text(
                                "All caught up!",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF1A1A2E)
                            )
                            Text(
                                "No properties pending verification",
                                fontSize = 13.sp,
                                color    = Color(0xFF888888)
                            )
                        }
                    }
                }

                else -> {
                    // ── Pending Banner ─────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(PrimaryBlue, Color(0xFF1565C0)))
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier         = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarningOrange.copy(.2f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    null,
                                    tint     = WarningOrange,
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
                                    color    = Color.White.copy(.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.pendingProperties, key = { it.propertyId }) { property ->
                            ModernPropertyVerifyCard(
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

// ── Property Card ─────────────────────────────────────────────────────────────
@Composable
fun ModernPropertyVerifyCard(property: Property, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Property icon
            Box(
                modifier         = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue.copy(.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Home,
                    null,
                    tint     = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    property.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color(0xFF1A1A2E),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    property.address.ifEmpty { property.city },
                    fontSize = 12.sp,
                    color    = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Type badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryBlue.copy(.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            property.propertyType
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            fontSize   = 11.sp,
                            color      = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Price badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentGold.copy(.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            property.formattedPrice,
                            fontSize   = 11.sp,
                            color      = AccentGoldDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Pending badge + arrow
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(WarningOrange.copy(.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "Pending",
                        fontSize   = 11.sp,
                        color      = WarningOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint     = Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}