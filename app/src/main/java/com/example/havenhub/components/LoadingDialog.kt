package com.example.havenhub.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Loading UI components for HavenHub.
 *
 * Three variants:
 *  1. [LoadingDialog]       – Modal dialog with scrim, blocks interaction
 *  2. [LoadingOverlay]      – Full-screen overlay on top of existing content
 *  3. [LoadingIndicator]    – Lightweight inline spinner for composable slots
 */

// ─── 1. Modal Loading Dialog ──────────────────────────────────────────────────

/**
 * Shows a centered modal dialog with a spinner and optional message.
 * Dismissal is disabled while loading is active.
 *
 * @param isVisible Whether the dialog is shown
 * @param message   Optional label shown below the spinner
 */
@Composable
fun LoadingDialog(
    isVisible: Boolean,
    message: String = "Please wait..."
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = { /* non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─── 2. Full-Screen Overlay ───────────────────────────────────────────────────

/**
 * Transparent full-screen overlay that blocks touches during async operations.
 * Typically placed as the last child in a [Box] composable.
 *
 * @param isVisible Whether the overlay is shown
 * @param dimBackground Whether to dim the background (default: true)
 */
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    dimBackground: Boolean = true
) {
    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (dimBackground) Modifier.background(Color.Black.copy(alpha = 0.4f))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(24.dp)
                    .size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

// ─── 3. Inline Spinner ────────────────────────────────────────────────────────

/**
 * Lightweight centered spinner for use inside lazy lists or content slots.
 *
 * @param modifier Optional modifier for sizing/positioning
 * @param label    Optional label shown below the spinner
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


