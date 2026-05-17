package com.theveloper.playpix.data

import com.theveloper.playpix.shared.WearLibraryItem

data class WearLocalQueueState(
    val items: List<WearLibraryItem> = emptyList(),
    val currentIndex: Int = -1,
)
