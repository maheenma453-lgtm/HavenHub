package com.example.havenhub.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dashboard card variants for HavenHub:
 *
 *  1. [DashboardCard]       – Stat card with icon, value, label, optional trend
 *  2. [DashboardActionCard] – Tappable card with icon, title, subtitle
 *  3. [DashboardSummaryCard]– Wide card with multiple inline metrics
 */

// ─── 1. Stat Card ─────────────────────────────────────────────────────────────

/**
 * @param title       Metric label (e.g. "Total Bookings")
 * @param value       Formatted metric value (e.g. "1,240")
 * @param icon        Icon representing the metric
 * @param trend       Optional trend string ("+" prefix = up, "-" prefix = down, else flat)
 * @param trendLabel  Contextual label for the trend (e.g. "vs last month")
 * @param iconTint    Custom tint for the icon container
 */
@Composable
fun DashboardCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendLabel: String = "vs last month",
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon + Trend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (trend != null) {
                    TrendBadge(trend = trend)
                }
            }

            // Value
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Title + trend label
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (trend != null) {
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// ─── 2. Action Card ───────────────────────────────────────────────────────────

/**
 * Tappable card used in Quick Actions grids on dashboards.
 *
 * @param title     Action label
 * @param subtitle  Short description or count
 * @param icon      Action icon
 * @param onClick   Tap callback
 * @param badge     Optional numeric badge (e.g. pending count)
 */
@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (badge != null && badge > 0) {
                    NotificationBadge(
                        count = badge,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── 3. Summary Card ──────────────────────────────────────────────────────────

/**
 * Wide card displaying multiple metrics in a single row.
 * Used for booking/revenue summaries.
 *
 * @param title   Card section title
 * @param metrics List of [MetricItem] to display horizontally
 */
data class MetricItem(val label: String, val value: String, val color: Color? = null)

@Composable
fun DashboardSummaryCard(
    title: String,
    metrics: List<MetricItem>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                metrics.forEach { metric ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = metric.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = metric.color ?: MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = metric.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun TrendBadge(trend: String) {
    val isUp = trend.startsWith("+")
    val isDown = trend.startsWith("-")

    val trendColor = when {
        isUp -> Color(0xFF43A047)
        isDown -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = when {
                isUp -> Icons.Default.TrendingUp
                isDown -> Icons.Default.TrendingDown
                else -> Icons.Default.TrendingFlat
            },
            contentDescription = null,
            tint = trendColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = trend,
            style = MaterialTheme.typography.labelSmall,
            color = trendColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

