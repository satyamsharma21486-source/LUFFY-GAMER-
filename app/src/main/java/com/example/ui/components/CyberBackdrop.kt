package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DeepSpaceBg
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FloatingParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radiusPx: Float,
    val speedFactor: Float,
    val phaseOffset: Float,
    val isCyan: Boolean,
    val alpha: Float
)

@Composable
fun CyberBackdrop(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_backdrop")

    val orbPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )

    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_drift"
    )

    val particles = remember {
        List(26) { idx ->
            val seed = (idx * 37 + 13) % 100
            val seedY = (idx * 59 + 7) % 100
            FloatingParticle(
                xFraction = seed / 100f,
                yFraction = seedY / 100f,
                radiusPx = 2.2f + (idx % 4) * 1.4f,
                speedFactor = 0.6f + (idx % 3) * 0.35f,
                phaseOffset = idx * 0.65f,
                isCyan = idx % 3 != 0,
                alpha = 0.22f + (idx % 4) * 0.10f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left cyan glowing orb
            val cyanCenter = Offset(
                x = w * (0.18f + 0.10f * cos(orbPhase)),
                y = h * (0.16f + 0.08f * sin(orbPhase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.20f),
                        NeonCyan.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = cyanCenter
            )

            // Bottom-right crimson glowing orb
            val crimsonCenter = Offset(
                x = w * (0.82f + 0.09f * sin(orbPhase + 1.2f)),
                y = h * (0.72f + 0.10f * cos(orbPhase + 0.7f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCrimson.copy(alpha = 0.18f),
                        NeonCrimson.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = crimsonCenter,
                    radius = w * 0.68f
                ),
                radius = w * 0.68f,
                center = crimsonCenter
            )

            // Mid-violet ambient orb
            val violetCenter = Offset(
                x = w * (0.50f + 0.14f * cos(orbPhase * 0.7f + 2.1f)),
                y = h * (0.44f + 0.09f * sin(orbPhase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonViolet.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = violetCenter
            )

            // Floating cyber particles
            particles.forEach { p ->
                val yProgress = (p.yFraction - particleDrift * p.speedFactor + 1f) % 1f
                val swayX = sin(orbPhase + p.phaseOffset) * 18f
                val cx = (p.xFraction * w + swayX).coerceIn(0f, w)
                val cy = yProgress * h
                val baseColor = if (p.isCyan) NeonCyan else NeonCrimson

                drawCircle(
                    color = baseColor.copy(alpha = p.alpha),
                    radius = p.radiusPx,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
