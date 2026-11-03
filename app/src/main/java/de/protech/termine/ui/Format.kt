package de.protech.termine.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DE = Locale.GERMAN
private val zone: ZoneId get() = ZoneId.systemDefault()

private val longDateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", DE)
private val mediumDateFmt = DateTimeFormatter.ofPattern("EE, d. MMM yyyy", DE)
private val dayMonthFmt = DateTimeFormatter.ofPattern("d. MMMM", DE)
private val numericDateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", DE)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", DE)

fun localDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

fun formatLongDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(longDateFmt)

fun formatMediumDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(mediumDateFmt)

fun formatDayMonth(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(dayMonthFmt)

fun formatNumericDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(numericDateFmt)

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(timeFmt)

/** Kurzer Wochentag, z. B. "Mo". */
fun weekdayShort(millis: Long): String =
    localDate(millis).dayOfWeek.getDisplayName(TextStyle.SHORT, DE).trimEnd('.')

fun dayOfMonth(millis: Long): String = localDate(millis).dayOfMonth.toString()

/** "heute", "morgen", "in 5 Tagen", "vor 3 Tagen" … */
fun relativeDay(millis: Long, now: Long = System.currentTimeMillis()): String {
    val days = ChronoUnit.DAYS.between(localDate(now), localDate(millis))
    return when {
        days == 0L -> "heute"
        days == 1L -> "morgen"
        days == 2L -> "übermorgen"
        days == -1L -> "gestern"
        days in 3..6 -> "in $days Tagen"
        days in 7..13 -> "in einer Woche"
        days in 14..30 -> "in ${days / 7} Wochen"
        days > 30 -> "in ${(days / 30).coerceAtLeast(1)} Monaten"
        days < -1 -> "vor ${-days} Tagen"
        else -> ""
    }
}

/** Countdown für die große Karte, z. B. "noch 2 Tage 3 Std". */
fun countdown(millis: Long, now: Long = System.currentTimeMillis()): String {
    val duration = Duration.ofMillis(millis - now)
    if (duration.isNegative || duration.isZero) return "jetzt"

    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60

    return when {
        days > 0 -> buildString {
            append("noch $days ")
            append(if (days == 1L) "Tag" else "Tage")
            if (hours > 0) append(" $hours Std")
        }
        hours > 0 -> "noch $hours Std $minutes Min"
        minutes > 0 -> "noch $minutes Min"
        else -> "jetzt"
    }
}

/** Überschrift einer Zeitgruppe in der Übersicht. */
fun groupLabel(millis: Long, now: Long = System.currentTimeMillis()): String {
    val today = localDate(now)
    val target = localDate(millis)
    val days = ChronoUnit.DAYS.between(today, target)

    // Sonntag als Wochenende, Montag als Wochenanfang.
    val daysUntilSunday = (7 - today.dayOfWeek.value) % 7

    return when {
        days == 0L -> "Heute"
        days == 1L -> "Morgen"
        days <= daysUntilSunday -> "Diese Woche"
        days <= daysUntilSunday + 7 -> "Nächste Woche"
        target.year == today.year ->
            target.month.getDisplayName(TextStyle.FULL, DE)
        else ->
            "${target.month.getDisplayName(TextStyle.FULL, DE)} ${target.year}"
    }
}

/** Menschlicher Text für eine Vorlaufzeit in Minuten. */
fun reminderLabel(minutes: Int): String = when {
    minutes <= 0 -> "zur Terminzeit"
    minutes < 60 -> "$minutes Minuten vorher"
    minutes == 60 -> "1 Stunde vorher"
    minutes < 1440 && minutes % 60 == 0 -> "${minutes / 60} Stunden vorher"
    minutes == 1440 -> "1 Tag vorher"
    minutes == 10080 -> "1 Woche vorher"
    minutes % 1440 == 0 -> "${minutes / 1440} Tage vorher"
    else -> "${minutes / 60} Std ${minutes % 60} Min vorher"
}

/** Knappe Variante für die Auswahl-Chips. */
fun reminderChipLabel(minutes: Int): String = when {
    minutes <= 0 -> "pünktlich"
    minutes < 60 -> "$minutes Min"
    minutes == 60 -> "1 Std"
    minutes < 1440 -> "${minutes / 60} Std"
    minutes == 1440 -> "1 Tag"
    minutes == 10080 -> "1 Woche"
    else -> "${minutes / 1440} Tage"
}

val REMINDER_PRESETS = listOf(0, 15, 30, 45, 60, 120, 180, 1440, 2880, 10080)
