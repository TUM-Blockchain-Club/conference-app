package com.conference.asmara.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.conference.asmara.db.ScheduleDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(ScheduleDatabase.Schema, "schedule.db")
}
