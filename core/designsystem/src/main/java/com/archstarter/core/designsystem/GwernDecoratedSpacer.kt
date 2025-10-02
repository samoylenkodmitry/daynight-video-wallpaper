package com.archstarter.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Immutable
data class GwernDecoratedSpacerColors(
    val accent: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val border: Color,
    val stripe: Color,
    val dotHighlight: Color,
)

object GwernDecoratedSpacerDefaults {
    val AccentColor: Color = Color(0xFFAD8A4A)
    val DotHighlightColor: Color = Color(0xFFF9F7F1)

    fun colors(
        accent: Color = AccentColor,
        backgroundTop: Color = Color(0xFFF7F5EF),
        backgroundBottom: Color = Color(0xFFF0EFEA),
        border: Color = Color(0xFF9C9C9C),
        stripe: Color = accent.copy(alpha = 0.18f),
        dotHighlight: Color = DotHighlightColor,
    ): GwernDecoratedSpacerColors = GwernDecoratedSpacerColors(
        accent = accent,
        backgroundTop = backgroundTop,
        backgroundBottom = backgroundBottom,
        border = border,
        stripe = stripe,
        dotHighlight = dotHighlight,
    )
}

@Composable
fun GwernDecoratedSpacer(
    height: Dp,
    isTop: Boolean,
    modifier: Modifier = Modifier,
    colors: GwernDecoratedSpacerColors = GwernDecoratedSpacerDefaults.colors(),
) {
    val coercedHeight = height.coerceAtLeast(0.dp)
    if (coercedHeight <= 0.dp) {
        Spacer(modifier.height(coercedHeight))
        return
    }

    val density = LocalDensity.current

    Canvas(
        modifier
            .fillMaxWidth()
            .height(coercedHeight)
    ) {
        val widthPx = size.width
        val heightPx = size.height
        val backgroundBrush = if (isTop) {
            Brush.verticalGradient(listOf(colors.backgroundBottom, colors.backgroundTop))
        } else {
            Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom))
        }
        drawRect(brush = backgroundBrush)

        val stripeSpacing = with(density) { 22.dp.toPx() }
        val stripeStrokeWidth = with(density) { 0.75.dp.toPx() }
        if (stripeSpacing > 0f && stripeStrokeWidth > 0f) {
            var startX = -heightPx
            while (startX < widthPx + heightPx) {
                val startY = if (isTop) heightPx else 0f
                val endY = if (isTop) 0f else heightPx
                drawLine(
                    color = colors.stripe,
                    start = Offset(startX, startY),
                    end = Offset(startX + heightPx, endY),
                    strokeWidth = stripeStrokeWidth,
                )
                startX += stripeSpacing
            }
        }

        val accentStrokeWidth = with(density) { 1.25.dp.toPx() }
        val accentMargin = min(heightPx / 3f, with(density) { 12.dp.toPx() })
        val accentY = if (isTop) heightPx - accentMargin else accentMargin
        drawLine(
            color = colors.accent,
            start = Offset(0f, accentY),
            end = Offset(widthPx, accentY),
            strokeWidth = accentStrokeWidth,
        )

        val dotRadius = min(with(density) { 2.5.dp.toPx() }, accentMargin / 1.8f)
        val dotSpacing = with(density) { 20.dp.toPx() }
        if (dotSpacing > 0f && dotRadius > 0f) {
            var x = dotSpacing / 2f
            while (x < widthPx) {
                drawCircle(
                    color = colors.accent.copy(alpha = 0.85f),
                    radius = dotRadius,
                    center = Offset(x, accentY),
                )
                drawCircle(
                    color = colors.dotHighlight,
                    radius = dotRadius * 0.45f,
                    center = Offset(x, accentY),
                )
                x += dotSpacing
            }
        }

        val borderStrokeWidth = with(density) { 1.dp.toPx() }
        val borderY = if (isTop) {
            heightPx - borderStrokeWidth / 2f
        } else {
            borderStrokeWidth / 2f
        }
        drawLine(
            color = colors.border,
            start = Offset(0f, borderY),
            end = Offset(widthPx, borderY),
            strokeWidth = borderStrokeWidth,
        )
    }
}
