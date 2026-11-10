package com.luckyprospector.app.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Handwriting for polaroid captions. Android's own "casual" family, so nothing is
 * downloaded or bundled; devices without it fall back to the default font.
 */
val HandwritingFont: FontFamily = FontFamily(Typeface.create("casual", Typeface.NORMAL))

private val PolaroidPaper = Color(0xFFFBFAF5)
private val PolaroidInk = Color(0xFF26302A)

/**
 * A finding as an instant photo: white frame, wider at the bottom where the caption
 * is written by hand, slightly tilted like a print laid on a table. The paper stays
 * white in dark mode too, as a real print would.
 *
 * [photoAspect] null lets the photo keep its own proportions (used where the user
 * must see exactly the image that will be saved).
 */
@Composable
fun Polaroid(
    modifier: Modifier = Modifier,
    caption: String? = null,
    subtitle: String? = null,
    tilt: Float = 0f,
    photoAspect: Float? = 1f,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    photo: @Composable (Modifier) -> Unit,
) {
    Surface(
        modifier = modifier.rotate(tilt),
        shape = RoundedCornerShape(3.dp),
        color = PolaroidPaper,
        shadowElevation = 10.dp,
    ) {
        Column(
            Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .then(if (photoAspect != null) Modifier.aspectRatio(photoAspect) else Modifier)
                    .clip(RoundedCornerShape(1.dp))
                    .glassSlideSheen(),
            ) {
                photo(Modifier.fillMaxWidth().then(if (photoAspect != null) Modifier.aspectRatio(photoAspect) else Modifier))
                badge?.let { Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { it() } }
            }
            caption?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = HandwritingFont,
                    fontSize = 22.sp,
                    color = PolaroidInk,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            subtitle?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = HandwritingFont,
                    fontSize = 15.sp,
                    color = PolaroidInk.copy(alpha = .65f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Alternating small tilts, so a list of prints looks laid by hand. */
fun polaroidTilt(index: Int): Float = listOf(-1.6f, 1.2f, -0.8f, 1.7f, -1.2f)[index % 5]
