package dev.randyapps.lapse.ui.edit

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Optional photo of the item. Uses the system Photo Picker, so the app needs no media
 * permission at all — the user grants access to exactly the one image they choose.
 */
@Composable
fun PhotoField(
    photoPath: String?,
    saving: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FieldLabel(stringResource(R.string.label_photo))

        if (photoPath != null) {
            PhotoThumbnail(path = photoPath)
        }

        Row(
            modifier = Modifier.padding(top = if (photoPath != null) 12.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionChip(
                onClick = onPick,
                enabled = !saving,
                label = {
                    Text(
                        stringResource(
                            if (photoPath == null) R.string.photo_add else R.string.photo_replace
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
            if (photoPath != null) {
                SuggestionChip(
                    onClick = onRemove,
                    label = {
                        Text(
                            stringResource(R.string.photo_remove),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        Text(
            text = stringResource(R.string.photo_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Decoded off the main thread and keyed by path, so replacing a photo reloads it.
 *
 * Decoding directly rather than pulling in an image-loading library keeps the app free of any
 * dependency capable of fetching over the network.
 */
@Composable
private fun PhotoThumbnail(path: String) {
    val description = stringResource(R.string.cd_item_photo)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                // Subsampled again on read: the stored file is already small, but this keeps a
                // thumbnail off the heap at full size.
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(path, options)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .semantics { contentDescription = description },
            )
        }
    }
}
