package com.conference.asmara.ui.common

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The conference happens in one place, so every session time is rendered in
 * that place's zone. Using the device zone instead would shift session times
 * for anyone travelling and could split a single conference day across two
 * day headers.
 */
val ConferenceTimeZone: TimeZone = TimeZone.of("Europe/Berlin")

// Formatted by hand rather than through DateTimeFormat, which is still
// experimental in kotlinx-datetime 0.7.1. English only; i18n is out of scope.

fun LocalTime.hhmm(): String = "${hour.pad2()}:${minute.pad2()}"

/** e.g. "Tuesday, 1 September". */
fun LocalDate.dayLabel(): String = "${dayOfWeek.fullName()}, $day ${month.fullName()}"

fun Instant.timeIn(zone: TimeZone): LocalTime = toLocalDateTime(zone).time

fun Instant.dateIn(zone: TimeZone): LocalDate = toLocalDateTime(zone).date

/** e.g. "10:00 – 10:45". */
fun timeRangeLabel(start: Instant, end: Instant, zone: TimeZone = ConferenceTimeZone): String =
    "${start.timeIn(zone).hhmm()} – ${end.timeIn(zone).hhmm()}"

/** e.g. "14 Aug, 09:12" — for the "last updated" line on the offline banner. */
fun Instant.syncedAtLabel(zone: TimeZone = ConferenceTimeZone): String {
    val dateTime = toLocalDateTime(zone)
    return "${dateTime.date.day} ${dateTime.date.month.shortName()}, ${dateTime.time.hhmm()}"
}

private fun Int.pad2(): String = if (this < 10) "0$this" else toString()

private fun DayOfWeek.fullName(): String = when (this) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

private fun Month.fullName(): String = when (this) {
    Month.JANUARY -> "January"
    Month.FEBRUARY -> "February"
    Month.MARCH -> "March"
    Month.APRIL -> "April"
    Month.MAY -> "May"
    Month.JUNE -> "June"
    Month.JULY -> "July"
    Month.AUGUST -> "August"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "December"
}

private fun Month.shortName(): String = fullName().take(3)
