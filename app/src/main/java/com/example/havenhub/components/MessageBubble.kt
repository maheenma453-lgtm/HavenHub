package com.example.havenhub.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Chat message bubble for the HavenHub messaging screen.
 *
 * Renders sent messages on the right (primary color) and
 * received messages on the left (surface variant).
 *
 * @param message      Text content of the message
 * @param timestamp    Formatted time string (e.g. "10:45 AM")
 * @param isSentByMe   True → right-aligned sender bubble, False → left-aligned receiver
 * @param isRead       Shows double-tick read receipt for sent messages
 * @param isPending    Shows a clock icon when message is not yet delivered
 * @param senderName   Optional name shown above received bubbles (for group-style display)
 */
@Composable
fun MessageBubble(
    message: String,
    timestamp: String,
    isSentByMe: Boolean,
    modifier: Modifier = Modifier,
    isRead: Boolean = false,
    isPending: Boolean = false,
    senderName: String? = null
) {
    val configuration = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.72).dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isSentByMe) Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Optional sender name (received messages only)
            if (!isSentByMe && senderName != null) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Bubble
            Surface(
                shape = when {
                    isSentByMe -> MaterialTheme.shapes.large.copy(
                        bottomEnd = androidx.compose.foundation.shape.CornerSize(4.dp)
                    )
                    else -> MaterialTheme.shapes.large.copy(
                        bottomStart = androidx.compose.foundation.shape.CornerSize(4.dp)
                    )
                },
                color = if (isSentByMe)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isSentByMe) 0.dp else 2.dp,
                modifier = Modifier.widthIn(min = 80.dp, max = maxBubbleWidth)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Message text
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSentByMe)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Timestamp + status row
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSentByMe)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )

                        // Read receipt / pending for sent messages
                        if (isSentByMe) {
                            when {
                                isPending -> Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Pending",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                                isRead -> Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF80DEEA) // Cyan tint on primary bg
                                )
                                else -> Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
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

/**
 * Inline date divider shown between messages from different days.
 *
 * @param date Formatted date string (e.g. "Today", "Yesterday", "Jan 20")
 */
@Composable
fun MessageDateDivider(date: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

