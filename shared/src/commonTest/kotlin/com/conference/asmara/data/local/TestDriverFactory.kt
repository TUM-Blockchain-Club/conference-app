package com.conference.asmara.data.local

import app.cash.sqldelight.db.SqlDriver

/** In-memory driver for tests. One actual per test target — see androidHostTest/iosTest. */
expect fun createTestDriver(): SqlDriver
