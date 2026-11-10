package com.luckyprospector.app.camera

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val color: Color,
)

private val CelebrationGreens = listOf(Color(0xFF2E9E4F), Color(0xFF45B864), Color(0xFF1F7A3D), Color(0xFF7DCE82))
private val CelebrationGold = Color(0xFFFFC928)

/**
 * A burst of four-leaf clovers from [origin] (normalised screen position), played
 * once per [trigger] value. Only a confirmed four-leaf clover triggers it, so it
 * never celebrates something the tracker has not vouched for.
 */
@Composable
fun CloverCelebration(trigger: Int, origin: Offset, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        val random = Random(trigger)
        List(PARTICLES) {
            Particle(
                angle = (random.nextFloat() * 2 * PI).toFloat(),
                speed = .45f + random.nextFloat() * .75f,
                size = 9f + random.nextFloat() * 13f,
                spin = (random.nextFloat() - .5f) * 720f,
                color = if (it % 7 == 0) CelebrationGold else CelebrationGreens[it % CelebrationGreens.size],
            )
        }
    }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
    }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val start = Offset(origin.x * size.width, origin.y * size.height)
        val reach = size.minDimension * .75f
        val alpha = if (t < .7f) 1f else 1f - (t - .7f) / .3f
        // A soft ring where the clover was found, fading as the burst spreads.
        drawCircle(
            CelebrationGold.copy(alpha = (1f - t) * .6f),
            radius = reach * .25f * t + 20f,
            center = start,
            style = Stroke(width = 8f * (1f - t)),
        )
        particles.forEach { particle ->
            val distance = reach * particle.speed * (1f - (1f - t) * (1f - t))
            val fall = size.height * .25f * t * t
            val center = Offset(
                start.x + cos(particle.angle) * distance,
                start.y + sin(particle.angle) * distance + fall,
            )
            val leaf = particle.size * density / 2.6f * (1f + .4f * t)
            rotate(particle.spin * t, pivot = center) {
                drawFourLeafClover(center, leaf, particle.color.copy(alpha = alpha))
            }
        }
    }
}

private fun DrawScope.drawFourLeafClover(center: Offset, leaf: Float, color: Color) {
    val offset = leaf * .72f
    listOf(Offset(0f, -offset), Offset(offset, 0f), Offset(0f, offset), Offset(-offset, 0f)).forEach {
        drawCircle(color, radius = leaf, center = center + it)
    }
    drawCircle(Color.White.copy(alpha = color.alpha * .35f), radius = leaf * .35f, center = center)
}

/**
 * A short rising chime synthesised on the device: an arpeggio of soft bell-like
 * tones. No audio asset to ship or license. Plays at media volume, so the phone's
 * own volume and silent settings apply.
 */
object LuckyChime {
    private const val SAMPLE_RATE = 44_100
    private val NOTES = floatArrayOf(1046.5f, 1318.5f, 1568f, 2093f) // C6 E6 G6 C7
    private const val NOTE_GAP_S = .085f
    private const val NOTE_LENGTH_S = .55f

    private val pcm: ShortArray by lazy {
        val total = ((NOTE_GAP_S * (NOTES.size - 1) + NOTE_LENGTH_S) * SAMPLE_RATE).toInt()
        val mix = FloatArray(total)
        NOTES.forEachIndexed { index, frequency ->
            val start = (index * NOTE_GAP_S * SAMPLE_RATE).toInt()
            val length = (NOTE_LENGTH_S * SAMPLE_RATE).toInt()
            for (i in 0 until length) {
                if (start + i >= total) break
                val time = i.toFloat() / SAMPLE_RATE
                val attack = (time / .006f).coerceAtMost(1f)
                val envelope = attack * exp(-time * 7f)
                // A touch of the octave gives the tone a bell-like shimmer.
                val tone = sin(2 * PI * frequency * time) + .3 * sin(2 * PI * frequency * 2 * time)
                mix[start + i] += (tone * envelope * .22).toFloat()
            }
        }
        ShortArray(total) { (mix[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
    }

    suspend fun play() = withContext(Dispatchers.Default) {
        runCatching {
            val samples = pcm
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            kotlinx.coroutines.delay((samples.size * 1000L / SAMPLE_RATE) + 100)
            track.release()
        }
    }
}

private const val PARTICLES = 34
private const val DURATION_MS = 1_700
