package com.example.havenhub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.havenhub.R

@Composable
fun PropertyCard(
    imageUrl        : String,
    title           : String,
    location        : String,
    pricePerNight   : String,
    rating          : Float,
    reviewCount     : Int,
    isFavorited     : Boolean   = false,
    isVerified      : Boolean   = false,
    isPremium       : Boolean   = false,
    propertyType    : String    = "",
    onClick         : () -> Unit,
    onFavoriteToggle: () -> Unit = {},
    modifier        : Modifier  = Modifier
) {
    val context = LocalContext.current

    val imageModel: Any = when {
        imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
        imageUrl.isNotEmpty() -> {
            val resId = context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
            if (resId != 0) resId else R.drawable.havenhub
        }
        else -> R.drawable.havenhub
    }

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model              = ImageRequest.Builder(context).data(imageModel).crossfade(true).build(),
                    contentDescription = title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxWidth().height(200.dp)
                )

                IconButton(
                    onClick  = { onFavoriteToggle() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        imageVector        = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites",
                        tint               = if (isFavorited) Color(0xFFE53935) else Color.White
                    )
                }

                // Premium badge — top start, above Verified
                if (isPremium) {
                    Surface(
                        color    = Color(0xFFD4AF37),
                        shape    = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint               = Color(0xFF1A2744),
                                modifier           = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text       = "Premium",
                                style      = MaterialTheme.typography.labelSmall,
                                color      = Color(0xFF1A2744),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 10.sp
                            )
                        }
                    }
                }

                // Verified badge — shown below Premium if both active
                if (isVerified) {
                    Surface(
                        color    = MaterialTheme.colorScheme.primaryContainer,
                        shape    = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = 8.dp,
                                top   = if (isPremium) 38.dp else 8.dp
                            )
                    ) {
                        Text(
                            text       = "✓ Verified",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (propertyType.isNotEmpty()) {
                    Surface(
                        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        shape    = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    ) {
                        Text(
                            text     = propertyType,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier            = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text     = location,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "$pricePerNight / night",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star, null,
                            tint     = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text       = String.format("%.1f", rating),
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text  = " ($reviewCount)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}