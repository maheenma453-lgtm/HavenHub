package com.example.havenhub.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

private val StarColor = Color(0xFFFFC107)

/**
 * Read-only star rating display.
 *
 * Supports half-star precision. Shows filled, half, and empty stars
 * based on [rating].
 *
 * @param rating      Value between 0.0 and [maxStars]
 * @param maxStars    Total number of stars (default 5)
 * @param starSize    Size of each star icon
 * @param showLabel   When true, appends the numeric rating next to the stars
 */
@Composable
fun RatingBar(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 18.dp,
    showLabel: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..maxStars) {
            val starType = when {
                i <= floor(rating)               -> StarType.Full
                i == floor(rating).toInt() + 1
                        && (rating % 1) >= 0.5f  -> StarType.Half
                else                              -> StarType.Empty
            }
            StarIcon(type = starType, size = starSize)
        }

        if (showLabel) {
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

/**
 * Interactive star rating input.
 *
 * @param rating       Current selected rating (1 – [maxStars])
 * @param onRatingChange Callback with the tapped star index
 * @param maxStars     Total stars
 * @param starSize     Size of each star icon
 * @param enabled      When false, disables tap interaction
 */
@Composable
fun RatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 28.dp,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = "Rate $i star${if (i > 1) "s" else ""}",
                tint = if (i <= rating) StarColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (enabled) Modifier.clickable(role = Role.Button) { onRatingChange(i) }
                        else Modifier
                    )
            )
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

private enum class StarType { Full, Half, Empty }

@Composable
private fun StarIcon(type: StarType, size: Dp) {
    Icon(
        imageVector = when (type) {
            StarType.Full  -> Icons.Default.Star
            StarType.Half  -> Icons.Default.StarHalf
            StarType.Empty -> Icons.Outlined.StarOutline
        },
        contentDescription = null,
        tint = when (type) {
            StarType.Empty -> MaterialTheme.colorScheme.outline
            else           -> StarColor
        },
        modifier = Modifier.size(size)
    )
}

