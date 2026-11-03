package de.protech.termine.look

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Helligkeit der App, unabhängig vom System einstellbar. */
enum class ThemeMode(val label: String) {
    SYSTEM("Wie das System"),
    LIGHT("Immer hell"),
    DARK("Immer dunkel")
}

/** Kachelform: Beschriftung und Eckenradius in dp. */
enum class CardShape(val label: String, val cornerDp: Int) {
    ANGULAR("Kantig", 8),
    SOFT("Weich", 20),
    ROUND("Rund", 34)
}

data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Color = Accents.DEFAULT,
    val useSystemColors: Boolean = false,
    val shape: CardShape = CardShape.SOFT,
    /** Deckkraft der Kacheln in Prozent, 55 bis 100. */
    val opacityPercent: Int = 100,
    val pattern: Pattern = Pattern.NONE,
    /** Stärke des Musters in Prozent, 20 bis 100. */
    val patternStrength: Int = 60,
    /** Eigene Farbe für das Muster. */
    val patternColor: Color = Accents.DEFAULT,
    /** Solange true, folgt das Muster der Akzentfarbe. */
    val patternFollowsAccent: Boolean = true
) {
    val cardAlpha: Float get() = opacityPercent.coerceIn(55, 100) / 100f
    val translucent: Boolean get() = opacityPercent < 100

    /** Die Farbe, in der das Muster tatsächlich gezeichnet wird. */
    val effectivePatternColor: Color
        get() = if (patternFollowsAccent) accent else patternColor
}

/** Auswahl der Akzentfarben. Namen wie bei „Unsere Tage“. */
object Accents {
    val DEFAULT = Color(0xFF0A6B5B)

    val ALL: List<Pair<String, Color>> = listOf(
        "Petrol" to Color(0xFF0A6B5B),
        "Salbei" to Color(0xFF4F8B63),
        "Enzian" to Color(0xFF3D6DB5),
        "Himmel" to Color(0xFF2E7D8A),
        "Flieder" to Color(0xFF7B52AB),
        "Rosé" to Color(0xFFC14D6B),
        "Mohn" to Color(0xFFC0392B),
        "Kupfer" to Color(0xFFC2662C),
        "Gold" to Color(0xFFBE912F),
        "Graphit" to Color(0xFF4A5A62)
    )

    /** Zusätzliche, farblose Töne – nur für Muster sinnvoll. */
    val NEUTRALS: List<Pair<String, Color>> = listOf(
        "Nebel" to Color(0xFF9AA7A3),
        "Sand" to Color(0xFFBCA98B),
        "Tinte" to Color(0xFF2B363B)
    )

    /** Auswahl für die Musterfarbe: alle Akzente plus die neutralen Töne. */
    val FOR_PATTERN: List<Pair<String, Color>> = ALL + NEUTRALS

    fun nameOf(color: Color): String =
        (ALL + NEUTRALS).firstOrNull { it.second.value == color.value }?.first ?: "Eigene"
}

/**
 * Hält die Darstellung und schreibt jede Änderung sofort mit, damit sie einen
 * Neustart übersteht. Bewusst mit SharedPreferences – für fünf Werte braucht
 * es keine Datenbank.
 */
class AppearanceStore(context: Context) {

    private val prefs = context.getSharedPreferences("darstellung", Context.MODE_PRIVATE)

    var value: Appearance by mutableStateOf(load())
        private set

    private fun load(): Appearance {
        val accentArgb = prefs.getInt(KEY_ACCENT, Accents.DEFAULT.toArgb())
        return Appearance(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
            }.getOrDefault(ThemeMode.SYSTEM),
            accent = Color(accentArgb),
            useSystemColors = prefs.getBoolean(KEY_SYSTEM_COLORS, false),
            shape = runCatching {
                CardShape.valueOf(prefs.getString(KEY_SHAPE, CardShape.SOFT.name)!!)
            }.getOrDefault(CardShape.SOFT),
            opacityPercent = prefs.getInt(KEY_OPACITY, 100),
            pattern = runCatching {
                Pattern.valueOf(prefs.getString(KEY_PATTERN, Pattern.NONE.name)!!)
            }.getOrDefault(Pattern.NONE),
            patternStrength = prefs.getInt(KEY_PATTERN_STRENGTH, 60),
            patternColor = Color(prefs.getInt(KEY_PATTERN_COLOR, Accents.DEFAULT.toArgb())),
            patternFollowsAccent = prefs.getBoolean(KEY_PATTERN_FOLLOWS, true)
        )
    }

    private fun update(block: (Appearance) -> Appearance) {
        val next = block(value)
        value = next
        prefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putInt(KEY_ACCENT, next.accent.toArgb())
            .putBoolean(KEY_SYSTEM_COLORS, next.useSystemColors)
            .putString(KEY_SHAPE, next.shape.name)
            .putInt(KEY_OPACITY, next.opacityPercent)
            .putString(KEY_PATTERN, next.pattern.name)
            .putInt(KEY_PATTERN_STRENGTH, next.patternStrength)
            .putInt(KEY_PATTERN_COLOR, next.patternColor.toArgb())
            .putBoolean(KEY_PATTERN_FOLLOWS, next.patternFollowsAccent)
            .apply()
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setAccent(color: Color) = update { it.copy(accent = color, useSystemColors = false) }
    fun setUseSystemColors(enabled: Boolean) = update { it.copy(useSystemColors = enabled) }
    fun setShape(shape: CardShape) = update { it.copy(shape = shape) }
    fun setOpacity(percent: Int) = update { it.copy(opacityPercent = percent.coerceIn(55, 100)) }
    fun setPattern(pattern: Pattern) = update { it.copy(pattern = pattern) }
    fun setPatternStrength(percent: Int) =
        update { it.copy(patternStrength = percent.coerceIn(20, 100)) }

    fun setPatternColor(color: Color) =
        update { it.copy(patternColor = color, patternFollowsAccent = false) }

    fun setPatternFollowsAccent(follows: Boolean) =
        update { it.copy(patternFollowsAccent = follows) }

    fun reset() = update {
        Appearance()
    }

    private companion object {
        const val KEY_THEME = "helligkeit"
        const val KEY_ACCENT = "akzent"
        const val KEY_SYSTEM_COLORS = "systemfarben"
        const val KEY_SHAPE = "kachelform"
        const val KEY_OPACITY = "deckkraft"
        const val KEY_PATTERN = "muster"
        const val KEY_PATTERN_STRENGTH = "musterstaerke"
        const val KEY_PATTERN_COLOR = "musterfarbe"
        const val KEY_PATTERN_FOLLOWS = "musterfolgtakzent"
    }
}

val LocalAppearance = staticCompositionLocalOf { Appearance() }
