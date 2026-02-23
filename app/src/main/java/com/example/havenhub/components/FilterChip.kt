package com.example.havenhub.components
import  androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable filter chip for category/tag selection in HavenHub.
 *
 * @param label      Display text
 * @param selected   Whether this chip is currently active
 * @param onClick    Selection callback
 * @param leadingIcon Optional icon shown before the label
 * @param modifier   Optional modifier
 */
@Composable
fun HavenFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        leadingIcon = when {
            selected -> {
                { Icon(Icons.Default.Check, contentDescription = null) }
            }
            leadingIcon != null -> {
                { Icon(leadingIcon, contentDescription = null) }
            }
            else -> null
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}

/**
 * Horizontally scrollable row of filter chips.
 * Used in Search, Home, and filter screens.
 *
 * @param options        List of option labels
 * @param selectedOption Currently selected option
 * @param onOptionSelect Callback with the selected label
 * @param icons          Optional map of label → icon
 */
@Composable
fun FilterChipRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    icons: Map<String, ImageVector> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            HavenFilterChip(
                label = option,
                selected = selectedOption == option,
                onClick = { onOptionSelect(option) },
                leadingIcon = icons[option]
            )
        }
    }
}

/**
 * Multi-select filter chip row — multiple options can be active at once.
 *
 * @param options          List of option labels
 * @param selectedOptions  Set of currently active options
 * @param onToggle         Callback with toggled label
 */
@Composable
fun MultiSelectFilterChipRow(
    options: List<String>,
    selectedOptions: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            HavenFilterChip(
                label = option,
                selected = option in selectedOptions,
                onClick = { onToggle(option) }
            )
        }
    }
}

