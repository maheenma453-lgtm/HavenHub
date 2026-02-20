package com.example.havenhub.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A stats card displayed in the Admin Dashboard overview row.
 *
 * @param title  Label for the metric (e.g. "Total Users")
 * @param value  Formatted value string (e.g. "1,240")
 * @param icon   Leading icon representing the metric
 * @param trend  Optional trend string like "+12%" or "-3%"; positive values shown in green, negative in red
 */
@Composable
fun DashboardStatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    trend: String? = null,
    modifier: Modifier = Modifier
) {
    val isPositiveTrend = trend?.startsWith("+") == true

    ElevatedCard(
        modifier = modifier
            .width(160.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                if (trend != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = if (isPositiveTrend) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = "Trend",
                            tint = if (isPositiveTrend) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = trend,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPositiveTrend) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Value
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

