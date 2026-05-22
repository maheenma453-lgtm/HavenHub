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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.havenhub.data.Property
import com.example.havenhub.data.PropertyStatus
import com.example.havenhub.viewmodel.ManagementViewModel

private val StatusApproved    = Color(0xFF27AE60)
private val StatusPending     = Color(0xFFE67E22)
private val StatusRejected    = Color(0xFFE74C3C)
private val StatusUnderReview = Color(0xFF2980B9)
private val StatusInactive    = Color(0xFF95A5A6)
private val StatusBooked      = Color(0xFF00897B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePropertiesScreen(
    navController: NavController,
    viewModel    : ManagementViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var showConfirmDialog    by remember { mutableStateOf(false) }
    var pendingAction        by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDialogTitle   by remember { mutableStateOf("") }
    var confirmDialogMessage by remember { mutableStateOf("") }

    val filteredProperties = remember(uiState.properties, searchQuery) {
        uiState.properties.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary         = MaterialTheme.colorScheme.tertiary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val background       = MaterialTheme.colorScheme.background

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            shape            = RoundedCornerShape(16.dp),
            title = {
                Text(confirmDialogTitle, fontWeight = FontWeight.Bold, color = onSurface, fontSize = 18.sp)
            },
            text = {
                Text(confirmDialogMessage, color = onSurface.copy(0.75f), fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = { pendingAction?.invoke(); showConfirmDialog = false; pendingAction = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = tertiary),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm", color = primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, primaryContainer)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tertiary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Manage Properties", color = onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        Text("${filteredProperties.size} properties", color = tertiary.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(2.dp)
                        .background(Brush.horizontalGradient(listOf(background.copy(0f), tertiary, background.copy(0f))))
                        .align(Alignment.BottomCenter)
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Search properties...", color = onSurface.copy(0.4f), fontSize = 14.sp) },
                    leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = tertiary) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = tertiary,
                        unfocusedBorderColor    = onSurface.copy(0.25f),
                        focusedContainerColor   = surface,
                        unfocusedContainerColor = surface,
                        cursorColor             = tertiary,
                        focusedTextColor        = onSurface,
                        unfocusedTextColor      = onSurface
                    )
                )
            }

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProperties) { property ->
                    MPPropertyCard(
                        property  = property,
                        // ← KEY FIX: sirf Super Admin delete kar sakta hai
                        canDelete = uiState.isSuperAdmin,
                        primary   = primary,
                        tertiary  = tertiary,
                        surface   = surface,
                        onSurface = onSurface,
                        onApprove = {
                            confirmDialogTitle   = "Approve Property"
                            confirmDialogMessage = "Approve \"${property.title}\"?"
                            pendingAction        = { viewModel.approveProperty(property.propertyId) }
                            showConfirmDialog    = true
                        },
                        onReject  = {
                            confirmDialogTitle   = "Reject Property"
                            confirmDialogMessage = "Reject \"${property.title}\"?"
                            pendingAction        = { viewModel.removeProperty(property.propertyId) }
                            showConfirmDialog    = true
                        },
                        onDelete  = {
                            confirmDialogTitle   = "Delete Property"
                            confirmDialogMessage = "Permanently delete \"${property.title}\"? This cannot be undone."
                            pendingAction        = { viewModel.deleteProperty(property.propertyId) }
                            showConfirmDialog    = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PropertyManagementCard(
    property : Property,
    onApprove: () -> Unit,
    onReject : () -> Unit,
    onDelete : () -> Unit
) {
    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val surface   = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    MPPropertyCard(
        property  = property,
        canDelete = false, // public wrapper mein delete nahi hona chahiye
        primary   = primary,
        tertiary  = tertiary,
        surface   = surface,
        onSurface = onSurface,
        onApprove = onApprove,
        onReject  = onReject,
        onDelete  = onDelete
    )
}

@Composable
private fun MPPropertyCard(
    property : Property,
    canDelete: Boolean,   // ← naya parameter
    primary  : Color,
    tertiary : Color,
    surface  : Color,
    onSurface: Color,
    onApprove: () -> Unit,
    onReject : () -> Unit,
    onDelete : () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val statusColor = when (property.propertyStatusEnum) {
        PropertyStatus.APPROVED     -> StatusApproved
        PropertyStatus.REJECTED     -> StatusRejected
        PropertyStatus.PENDING      -> StatusPending
        PropertyStatus.UNDER_REVIEW -> StatusUnderReview
        PropertyStatus.INACTIVE     -> StatusInactive
        PropertyStatus.BOOKED       -> StatusBooked
    }

    val imageModel: Any? = when {
        property.coverImageUrl.startsWith("http://") ||
                property.coverImageUrl.startsWith("https://") -> {
            ImageRequest.Builder(context).data(property.coverImageUrl).crossfade(true).build()
        }
        property.resolvedDrawableName.isNotEmpty() -> {
            val resId = context.resources.getIdentifier(
                property.resolvedDrawableName, "drawable", context.packageName
            )
            if (resId != 0) resId else null
        }
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = primary.copy(0.08f), spotColor = primary.copy(0.12f)),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(primary, tertiary))))

        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(primary), contentAlignment = Alignment.Center) {
                if (imageModel != null) {
                    SubcomposeAsyncImage(
                        model              = imageModel,
                        contentDescription = property.title,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize().background(primary), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = tertiary, strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = tertiary, modifier = Modifier.size(32.dp))
                        }
                    )
                } else {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = tertiary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(property.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(property.address, fontSize = 12.sp, color = onSurface.copy(0.55f), maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color    = statusColor.copy(0.12f),
                    shape    = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, statusColor.copy(0.35f), RoundedCornerShape(6.dp))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(property.propertyStatusEnum.displayName(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }
            }

            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(primary.copy(0.06f))
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = onSurface, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier         = Modifier.background(surface).width(160.dp)
                ) {
                    // Approve — sub-admin bhi kar sakta hai
                    DropdownMenuItem(
                        leadingIcon = { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusApproved)) },
                        text        = { Text("Approve", color = StatusApproved, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        onClick     = { menuExpanded = false; onApprove() }
                    )
                    HorizontalDivider(color = onSurface.copy(0.07f), thickness = 0.5.dp)

                    // Reject — sub-admin bhi kar sakta hai
                    DropdownMenuItem(
                        leadingIcon = { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusPending)) },
                        text        = { Text("Reject", color = StatusPending, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        onClick     = { menuExpanded = false; onReject() }
                    )

                    // Delete — sirf Super Admin
                    if (canDelete) {
                        HorizontalDivider(color = onSurface.copy(0.07f), thickness = 0.5.dp)
                        DropdownMenuItem(
                            leadingIcon = { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusRejected)) },
                            text        = { Text("Delete", color = StatusRejected, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                            onClick     = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}