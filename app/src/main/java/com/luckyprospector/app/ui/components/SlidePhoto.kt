package com.luckyprospector.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * The light of a glass microscope slide over a photo: a soft sheen from the top
 * left corner and a thin diagonal glint. Drawn, not an image asset, so it scales
 * to any size and costs nothing to ship.
 */
fun Modifier.glassSlideSheen(): Modifier = drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    drawRect(
        Brush.linearGradient(
            0f to Color.White.copy(alpha = .38f),
            .32f to Color.White.copy(alpha = .10f),
            .55f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(w, h),
        ),
    )
    drawRect(
        Brush.linearGradient(
            .40f to Color.Transparent,
            .47f to Color.White.copy(alpha = .22f),
            .50f to Color.White.copy(alpha = .06f),
            .56f to Color.Transparent,
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
    )
}

/** A square photo from JPEG bytes, as a polaroid print. */
@Composable
fun SlidePhoto(jpeg: ByteArray, modifier: Modifier = Modifier, caption: String? = null) {
    val bitmap = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap() }
    Polaroid(modifier = modifier, caption = caption, tilt = 1.2f) { photoModifier ->
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = photoModifier) }
            ?: Box(photoModifier)
    }
}
