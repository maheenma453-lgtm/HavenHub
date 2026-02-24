package com.example.havenhub.utils
import kotlinx.datetime.*
object DateUtils {

    // ── Today's date ──────────────────────────────────────────────────
    fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun todayAsString(): String = today().toString()

    // ── Days between two dates ────────────────────────────────────────
    fun daysBetween(startDate: String, endDate: String): Int {
        return try {
            LocalDate.parse(startDate).daysUntil(LocalDate.parse(endDate))
        } catch (e: Exception) {
            0
        }
    }

    // ── Is date in the past ───────────────────────────────────────────
    fun isPast(dateString: String): Boolean {
        return try {
            LocalDate.parse(dateString) < today()
        } catch (e: Exception) {
            false
        }
    }

    // ── Is date in the future ─────────────────────────────────────────
    fun isFuture(dateString: String): Boolean {
        return try {
            LocalDate.parse(dateString) > today()
        } catch (e: Exception) {
            false
        }
    }

    // ── Relative time label (e.g. "2 hrs ago") ───────────────────────
    fun getRelativeTime(instantString: String): String {
        return try {
            val past = Instant.parse(instantString)
            val diffSeconds = (Clock.System.now() - past).inWholeSeconds
            when {
                diffSeconds < 60     -> "Just now"
                diffSeconds < 3600   -> "${diffSeconds / 60} min ago"
                diffSeconds < 86400  -> "${diffSeconds / 3600} hrs ago"
                diffSeconds < 604800 -> "${diffSeconds / 86400} days ago"
                else                 -> instantString.take(10)
            }
        } catch (e: Exception) {
            instantString
        }
    }

    // ── Display format (e.g. "10 Nov 2024") ──────────────────────────
    fun toDisplayFormat(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
            "${date.dayOfMonth} $month ${date.year}"
        } catch (e: Exception) {
            dateString
        }
    }

    // ── Current timestamp ─────────────────────────────────────────────
    fun currentTimestamp(): String = Clock.System.now().toString()

    // ── Calculate total booking amount ────────────────────────────────
    fun calculateTotal(checkIn: String, checkOut: String, pricePerNight: Double): Double {
        return daysBetween(checkIn, checkOut) * pricePerNight
    }

    // ── Validate date range ───────────────────────────────────────────
    fun isValidDateRange(checkIn: String, checkOut: String): Boolean {
        return try {
            LocalDate.parse(checkOut) > LocalDate.parse(checkIn)
        } catch (e: Exception) {
            false
        }
    }
}
