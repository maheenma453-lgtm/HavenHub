package com.example.havenhub.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
 *  1. [NotificationBadge]       – Standalone numeric badge (for icons, cards)
 *  2. [NotificationIconButton]  – Bell icon with animated badge — use in TopAppBar
 *  3. [DotBadge]                – Small unread dot (no count)
 *  4. [BadgedIconSlot]          – Wraps any composable with a dot badge
 *
 * FIX: Badge was only showing on Admin dashboard because Tenant/Landlord
 * screens were not calling startListening(userId) on their ViewModels.
 * Badge component itself is correct — the fix is in NotificationViewModel
 * (startListening guard) and in each dashboard screen's LaunchedEffect.
 *
 * HOW TO USE in every dashboard TopAppBar:
 * ─────────────────────────────────────────
 *   val notifState by notificationViewModel.uiState.collectAsStateWithLifecycle()
 *
 *   LaunchedEffect(currentUserId) {
 *       notificationViewModel.startListening(currentUserId)   // ← THIS was missing
 *   }
 *
 *   NotificationIconButton(
 *       count   = notifState.unreadCount,
 *       onClick = { navController.navigate(Screen.Notifications.route) }
 *   )
 */

// ─── 1. Standalone Badge ──────────────────────────────────────────────────────

/**
 * Circular badge showing a count. Hidden when count <= 0.
 *
 * @param count      Number to display (0 = hidden)
 * @param modifier   Positioning modifier (typically .align(Alignment.TopEnd))
 * @param size       Badge diameter
 * @param badgeColor Background color (defaults to MaterialTheme error color)
 */
@Composable
fun NotificationBadge(
    count     : Int,
    modifier  : Modifier = Modifier,
    size      : Dp       = 18.dp,
    badgeColor: Color    = MaterialTheme.colorScheme.error
) {
    AnimatedVisibility(
        visible = count > 0,
        enter   = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit    = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        modifier = modifier
    ) {
        Surface(
            shape    = MaterialTheme.shapes.extraLarge,
            color    = badgeColor,
            modifier = Modifier.size(if (count > 9) 22.dp else size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text       = if (count > 99) "99+" else count.toString(),
                    color      = Color.White,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

// ─── 2. Notification Icon Button ─────────────────────────────────────────────

/**
 * Bell icon button with animated count badge.
 * Place in TopAppBar actions on Admin, Landlord, AND Tenant dashboards.
 *
 * @param count   Unread notification count (0 hides the badge)
 * @param onClick Button tap callback — navigate to NotificationsScreen
 * @param tint    Icon tint (auto-adapts to light/dark theme via onSurface)
 */
@Composable
fun NotificationIconButton(
    count   : Int,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    tint    : Color    = MaterialTheme.colorScheme.onSurface
) {
    BadgedBox(
        badge = {
            AnimatedVisibility(
                visible = count > 0,
                enter   = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit    = scaleOut(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = Color.White
                ) {
                    Text(
                        text       = if (count > 99) "99+" else count.toString(),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = modifier
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector     = Icons.Default.Notifications,
                contentDescription = if (count > 0)
                    "$count unread notifications"
                else
                    "Notifications",
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// ─── 3. Dot Badge ─────────────────────────────────────────────────────────────

/**
 * Small unread indicator dot — no count shown.
 * Use on tab icons and list items to signal new content.
 */
@Composable
fun DotBadge(
    isVisible: Boolean,
    modifier : Modifier = Modifier,
    dotSize  : Dp      = 8.dp,
    dotColor : Color   = MaterialTheme.colorScheme.error
) {
    AnimatedVisibility(
        visible  = isVisible,
        enter    = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
        exit     = scaleOut(spring(stiffness = Spring.StiffnessMediumLow)),
        modifier = modifier
    ) {
        Surface(
            shape    = MaterialTheme.shapes.extraLarge,
            color    = dotColor,
            modifier = Modifier.size(dotSize)
        ) {}
    }
}

// ─── 4. Badged Icon Slot ──────────────────────────────────────────────────────

/**
 * Wraps any composable with a [DotBadge] in the top-end corner.
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
    showDot : Boolean,
    modifier: Modifier = Modifier,
    content : @Composable () -> Unit
) {
    BadgedBox(
        badge = {
            AnimatedVisibility(
                visible = showDot,
                enter   = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit    = scaleOut(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Badge(containerColor = MaterialTheme.colorScheme.error)
            }
        },
        modifier = modifier
    ) {
        content()
    }
}



















