package com.example.havenhub.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.havenhub.utils.ValidationUtils

/**
 * Image picker components for HavenHub.
 *
 * Three variants:
 *   1. [SingleImagePicker]  – Single photo picker (avatar / cover image).
 *   2. [rememberCnicPicker] – Returns a lambda that opens the CNIC picker.
 *                             Validates MIME type; only JPG/PNG/WebP pass through.
 *                             The caller owns the button UI — no hidden overlay.
 *   3. [MultiImagePicker]   – Grid picker for up to [maxImages] property photos.
 */

// ── 1. Single Image Picker ────────────────────────────────────────────────────

/**
 * General-purpose single image picker (profile photo, cover image, etc.)
 *
 * @param imageUri      Currently selected image URI; null shows the placeholder.
 * @param onImagePicked Called with the URI the user chose.
 * @param label         Helper text shown below the picker.
 * @param isCircle      true → circular crop (avatar); false → square (cover).
 */
@Composable
fun SingleImagePicker(
    imageUri: Uri?,
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Tap to select image",
    isCircle: Boolean = false
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onImagePicked) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val shape = if (isCircle) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large

        Box(
            modifier = Modifier
                .size(if (isCircle) 100.dp else 180.dp)
                .clip(shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                .clickable(role = Role.Button) {
                    launcher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder icon + label when nothing is selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Add Photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── 2. CNIC Image Picker ──────────────────────────────────────────────────────

/**
 * Returns a lambda that, when invoked, opens the system image picker for CNIC photos.
 *
 * WHY THIS DESIGN:
 *   The previous implementation rendered a hidden 0.dp composable containing the
 *   real launcher, while showing a separate OutlinedButton whose onClick did nothing.
 *   As a result, tapping the button never opened the picker.
 *
 *   Fix: expose the launcher as a plain Kotlin lambda via this @Composable function.
 *   SignUpScreen calls openCnicPicker() directly inside the button's onClick.
 *   One button → one launcher → no invisible overlay.
 *
 * MIME VALIDATION:
 *   Only image/jpeg, image/png, and image/webp are accepted.
 *   The check uses ContentResolver.getType() which reads the actual file header —
 *   renaming a PDF to "cnic.jpg" will NOT bypass the check.
 *
 * Usage in SignUpScreen:
 *   val openCnicPicker = rememberCnicPicker(
 *       onImagePicked = { uri -> viewModel.onCnicImageSelected(uri) },
 *       onInvalidFile = { cnicFileTypeError = true }
 *   )
 *   ...
 *   Button(onClick = { openCnicPicker() }) { Text("Upload CNIC") }
 *
 * @param onImagePicked  Called only when the selected file passes MIME validation.
 * @param onInvalidFile  Called when the user picks a non-image file (PDF, DOC, etc.).
 * @return               A lambda — invoke it to open the system photo picker.
 */
@Composable
fun rememberCnicPicker(
    onImagePicked: (Uri) -> Unit,
    onInvalidFile: () -> Unit,
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // User cancelled the picker — do nothing
        if (uri == null) return@rememberLauncherForActivityResult

        // Resolve MIME type from ContentResolver (reads the actual file header).
        // Do NOT use the file extension — it can be faked (e.g. a .pdf renamed to .jpg).
        val mimeType = context.contentResolver.getType(uri)

        if (ValidationUtils.isValidImageMimeType(mimeType)) {
            onImagePicked(uri)  // Valid CNIC image — forward to ViewModel
        } else {
            onInvalidFile()     // Not a real image — caller shows an error
        }
    }

    // Return a stable lambda that restricts the picker to images only
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

// ── 3. Multi-Image Picker ─────────────────────────────────────────────────────

/**
 * Grid-based picker for selecting multiple property photos.
 *
 * @param images          Current list of selected URIs.
 * @param onImagesChanged Called with the updated list after any add or remove.
 * @param maxImages       Upper limit for how many photos can be selected (default 10).
 */
@Composable
fun MultiImagePicker(
    images: List<Uri>,
    onImagesChanged: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    maxImages: Int = 10
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Merge new picks with existing ones; deduplicate and respect the cap
            val combined = (images + uris).distinctBy { it.toString() }.take(maxImages)
            onImagesChanged(combined)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Header row: current count + "Add more" link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Photos (${images.size}/$maxImages)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (images.size < maxImages) {
                Text(
                    text = "Add more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        launcher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
        }

        // 3-column image grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(images) { uri ->
                ImageGridItem(
                    uri = uri,
                    onRemove = { onImagesChanged(images - uri) }
                )
            }

            // "+" add button — only shown when under the limit
            if (images.size < maxImages) {
                item {
                    AddImageCell {
                        launcher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── Private helper composables ────────────────────────────────────────────────

/** Single grid cell showing the selected image with a remove (×) button. */
@Composable
private fun ImageGridItem(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // × remove button anchored to the top-right corner
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(2.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

/** Grid cell with a "+" icon — tapping it opens the image picker. */
@Composable
private fun AddImageCell(onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Add",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}