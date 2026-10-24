package com.keyo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * "Transparency effects": a frosted-glass keyboard after ColorOS 17's "Liquid Acrylic" look —
 * layered translucent sheets over a blurred backdrop, a thin luminous contour on every surface
 * ("contour glow"), light that gathers where the finger touches, and a soft springy squish.
 *
 * The blur itself is the window's (cross-window blur, Android 12+, see KeyoService.applyWindowGlass);
 * everything here is drawing. Where the device can't blur, the sheets stay more opaque so text on
 * them stays readable over whatever the app shows.
 */
internal object Glass {
    /** Blur radius of the app behind the keyboard. */
    const val BLUR_DP = 30
    /** Corner radius of keys in glass mode: rounder, like the ColorOS 17 controls. */
    val KEY_RADIUS = 10.dp
    val POPUP_RADIUS = 14.dp

    fun isDark(bg: Color) = bg.luminance() < 0.5f

    /** How opaque the keyboard's own sheet is. With a real blur behind it the app may show through
     *  more; without one the busy content behind has to be muted by the sheet alone. */
    fun sheetAlpha(blurring: Boolean, dark: Boolean): Float =
        if (blurring) (if (dark) 0.55f else 0.60f) else (if (dark) 0.86f else 0.88f)
}

/**
 * The face of one key. Solid with a 22%-darker press when glass is off — exactly the classic look.
 * In glass: a translucent tile, a sheen from above, a contour rim lit from the top-left, a glow at
 * the centre while pressed and a springy squish. [strong] keeps a key nearly opaque: accent keys
 * (Enter, an active Shift) and state colours (recording, swipe-to-clear) must not wash out.
 */
@Composable
internal fun Modifier.keyFace(
    color: Color,
    pressed: Boolean,
    glass: Boolean,
    dark: Boolean,
    strong: Boolean = false
): Modifier {
    if (!glass) return background(if (pressed) lerp(color, Color.Black, 0.22f) else color, RoundedCornerShape(6.dp))
    val squish by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1200f),
        label = "key-squish"
    )
    return this
        .graphicsLayer { scaleX = squish; scaleY = squish }
        .drawBehind { drawGlassTile(color, pressed, dark, strong, Glass.KEY_RADIUS.toPx()) }
}

/** A floating surface above the keys (accent popup, key preview): glass, but nearly opaque. */
internal fun Modifier.popupFace(color: Color, glass: Boolean, dark: Boolean): Modifier =
    if (!glass) background(color, RoundedCornerShape(8.dp))
    else drawBehind { drawGlassTile(color, pressed = false, dark = dark, strong = true, radius = Glass.POPUP_RADIUS.toPx()) }

/** The keyboard's own sheet: translucent theme colour, with a luminous hairline along the top edge
 *  where the glass meets the app. */
internal fun Modifier.keyboardSheet(color: Color, glass: Boolean, blurring: Boolean): Modifier =
    if (!glass) background(color)
    else drawBehind {
        val dark = Glass.isDark(color)
        drawRect(color.copy(alpha = Glass.sheetAlpha(blurring, dark)))
        val hair = 1.dp.toPx()
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = if (dark) 0.22f else 0.75f), Color.White.copy(alpha = 0f))
            ),
            size = Size(size.width, hair)
        )
    }

internal fun DrawScope.drawGlassTile(base: Color, pressed: Boolean, dark: Boolean, strong: Boolean, radius: Float) {
    val cr = CornerRadius(radius)
    val fill = when {
        strong -> 0.92f
        dark -> 0.42f
        else -> 0.62f
    }
    drawRoundRect(base.copy(alpha = base.alpha * fill), cornerRadius = cr)
    // Light falling from above: the top of the sheet is a little brighter than its foot.
    drawRoundRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (dark) 0.10f else 0.32f),
            0.6f to Color.White.copy(alpha = 0f)
        ),
        cornerRadius = cr
    )
    // Light gathering under the finger.
    if (pressed) drawRoundRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (dark) 0.30f else 0.60f), Color.White.copy(alpha = 0f)),
            center = center,
            radius = size.maxDimension * 0.7f
        ),
        cornerRadius = cr
    )
    // Contour glow: a hairline rim, brightest where the light comes from (top-left).
    val sw = 1.dp.toPx()
    val boost = if (pressed) 0.2f else 0f
    drawRoundRect(
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = ((if (dark) 0.40f else 0.95f) + boost).coerceAtMost(1f)),
                Color.White.copy(alpha = ((if (dark) 0.05f else 0.35f) + boost / 2).coerceAtMost(1f))
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        topLeft = Offset(sw / 2, sw / 2),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius((radius - sw / 2).coerceAtLeast(0f)),
        style = Stroke(sw)
    )
}
