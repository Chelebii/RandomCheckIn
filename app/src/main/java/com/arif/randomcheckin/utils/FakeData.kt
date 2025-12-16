package com.arif.randomcheckin.utils

import com.arif.randomcheckin.data.model.Goal

object FakeData {

    fun getGoals(): List<Goal> {
        return listOf(
            Goal(
                id = "1",
                title = "Günlük spor",
                description = "Her gün en az 20 dakika hareket",
                endDate = "31.12.2025"
            )
        )
    }
}
