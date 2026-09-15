package com.lodgy.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lodgy.app.R

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

internal enum class DurationUnit { TODAY, DAYS, MONTHS, YEARS, YEARS_MONTHS }

internal data class DurationValue(val unit: DurationUnit, val primary: Int, val secondary: Int = 0)

/**
 * Buckets a span into the coarse unit a warden would say out loud - 30-day months, 365-day years,
 * not calendar-accurate. Pulled out of [durationLabel] so the bucketing itself, the part actually
 * worth getting right, is plain and testable without a Composition (LODGY-92).
 */
internal fun durationValue(fromMillis: Long, toMillis: Long): DurationValue {
    val days = ((toMillis - fromMillis).coerceAtLeast(0L)) / DAY_MILLIS
    return when {
        days < 1 -> DurationValue(DurationUnit.TODAY, 0)
        days < 30 -> DurationValue(DurationUnit.DAYS, days.toInt())
        days < 365 -> DurationValue(DurationUnit.MONTHS, (days / 30).toInt())
        else -> {
            val years = (days / 365).toInt()
            val months = ((days % 365) / 30).toInt()
            if (months == 0) {
                DurationValue(DurationUnit.YEARS, years)
            } else {
                DurationValue(DurationUnit.YEARS_MONTHS, years, months)
            }
        }
    }
}

/** "3 months", "1 year 2 months", "Today" - a rough, warden-facing span between two dates, the
 *  same "how long" a tenant asking about a rent increase or a deposit gets answered with. */
@Composable
fun durationLabel(fromMillis: Long, toMillis: Long): String {
    val value = durationValue(fromMillis, toMillis)
    return when (value.unit) {
        DurationUnit.TODAY -> stringResource(R.string.duration_today)
        DurationUnit.DAYS -> stringResource(R.string.duration_days, value.primary)
        DurationUnit.MONTHS -> stringResource(R.string.duration_months, value.primary)
        DurationUnit.YEARS -> stringResource(R.string.duration_years, value.primary)
        DurationUnit.YEARS_MONTHS -> stringResource(R.string.duration_years_months, value.primary, value.secondary)
    }
}
