@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package de.protech.termine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.protech.termine.look.Accents
import de.protech.termine.look.AppCard
import de.protech.termine.look.Appearance
import de.protech.termine.look.CardShape
import de.protech.termine.look.LocalAppearance
import de.protech.termine.look.Pattern
import de.protech.termine.look.ThemeMode
import de.protech.termine.look.drawPattern
import de.protech.termine.look.luminanceIsDark
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAccent: (Color) -> Unit,
    onSystemColors: (Boolean) -> Unit,
    onShape: (CardShape) -> Unit,
    onOpacity: (Int) -> Unit,
    onPattern: (Pattern) -> Unit,
    onPatternStrength: (Int) -> Unit,
    onReset: () -> Unit
) {
    val look = LocalAppearance.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Darstellung") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Restore, contentDescription = "Zurücksetzen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            PreviewTile(look)

            Block(title = "Helligkeit") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = look.themeMode == mode,
                            onClick = { onThemeMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            Block(
                title = "Akzentfarbe",
                subtitle = if (look.useSystemColors) "Gerade übernimmt die App die Farben des Systems."
                else "Gewählt: ${Accents.nameOf(look.accent)}"
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Accents.ALL.forEach { (_, color) ->
                        val selected = !look.useSystemColors && look.accent.value == color.value
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { onAccent(color) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (color.luminanceIsDark()) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                FilterChip(
                    selected = look.useSystemColors,
                    onClick = { onSystemColors(!look.useSystemColors) },
                    label = { Text("Systemfarben verwenden") }
                )
            }

            Block(
                title = "Kachelform",
                subtitle = "Wie rund die Ecken der Kacheln sind."
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardShape.entries.forEach { shape ->
                        FilterChip(
                            selected = look.shape == shape,
                            onClick = { onShape(shape) },
                            label = { Text(shape.label) }
                        )
                    }
                }
            }

            Block(
                title = "Deckkraft der Kacheln",
                subtitle = if (look.opacityPercent >= 100) "Vollflächig"
                else "${look.opacityPercent} % – das Muster scheint durch"
            ) {
                Slider(
                    value = look.opacityPercent.toFloat(),
                    onValueChange = { onOpacity(it.roundToInt()) },
                    valueRange = 55f..100f,
                    steps = 8
                )
            }

            Block(
                title = "Hintergrundmuster",
                subtitle = "Gewählt: ${look.pattern.label}"
            ) {
                Pattern.grouped().forEach { (group, patterns) ->
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        patterns.forEach { pattern ->
                            PatternTile(
                                pattern = pattern,
                                accent = look.accent,
                                selected = look.pattern == pattern,
                                onClick = { onPattern(pattern) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (look.pattern != Pattern.NONE) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Stärke: ${look.patternStrength} %",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = look.patternStrength.toFloat(),
                        onValueChange = { onPatternStrength(it.roundToInt()) },
                        valueRange = 20f..100f,
                        steps = 7
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Restore, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Auf Standard zurücksetzen")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ---------- Bausteine ---------- */

@Composable
private fun Block(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(14.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/** Zeigt sofort, wie sich Farbe, Form, Deckkraft und Muster zusammen anfühlen. */
@Composable
private fun PreviewTile(look: Appearance) {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val canvasColor = if (dark) Color(0xFF101614) else Color(0xFFF3F8F5)
    val patternColor = look.accent.copy(alpha = (look.patternStrength / 100f) * 0.22f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                drawRect(canvasColor)
                drawPattern(look.pattern, patternColor)
            }
    ) {
        AppCard(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 18.dp)
                .fillMaxWidth(),
            container = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(look.accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ALS NÄCHSTES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("noch 2 Tage 3 Std", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("Zahnarzt · Kontrolle", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text("1 Tag · 1 Std vorher", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Kleine Kachel mit dem gezeichneten Muster als Vorschau. */
@Composable
private fun PatternTile(
    pattern: Pattern,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val tileColor = scheme.surfaceContainerHighest
    val tilePattern = accent.copy(alpha = 0.55f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp)
    ) {
        Box(
            Modifier
                .size(width = 78.dp, height = 58.dp)
                .clip(RoundedCornerShape(14.dp))
                .drawBehind {
                    drawRect(tileColor)
                    drawPattern(pattern, tilePattern, scale = 0.55f)
                }
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) scheme.primary else scheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Surface(shape = CircleShape, color = scheme.primary) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(16.dp),
                        tint = scheme.onPrimary
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = pattern.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}
