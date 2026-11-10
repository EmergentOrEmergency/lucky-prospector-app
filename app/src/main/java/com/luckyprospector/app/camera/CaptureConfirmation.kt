package com.luckyprospector.app.camera

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.R
import com.luckyprospector.app.capture.CaptureFormState
import com.luckyprospector.app.domain.FindingType
import com.luckyprospector.app.sharing.decodeUpright
import com.luckyprospector.app.ui.components.Polaroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen confirmation of a capture.
 *
 * It replaced an AlertDialog that re-centred itself on every keystroke: the
 * keyboard, a growing text field and a photo loading in all changed its height.
 * Here every block has a fixed height and the content scrolls above the keyboard,
 * so nothing moves while the user types.
 */
@Composable
fun CaptureConfirmation(
    photo: Uri,
    state: CaptureFormState,
    saving: Boolean,
    onStateChange: (CaptureFormState) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = .97f)) {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.confirm_finding), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            // One fixed line of handwriting: the wish is not echoed here, or the print
            // would grow while typing and move the screen again.
            FixedPhotoPreview(
                photo,
                height = PHOTO_HEIGHT,
                caption = stringResource(if (state.type == FindingType.FOUR_LEAF_CLOVER) R.string.type_four_leaf_clover else R.string.type_patch_short),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.type == FindingType.FOUR_LEAF_CLOVER, { onStateChange(state.copy(type = FindingType.FOUR_LEAF_CLOVER)) }, { Text(stringResource(R.string.type_four_leaf_clover)) }, enabled = !saving)
                FilterChip(state.type == FindingType.CLOVER_PATCH, { onStateChange(state.copy(type = FindingType.CLOVER_PATCH)) }, { Text(stringResource(R.string.type_patch_short)) }, enabled = !saving)
            }
            OutlinedTextField(
                value = state.wish,
                onValueChange = { onStateChange(state.withWish(it)) },
                label = { Text(stringResource(R.string.wish_short_optional)) },
                supportingText = { Text("${state.wish.length}/${CaptureFormState.MAX_WISH_LENGTH}") },
                // A fixed number of lines: a field that grows while typing moved the whole screen.
                minLines = 3,
                maxLines = 3,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.location_private_note), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                OutlinedButton(onClick = onCancel, enabled = !saving) { Text(stringResource(R.string.cancel)) }
                Button(onClick = onConfirm, enabled = !saving) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.saving), Modifier.padding(start = 10.dp))
                    } else {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

/**
 * A photo in a polaroid of constant height whose width follows the photo, like the
 * prints in the findings list. Until the photo loads, camera proportions (3:4)
 * are assumed, so the frame does not jump when it appears.
 */
@Composable
fun FixedPhotoPreview(photo: Uri?, height: Dp, modifier: Modifier = Modifier, caption: String? = null) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, photo) {
        value = photo?.let { withContext(Dispatchers.IO) { decodeUpright(context, it, maxSide = 900)?.asImageBitmap() } }
    }
    val aspect = bitmap?.let { it.width.toFloat() / it.height } ?: (3f / 4f)
    Polaroid(
        modifier = modifier.width(height * aspect + POLAROID_SIDES),
        caption = caption,
        tilt = -1.2f,
        photoAspect = aspect,
    ) { photoModifier ->
        Box(photoModifier, contentAlignment = Alignment.Center) {
            val loaded = bitmap
            if (loaded != null) {
                Image(
                    loaded,
                    contentDescription = stringResource(R.string.captured_photo_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (photo != null) {
                CircularProgressIndicator()
            }
        }
    }
}

/** The polaroid's white border on the left and right (12 dp each). */
private val POLAROID_SIDES = 24.dp
private val PHOTO_HEIGHT = 240.dp
