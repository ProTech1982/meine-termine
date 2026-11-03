package de.protech.termine.look

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Hintergrund der ganzen App: Grundfarbe, ein sanfter Verlauf aus der
 * Akzentfarbe und darüber das gewählte Muster.
 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val look = LocalAppearance.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminanceIsDark()

    val accent = look.accent
    val patternColor = look.effectivePatternColor
        .copy(alpha = (look.patternStrength / 100f) * if (dark) 0.14f else 0.11f)
    val glow = accent.copy(alpha = if (dark) 0.16f else 0.11f)

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(scheme.background)
                drawRect(
                    Brush.verticalGradient(
                        0f to glow,
                        0.45f to Color.Transparent,
                        1f to glow.copy(alpha = glow.alpha * 0.5f)
                    )
                )
                drawPattern(look.pattern, patternColor)
            }
    ) {
        content()
    }
}

/** Eckenradius der gewählten Kachelform. */
@Composable
fun cardShape(): Shape = RoundedCornerShape(LocalAppearance.current.shape.cornerDp.dp)

/** Etwas kleinerer Radius für kleine Elemente wie Eingabefelder. */
@Composable
fun fieldShape(): Shape =
    RoundedCornerShape((LocalAppearance.current.shape.cornerDp - 6).coerceAtLeast(4).dp)

/**
 * Kachel im eingestellten Stil: Form, Deckkraft und – bei durchsichtigen
 * Kacheln – eine feine Kante statt eines Schattens. Ein Schatten unter einer
 * halbdurchsichtigen Fläche sieht falsch aus.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color? = null,
    contentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val look = LocalAppearance.current
    val scheme = MaterialTheme.colorScheme
    val base = container ?: scheme.surfaceContainerLow
    val filled = base.copy(alpha = base.alpha * look.cardAlpha)

    val colors = CardDefaults.cardColors(
        containerColor = filled,
        contentColor = contentColor ?: scheme.onSurface
    )
    val elevation = CardDefaults.cardElevation(
        defaultElevation = if (look.translucent) 0.dp else 1.dp
    )
    val border = if (look.translucent) {
        BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f))
    } else null

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = cardShape(),
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = cardShape(),
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

/** Grobe Einschätzung, ob eine Farbe dunkel ist – für Kontrastentscheidungen. */
fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
