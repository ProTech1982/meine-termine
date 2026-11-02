package de.protech.termine.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
private val shortDateFmt = DateTimeFormatter.ofPattern("EE, d. MMM yyyy", Locale.GERMAN)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFmt)

fun formatShortDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(shortDateFmt)

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFmt)

/** "heute", "morgen", "in 5 Tagen", "vor 3 Tagen" ... */
fun relativeDay(millis: Long, now: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val target = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, target)

    return when {
        days == 0L -> "heute"
        days == 1L -> "morgen"
        days == 2L -> "übermorgen"
        days == -1L -> "gestern"
        days > 1 && days < 7 -> "in $days Tagen"
        days >= 7 && days < 14 -> "in einer Woche"
        days >= 14 && days < 31 -> "in ${days / 7} Wochen"
        days >= 31 -> "in ${LocalDate.from(today).until(target).toTotalMonths().coerceAtLeast(1)} Monaten"
        else -> "vor ${-days} Tagen"
    }
}

/** Kurzer Countdown fuer den naechsten Termin, z.B. "noch 2 Std 15 Min". */
fun countdown(millis: Long, now: Long = System.currentTimeMillis()): String {
    val duration = Duration.ofMillis(millis - now)
    if (duration.isNegative || duration.isZero) return "jetzt"

    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60

    return when {
        days > 0 -> "noch $days ${if (days == 1L) "Tag" else "Tage"}" +
            if (hours > 0) " $hours Std" else ""
        hours > 0 -> "noch $hours Std $minutes Min"
        else -> "noch $minutes Min"
    }
}

/** Menschlicher Text fuer eine Vorlaufzeit in Minuten. */
fun reminderLabel(minutes: Int): String = when {
    minutes <= 0 -> "zur Terminzeit"
    minutes < 60 -> "$minutes Minuten vorher"
    minutes == 60 -> "1 Stunde vorher"
    minutes < 1440 && minutes % 60 == 0 -> "${minutes / 60} Stunden vorher"
    minutes == 1440 -> "1 Tag vorher"
    minutes % 1440 == 0 -> "${minutes / 1440} Tage vorher"
    else -> "${minutes / 60} Std ${minutes % 60} Min vorher"
}

val REMINDER_PRESETS = listOf(0, 15, 30, 45, 60, 120, 180, 1440, 2880, 10080)
