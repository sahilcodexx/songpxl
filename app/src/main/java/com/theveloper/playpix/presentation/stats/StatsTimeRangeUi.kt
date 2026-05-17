package com.theveloper.playpix.presentation.stats

import androidx.annotation.StringRes
import com.theveloper.playpix.R
import com.theveloper.playpix.data.stats.StatsTimeRange

@StringRes
fun StatsTimeRange.displayNameRes(): Int = when (this) {
    StatsTimeRange.DAY -> R.string.presentation_batch_g_stats_range_today
    StatsTimeRange.WEEK -> R.string.presentation_batch_g_stats_range_week_to_date
    StatsTimeRange.MONTH -> R.string.presentation_batch_g_stats_range_month_to_date
    StatsTimeRange.YEAR -> R.string.presentation_batch_g_stats_range_year_to_date
    StatsTimeRange.ALL -> R.string.presentation_batch_g_stats_range_all_time
}
