package com.conference.asmara.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.conference.asmara.db.ScheduleDatabase

actual fun createTestDriver(): SqlDriver =
    NativeSqliteDriver(
        schema = ScheduleDatabase.Schema,
        name = "test.db",
        onConfiguration = { it.copy(inMemory = true) },
    )
