package com.example.havenhub.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── HavenHub Brand Colors ─────────────────────────────────────────────────────
private val HavenNavy      = Color(0xFF1A2744)
private val HavenNavyLight = Color(0xFF2D3F6B)
private val HavenGold      = Color(0xFFC9973A)
private val HavenGoldLight = Color(0xFFE8B84B)
private val HavenGoldDark  = Color(0xFFA87C2A)

/**
 * Chat message bubble — HavenHub branded.
 *
 * ✦ ROLE-BASED + SIDE-BASED COLORS:
 *
 *   isSentByMe = true  → RIGHT side bubble (mera message)
 *     • Landlord send kare → NAVY gradient  (HavenNavy → HavenNavyLight)
 *     • Tenant send kare   → NAVY gradient  (same — mera apna sent)
 *
 *   isSentByMe = false → LEFT side bubble (dusre ka message — RECEIVED)
 *     ✦ Received bubble HAMESHA GOLDEN hoga — role se qata nazar
 *     • Golden gradient (HavenGoldDark → HavenGold)
 *     • Text white hoga received bubble mein bhi (gold pe white readable hai)
 *
 *   Yani:
 *     → Mera message     = RIGHT, NAVY bubble
 *     → Dusre ka message = LEFT,  GOLDEN bubble  ✦ (aapki requirement)
 *
 * ✦ Selection mode: long press → select, tap → toggle
 * ✦ Date dividers: use [MessageDateDivider] between date groups
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message        : String,
    timestamp      : String,
    isSentByMe     : Boolean,
    modifier       : Modifier = Modifier,
    isRead         : Boolean  = false,
    isPending      : Boolean  = false,
    senderName     : String?  = null,
    senderRole     : String?  = null,   // "landlord" ya "tenant" — optional badge ke liye
    isSelected     : Boolean  = false,
    isSelectionMode: Boolean  = false,
    onLongPress    : () -> Unit = {},
    onTap          : () -> Unit = {}
) {
    val configuration  = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.72).dp

    val isLandlord = senderRole?.lowercase()?.trim() == "landlord"

    // ✦ SENT bubble (isSentByMe = true → RIGHT side) → NAVY
    val sentBubbleBrush = when {
        isSelected -> Brush.linearGradient(
            listOf(HavenNavy.copy(0.65f), HavenNavyLight.copy(0.65f))
        )
        else       -> Brush.linearGradient(
            listOf(HavenNavy, HavenNavyLight)
        )
    }

    // ✦ RECEIVED bubble (isSentByMe = false → LEFT side) → GOLDEN
    // Aapki requirement: received bubble golden honi chahiye
    val receivedBubbleBrush = when {
        isSelected -> Brush.linearGradient(
            listOf(HavenGoldDark.copy(0.60f), HavenGold.copy(0.60f))
        )
        else       -> Brush.linearGradient(
            listOf(HavenGoldDark, HavenGold)
        )
    }

    val rowBgColor by animateColorAsState(
        targetValue   = if (isSelected) HavenGold.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label         = "selectionHighlight"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBgColor)
            .combinedClickable(onClick = { onTap() }, onLongClick = { onLongPress() })
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection checkbox
        if (isSelectionMode) {
            Icon(
                imageVector        = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint               = if (isSelected) HavenGold
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier           = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(4.dp))
        }

        Row(
            modifier              = Modifier.weight(1f),
            horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start,
            verticalAlignment     = Alignment.Bottom
        ) {
            if (isSentByMe) Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Sender name + role badge — only for received messages
                if (!isSentByMe && senderName != null) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text       = senderName,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            // ✦ Name color: Gold text on white background (above bubble)
                            color      = HavenGoldDark
                        )
                        // Role badge (optional)
                        if (senderRole != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = HavenGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text       = if (isLandlord) "Landlord" else "Tenant",
                                    fontSize   = 9.sp,
                                    color      = HavenGoldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── Bubble shape ──────────────────────────────────────────────
                val bubbleShape = if (isSentByMe)
                    RoundedCornerShape(
                        topStart    = 16.dp,
                        topEnd      = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd   = 4.dp
                    )
                else
                    RoundedCornerShape(
                        topStart    = 4.dp,
                        topEnd      = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd   = 16.dp
                    )

                Box(
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = maxBubbleWidth)
                        .background(
                            brush = if (isSentByMe) sentBubbleBrush else receivedBubbleBrush,
                            shape = bubbleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            // ✦ Both sent (navy) and received (gold) bubbles → white text
                            color      = Color.White,
                            lineHeight = 20.sp
                        )

                        Row(
                            modifier              = Modifier
                                .align(Alignment.End)
                                .padding(top = 5.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text     = timestamp,
                                style    = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                // ✦ Both sides → white timestamp (readable on navy + gold)
                                color    = Color.White.copy(alpha = 0.70f)
                            )

                            // Read receipts — only for sent messages
                            if (isSentByMe) {
                                when {
                                    isPending -> Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Pending",
                                        modifier           = Modifier.size(12.dp),
                                        tint               = Color.White.copy(alpha = 0.65f)
                                    )
                                    isRead    -> Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        modifier           = Modifier.size(12.dp),
                                        // ✦ Sent bubble is NAVY → gold tick shows nicely
                                        tint               = HavenGoldLight
                                    )
                                    else      -> Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = "Delivered",
                                        modifier           = Modifier.size(12.dp),
                                        tint               = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isSentByMe) Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * Date divider shown between messages of different days.
 * e.g. "Today", "Yesterday", "May 12"
 * HavenHub Gold accent style.
 */
@Composable
fun MessageDateDivider(date: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = HavenGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = HavenGold,
                fontSize = 11.sp
            )
        }
    }
}