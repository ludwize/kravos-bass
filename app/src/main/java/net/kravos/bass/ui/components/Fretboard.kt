package net.kravos.bass.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kravos.bass.music.Bass
import net.kravos.bass.music.BoardMarker
import net.kravos.bass.music.MarkerStyle
import net.kravos.bass.ui.theme.RoleColors

private val Wood = Color(0xFF2E2019)
private val WoodDark = Color(0xFF241812)
private val FretColor = Color(0xFFB9B2A8)
private val NutColor = Color(0xFFE9E2D4)
private val Inlay = Color(0x55EFE4D0)
private val StringColor = Color(0xFFD9D2C6)

/** Extra overlay for tap feedback. */
data class TapMark(val string: Int, val fret: Int, val ok: Boolean)

/**
 * Bass neck seen like a tab: G string on top, E at the bottom, nut on the left.
 * Fits the width when possible, otherwise scrolls horizontally.
 */
@Composable
fun Fretboard(
    markers: List<BoardMarker>,
    modifier: Modifier = Modifier,
    frets: Int = 12,
    onTap: ((string: Int, fret: Int) -> Unit)? = null,
    taps: List<TapMark> = emptyList(),
    rowHeight: Dp = 38.dp,
) {
    val measurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val nutW = 34.dp
        val fretW = maxOf(24.dp, (maxWidth - nutW) / frets)
        val totalW = nutW + fretW * frets
        val numbersH = 18.dp
        val height = rowHeight * 4 + numbersH
        val scroll = rememberScrollState()
        val focus = markers.firstOrNull { it.style == MarkerStyle.QUESTION } ?: markers.firstOrNull { it.style == MarkerStyle.TARGET }
            ?: markers.firstOrNull { it.label == "R" }
        val viewW = maxWidth
        LaunchedEffect(focus, totalW > viewW) {
            if (focus != null && totalW > viewW) {
                with(density) {
                    val x = (nutW + fretW * focus.fret - fretW / 2).toPx() - viewW.toPx() / 2
                    scroll.animateScrollTo(x.toInt().coerceIn(0, scroll.maxValue.coerceAtLeast(0)))
                }
            }
        }
        val labelStyle = remember { TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        val smallStyle = remember { TextStyle(fontSize = 11.sp, color = Color(0xFF9A948C)) }
        val openStyle = remember { TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6A5A4E)) }

        Canvas(
            Modifier
                .horizontalScroll(scroll, enabled = totalW > maxWidth)
                .width(totalW)
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .then(
                    if (onTap != null) {
                        Modifier.pointerInput(frets, fretW) {
                            detectTapGestures { p ->
                                val nutPx = nutW.toPx()
                                val fw = fretW.toPx()
                                val rh = rowHeight.toPx()
                                if (p.y > rh * 4) return@detectTapGestures
                                val row = (p.y / rh).toInt().coerceIn(0, 3)
                                val string = 3 - row
                                val fret = if (p.x < nutPx) 0 else (1 + ((p.x - nutPx) / fw).toInt()).coerceAtMost(frets)
                                onTap(string, fret)
                            }
                        }
                    } else Modifier,
                ),
        ) {
            val nutPx = nutW.toPx()
            val fw = fretW.toPx()
            val rh = rowHeight.toPx()
            val boardH = rh * 4
            // wood
            drawRect(Brush.verticalGradient(listOf(Wood, WoodDark)), size = Size(size.width, boardH))
            drawRect(Color(0xFF1A1310), topLeft = Offset(0f, 0f), size = Size(nutPx, boardH))
            // inlays
            for (f in listOf(3, 5, 7, 9, 15, 17)) if (f <= frets) {
                drawCircle(Inlay, radius = rh * 0.16f, center = Offset(nutPx + (f - 0.5f) * fw, boardH / 2))
            }
            if (12 <= frets) {
                drawCircle(Inlay, radius = rh * 0.16f, center = Offset(nutPx + 11.5f * fw, rh * 1f))
                drawCircle(Inlay, radius = rh * 0.16f, center = Offset(nutPx + 11.5f * fw, rh * 3f))
            }
            // frets
            drawRect(NutColor, topLeft = Offset(nutPx - 5f, 0f), size = Size(5f, boardH))
            for (f in 1..frets) {
                val x = nutPx + f * fw
                drawLine(FretColor, Offset(x, 0f), Offset(x, boardH), strokeWidth = 2.5f)
            }
            // strings (thicker for low)
            for (row in 0..3) {
                val s = 3 - row
                val y = row * rh + rh / 2
                drawLine(StringColor, Offset(nutPx - 4f, y), Offset(size.width, y), strokeWidth = 2f + s * 1.1f)
                // open string name in the nut area (dimmed; a marker at fret 0 will cover it)
                val t = measurer.measure(Bass.STRING_NAMES[s], openStyle)
                drawText(t, topLeft = Offset(nutPx / 2 - t.size.width / 2f - 2f, y - t.size.height / 2f))
            }
            // fret numbers
            for (f in 0..frets) {
                if (f == 0 || f in listOf(3, 5, 7, 9, 12, 15) || fw > 44.dp.toPx()) {
                    val t = measurer.measure(f.toString(), smallStyle)
                    val cx = if (f == 0) nutPx / 2 else nutPx + (f - 0.5f) * fw
                    drawText(t, topLeft = Offset(cx - t.size.width / 2f, boardH + 2f))
                }
            }
            val r = minOf(fw, rh) * 0.40f
            fun center(s: Int, f: Int) = Offset(if (f == 0) nutPx / 2 - 2f else nutPx + (f - 0.5f) * fw, (3 - s) * rh + rh / 2)

            // markers: soft/ghost first so strong ones sit on top
            val sorted = markers.filter { it.fret <= frets }.sortedBy {
                when (it.style) {
                    MarkerStyle.GHOST -> 0; MarkerStyle.SOFT -> 1; MarkerStyle.STRONG -> 2; MarkerStyle.TARGET -> 3; MarkerStyle.QUESTION -> 4
                }
            }
            for (m in sorted) {
                val c = center(m.string, m.fret)
                val col = if (m.style == MarkerStyle.QUESTION) RoleColors.question else RoleColors.of(m.role)
                when (m.style) {
                    MarkerStyle.GHOST -> {
                        drawCircle(Color(0x33FFFFFF), radius = r * 0.85f, center = c)
                    }
                    MarkerStyle.SOFT -> {
                        drawCircle(col.copy(alpha = 0.45f), radius = r * 0.88f, center = c)
                        drawCircle(col.copy(alpha = 0.7f), radius = r * 0.88f, center = c, style = Stroke(2f))
                    }
                    MarkerStyle.TARGET -> {
                        drawCircle(col, radius = r, center = c)
                        drawCircle(Color.White, radius = r + 3f, center = c, style = Stroke(3f))
                    }
                    else -> drawCircle(col, radius = r, center = c)
                }
                m.label?.let { label ->
                    val st = if (m.style == MarkerStyle.SOFT || m.style == MarkerStyle.GHOST) {
                        labelStyle.copy(fontSize = 11.sp, color = Color(0xFFEDE7DE), fontWeight = FontWeight.SemiBold)
                    } else if (m.style == MarkerStyle.QUESTION) labelStyle.copy(color = Color(0xFF2A1800)) else labelStyle
                    val fs = if (label.length >= 3) st.copy(fontSize = 10.sp) else st
                    val t = measurer.measure(label, fs)
                    drawText(t, topLeft = Offset(c.x - t.size.width / 2f, c.y - t.size.height / 2f))
                }
            }
            for (tp in taps) {
                val c = center(tp.string, tp.fret)
                val col = if (tp.ok) Color(0xFF34C77B) else Color(0xFFFF5A4E)
                drawCircle(col, radius = r * 1.12f, center = c, style = Stroke(5f))
                if (!tp.ok) {
                    val d = r * 0.5f
                    drawLine(col, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), strokeWidth = 5f)
                    drawLine(col, Offset(c.x - d, c.y + d), Offset(c.x + d, c.y - d), strokeWidth = 5f)
                }
            }
            drawRoundRect(Color(0x22000000), size = Size(size.width, boardH), cornerRadius = CornerRadius(0f), style = Stroke(1f))
        }
    }
}
