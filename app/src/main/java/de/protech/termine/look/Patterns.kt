package de.protech.termine.look

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Gruppen, damit die Auswahl übersichtlich bleibt. */
enum class PatternGroup(val label: String) {
    PLAIN("Ohne"),
    SUBTLE("Dezent"),
    GEOMETRIC("Geometrisch"),
    LIVELY("Lebhaft")
}

enum class Pattern(val label: String, val group: PatternGroup) {
    NONE("Kein Muster", PatternGroup.PLAIN),

    DOTS("Punkte", PatternGroup.SUBTLE),
    GRID("Raster", PatternGroup.SUBTLE),
    CROSSHATCH("Karo", PatternGroup.SUBTLE),
    CONTOUR("Höhenlinien", PatternGroup.SUBTLE),

    DIAGONAL("Schräge", PatternGroup.GEOMETRIC),
    CHEVRON("Zickzack", PatternGroup.GEOMETRIC),
    HONEYCOMB("Waben", PatternGroup.GEOMETRIC),
    PLUS("Kreuze", PatternGroup.GEOMETRIC),

    WAVES("Wellen", PatternGroup.LIVELY),
    CONFETTI("Konfetti", PatternGroup.LIVELY);

    companion object {
        /** Nach Gruppe sortiert, für die Auswahl in den Einstellungen. */
        fun grouped(): List<Pair<PatternGroup, List<Pattern>>> =
            PatternGroup.entries.map { group -> group to entries.filter { it.group == group } }
                .filter { it.second.isNotEmpty() }
    }
}

/**
 * Zeichnet ein Muster über die gesamte Fläche.
 *
 * Alle Muster werden berechnet, nicht aus Bilddateien geladen: so passen sie
 * sich jeder Akzentfarbe und jeder Bildschirmgröße an und kosten kein Gewicht
 * in der APK.
 */
fun DrawScope.drawPattern(pattern: Pattern, color: Color, scale: Float = 1f) {
    if (pattern == Pattern.NONE) return

    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val unit = 34f * scale * density

    when (pattern) {
        Pattern.NONE -> Unit

        Pattern.DOTS -> {
            val radius = 1.9f * scale * density
            var y = unit / 2
            var row = 0
            while (y < h + unit) {
                val offset = if (row % 2 == 0) 0f else unit / 2
                var x = offset
                while (x < w + unit) {
                    drawCircle(color, radius, Offset(x, y))
                    x += unit
                }
                y += unit
                row++
            }
        }

        Pattern.GRID -> {
            val stroke = 1f * density
            var x = 0f
            while (x < w + unit) {
                drawLine(color, Offset(x, 0f), Offset(x, h), stroke)
                x += unit
            }
            var y = 0f
            while (y < h + unit) {
                drawLine(color, Offset(0f, y), Offset(w, y), stroke)
                y += unit
            }
        }

        Pattern.CROSSHATCH -> {
            val stroke = 1f * density
            val step = unit * 1.1f
            var d = -h
            while (d < w + h) {
                drawLine(color, Offset(d, 0f), Offset(d + h, h), stroke)
                drawLine(color, Offset(d, h), Offset(d + h, 0f), stroke)
                d += step
            }
        }

        Pattern.CONTOUR -> {
            val stroke = 1.3f * density
            val center = Offset(w * 0.82f, h * 0.18f)
            var radius = unit * 0.8f
            while (radius < (w + h)) {
                drawCircle(color, radius, center, style = Stroke(width = stroke))
                radius += unit * 1.15f
            }
        }

        Pattern.DIAGONAL -> {
            val stroke = 3f * scale * density
            val step = unit * 0.9f
            var d = -h
            while (d < w + h) {
                drawLine(color, Offset(d, 0f), Offset(d + h, h), stroke)
                d += step
            }
        }

        Pattern.CHEVRON -> {
            val stroke = 2f * scale * density
            val step = unit * 0.95f
            val width = unit * 0.8f
            var y = -step
            while (y < h + step * 2) {
                val path = Path()
                var x = 0f
                var up = true
                path.moveTo(0f, y)
                while (x < w + width) {
                    x += width
                    path.lineTo(x, if (up) y - width * 0.55f else y)
                    up = !up
                }
                drawPath(path, color, style = Stroke(width = stroke))
                y += step
            }
        }

        Pattern.HONEYCOMB -> {
            val stroke = 1.2f * density
            val radius = unit * 0.62f
            val horizontal = radius * 1.732f
            val vertical = radius * 1.5f
            var row = 0
            var y = 0f
            while (y < h + vertical) {
                val shift = if (row % 2 == 0) 0f else horizontal / 2
                var x = shift
                while (x < w + horizontal) {
                    drawPath(hexagon(Offset(x, y), radius), color, style = Stroke(width = stroke))
                    x += horizontal
                }
                y += vertical
                row++
            }
        }

        Pattern.PLUS -> {
            val stroke = 1.8f * density
            val arm = unit * 0.22f
            var row = 0
            var y = unit / 2
            while (y < h + unit) {
                val shift = if (row % 2 == 0) 0f else unit / 2
                var x = shift
                while (x < w + unit) {
                    drawLine(color, Offset(x - arm, y), Offset(x + arm, y), stroke)
                    drawLine(color, Offset(x, y - arm), Offset(x, y + arm), stroke)
                    x += unit
                }
                y += unit
                row++
            }
        }

        Pattern.WAVES -> {
            val stroke = 1.8f * density
            val amplitude = unit * 0.28f
            val wavelength = unit * 1.7f
            val stepX = 4f * density
            var y = 0f
            while (y < h + unit) {
                val path = Path()
                var x = 0f
                path.moveTo(0f, y)
                while (x < w) {
                    x += stepX
                    path.lineTo(x, y + sin(x / wavelength * 6.2832f) * amplitude)
                }
                drawPath(path, color, style = Stroke(width = stroke))
                y += unit * 0.85f
            }
        }

        Pattern.CONFETTI -> {
            // Fester Startwert: das Muster soll bei jedem Zeichnen gleich aussehen.
            val random = Random(42)
            val count = ((w * h) / (unit * unit * 1.6f)).toInt().coerceIn(30, 900)
            repeat(count) {
                val x = random.nextFloat() * w
                val y = random.nextFloat() * h
                val length = unit * (0.16f + random.nextFloat() * 0.2f)
                val angle = random.nextFloat() * 6.2832f
                drawLine(
                    color,
                    Offset(x, y),
                    Offset(x + cos(angle) * length, y + sin(angle) * length),
                    3f * scale * density
                )
            }
        }
    }
}

private fun hexagon(center: Offset, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians((60.0 * i) - 30.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
