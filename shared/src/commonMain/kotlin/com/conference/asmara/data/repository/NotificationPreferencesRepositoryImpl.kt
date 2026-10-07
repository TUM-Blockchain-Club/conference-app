package com.conference.asmara.data.repository

import com.conference.asmara.data.local.AnnouncementLocalDataSource
import com.conference.asmara.domain.model.AnnouncementCategory
import com.conference.asmara.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow

class NotificationPreferencesRepositoryImpl(private val local: AnnouncementLocalDataSource) :
    NotificationPreferencesRepository {

    override fun observeEnabledCategories(): Flow<Set<AnnouncementCategory>> = local.observeEnabledCategories()

    override suspend fun setCategoryEnabled(category: AnnouncementCategory, enabled: Boolean) {
        if (!category.canOptOut) return
        local.setCategoryEnabled(category, enabled)
    }
}
