package com.iyes.dacpressuremanager.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iyes.dacpressuremanager.data.local.DacDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DacMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DacDatabase::class.java,
    )

    @Test
    fun migrationOneToTwoDefaultsExistingRubyDataToRoomTemperature() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO profiles
                    (id, mode, name, referenceCenti, measuredCenti, sortOrder)
                VALUES
                    (1, 'RUBY', 'Ruby #1', 69424, 69524, 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO history_records
                    (id, profileId, createdAtEpochMillis, referenceCenti,
                     measuredCenti, pressureCenti)
                VALUES
                    (1, 1, 1000, 69424, 69524, 276)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_NAME,
            2,
            true,
            DacDatabase.MIGRATION_1_2,
        )
        migrated.query("SELECT temperatureK FROM profiles WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(298, cursor.getInt(0))
        }
        migrated.query("SELECT temperatureK FROM history_records WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(298, cursor.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "dac-migration-test"
    }
}
