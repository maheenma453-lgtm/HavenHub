package com.example.havenhub.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Notification badge components for HavenHub.
 *
 *  1. [NotificationBadge]          – Standalone numeric badge (for icons, cards)
 *  2. [NotificationIconButton]     – Bell icon with badge, used in TopAppBar
 *  3. [DotBadge]                   – Small unread dot (no count)
 */

// ─── 1. Standalone Badge ──────────────────────────────────────────────────────

/**
 * Circular badge showing a count. Clamps display to "99+" for large counts.
 *
 * @param count      Number to display (0 = hidden)
 * @param modifier   Positioning modifier (typically .align(Alignment.TopEnd))
 * @param size       Badge diameter
 * @param badgeColor Background color (defaults to error/red)
 */
@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    badgeColor: Color = MaterialTheme.colorScheme.error
) {
    if (count <= 0) return

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = badgeColor,
        modifier = modifier.size(if (count > 9) 22.dp else size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = MaterialTheme.colorScheme.onError,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

// ─── 2. Notification Icon Button ─────────────────────────────────────────────

/**
 * Bell icon button with a count badge.
 * Designed for use in TopAppBar actions.
 *
 * @param count       Unread notification count (0 hides the badge)
 * @param onClick     Button tap callback
 * @param tint        Icon tint color
 */
@Composable
fun NotificationIconButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = modifier
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = if (count > 0) "$count unread notifications" else "Notifications",
                tint = tint
            )
        }
    }
}

// ─── 3. Dot Badge ─────────────────────────────────────────────────────────────

/**
 * Small unread indicator dot with no count.
 * Used on tab icons and list items to indicate new content.
 *
 * @param isVisible   Controls dot visibility
 * @param dotSize     Size of the dot
 * @param dotColor    Dot color (defaults to error/red)
 */
@Composable
fun DotBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotColor: Color = MaterialTheme.colorScheme.error
) {
    if (!isVisible) return

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = dotColor,
        modifier = modifier.size(dotSize)
    ) {}
}

/**
 * Wraps any composable content with a [DotBadge] in the top-end corner.
 *
 * Usage:
 * ```
 * BadgedIconSlot(showDot = hasUnread) {
 *     Icon(Icons.Default.Message, contentDescription = "Messages")
 * }
 * ```
 */
@Composable
fun BadgedIconSlot(
    showDot: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BadgedBox(
        badge = {
            if (showDot) {
                Badge(containerColor = MaterialTheme.colorScheme.error)
            }
        },
        modifier = modifier
    ) {
        content()
    }
}

