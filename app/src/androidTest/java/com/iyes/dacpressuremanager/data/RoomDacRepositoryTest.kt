package com.iyes.dacpressuremanager.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyes.dacpressuremanager.data.local.DacDatabase
import com.iyes.dacpressuremanager.domain.CommandResult
import com.iyes.dacpressuremanager.domain.DacDataState
import com.iyes.dacpressuremanager.domain.DacSnapshot
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureMode
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDacRepositoryTest {
    private lateinit var database: DacDatabase
    private lateinit var scopeJob: CompletableJob
    private lateinit var scope: CoroutineScope
    private lateinit var repository: RoomDacRepository
    private val timestamp = AtomicLong(0)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DacDatabase::class.java).build()
        scopeJob = SupervisorJob()
        scope = CoroutineScope(scopeJob + Dispatchers.IO)
        repository = RoomDacRepository(
            database = database,
            applicationScope = scope,
            now = { timestamp.incrementAndGet() },
        )
    }

    @After
    fun tearDown() = runBlocking {
        scopeJob.cancelAndJoin()
        database.close()
    }

    @Test
    fun initializesIndependentModesAndPersistsProfileOperations() = runBlocking {
        val initial = awaitSnapshot()
        assertEquals(PressureMode.DIAMOND, initial.currentMode)
        assertEquals("Diamond #1", initial.activeProfile(PressureMode.DIAMOND)?.name)
        assertEquals("Ruby #1", initial.activeProfile(PressureMode.RUBY)?.name)

        repository.setCurrentMode(PressureMode.RUBY)
        val rubyId = repository.addProfile("Ruby sample")
        repository.renameProfile(rubyId, "Ruby renamed")
        repository.adjustValue(rubyId, MeasurementField.MEASURED, 1)

        val updated = awaitSnapshot {
            it.currentMode == PressureMode.RUBY &&
                it.activeProfile(PressureMode.RUBY)?.name == "Ruby renamed" &&
                it.activeProfile(PressureMode.RUBY)?.measuredCenti == 69_425
        }
        assertEquals(133_300, updated.activeProfile(PressureMode.DIAMOND)?.measuredCenti)

        repository.moveProfile(rubyId, 0)
        val reordered = awaitSnapshot {
            it.profilesFor(PressureMode.RUBY).first().id == rubyId
        }
        assertEquals(rubyId, reordered.profilesFor(PressureMode.RUBY).first().id)

        assertEquals(CommandResult.Success, repository.deleteProfile(rubyId))
        val remainingRuby = awaitSnapshot { it.profilesFor(PressureMode.RUBY).size == 1 }
        assertEquals(
            CommandResult.KeepOneProfile,
            repository.deleteProfile(requireNotNull(remainingRuby.activeProfile(PressureMode.RUBY)).id),
        )
    }

    @Test
    fun ignoresOutOfBoundsAdjustmentsAndResetCopiesReference() = runBlocking {
        val profile = requireNotNull(awaitSnapshot().activeProfile())
        repository.adjustValue(
            profile.id,
            MeasurementField.REFERENCE,
            PressureMode.DIAMOND.minimumValueCenti - profile.referenceCenti,
        )
        repository.adjustValue(profile.id, MeasurementField.REFERENCE, -1)
        repository.adjustValue(profile.id, MeasurementField.MEASURED, 100)
        repository.resetMeasured(profile.id)

        val updated = awaitSnapshot {
            it.activeProfile()?.referenceCenti == 100_000 &&
                it.activeProfile()?.measuredCenti == 100_000
        }
        assertEquals(100_000, updated.activeProfile()?.referenceCenti)
    }

    @Test
    fun acceptsEveryModeBoundaryAndIgnoresOneCentiBeyondIt() = runBlocking {
        val diamond = requireNotNull(awaitSnapshot().activeProfile(PressureMode.DIAMOND))
        repository.adjustValue(
            diamond.id,
            MeasurementField.REFERENCE,
            PressureMode.DIAMOND.maximumValueCenti - diamond.referenceCenti,
        )
        repository.adjustValue(diamond.id, MeasurementField.REFERENCE, 1)
        assertEquals(
            PressureMode.DIAMOND.maximumValueCenti,
            awaitSnapshot {
                it.activeProfile(PressureMode.DIAMOND)?.referenceCenti ==
                    PressureMode.DIAMOND.maximumValueCenti
            }.activeProfile(PressureMode.DIAMOND)?.referenceCenti,
        )

        repository.setCurrentMode(PressureMode.RUBY)
        val ruby = requireNotNull(
            awaitSnapshot { it.currentMode == PressureMode.RUBY }
                .activeProfile(PressureMode.RUBY),
        )
        repository.adjustValue(
            ruby.id,
            MeasurementField.MEASURED,
            PressureMode.RUBY.minimumValueCenti - ruby.measuredCenti,
        )
        repository.adjustValue(ruby.id, MeasurementField.MEASURED, -1)
        assertEquals(
            PressureMode.RUBY.minimumValueCenti,
            awaitSnapshot {
                it.activeProfile(PressureMode.RUBY)?.measuredCenti ==
                    PressureMode.RUBY.minimumValueCenti
            }.activeProfile(PressureMode.RUBY)?.measuredCenti,
        )
    }

    @Test
    fun historyIsCappedRestorableClearableAndCalibrationProtected() = runBlocking {
        val profileId = requireNotNull(awaitSnapshot().activeProfile()).id
        repeat(51) {
            assertEquals(CommandResult.Success, repository.saveHistory(profileId))
        }
        val capped = awaitSnapshot {
            val history = it.historyFor(profileId)
            history.size == 50 &&
                history.firstOrNull()?.createdAtEpochMillis == 51L
        }
        assertEquals(50, capped.historyFor(profileId).size)
        assertEquals(51L, capped.historyFor(profileId).first().createdAtEpochMillis)
        assertEquals(2L, capped.historyFor(profileId).last().createdAtEpochMillis)

        repository.adjustValue(profileId, MeasurementField.MEASURED, 6_700)
        val recordToRestore = capped.historyFor(profileId).first()
        repository.restoreHistory(recordToRestore.id)
        assertEquals(
            recordToRestore.measuredCenti,
            awaitSnapshot { it.activeProfile()?.measuredCenti == recordToRestore.measuredCenti }
                .activeProfile()?.measuredCenti,
        )

        repository.adjustValue(profileId, MeasurementField.MEASURED, 51_700)
        assertTrue(repository.saveHistory(profileId) is CommandResult.PressureOutOfRange)
        assertEquals(50, awaitSnapshot().historyFor(profileId).size)

        repository.clearHistory(profileId)
        assertTrue(awaitSnapshot { it.historyFor(profileId).isEmpty() }.historyFor(profileId).isEmpty())
    }

    @Test
    fun deletingProfileCascadesItsHistory() = runBlocking {
        awaitSnapshot()
        val secondId = repository.addProfile("Second")
        repository.saveHistory(secondId)
        awaitSnapshot { it.historyFor(secondId).size == 1 }

        repository.deleteProfile(secondId)
        val afterDelete = awaitSnapshot {
            it.profiles.none { profile -> profile.id == secondId } &&
                it.historyFor(secondId).isEmpty()
        }
        assertTrue(afterDelete.historyFor(secondId).isEmpty())
    }

    @Test
    fun fileDatabaseRestoresModeActiveProfileValuesAndHistoryAfterReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "dac-restart-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        val firstDatabase = Room.databaseBuilder(
            context,
            DacDatabase::class.java,
            databaseName,
        ).build()
        val firstJob = SupervisorJob()
        val firstScope = CoroutineScope(firstJob + Dispatchers.IO)
        val firstRepository = RoomDacRepository(
            database = firstDatabase,
            applicationScope = firstScope,
            now = { 42L },
        )
        var savedProfileId = 0L
        try {
            awaitSnapshot(firstRepository)
            firstRepository.setCurrentMode(PressureMode.RUBY)
            savedProfileId = firstRepository.addProfile("Persistent Ruby")
            firstRepository.adjustValue(
                savedProfileId,
                MeasurementField.MEASURED,
                1,
            )
            firstRepository.saveHistory(savedProfileId)
            awaitSnapshot(firstRepository) {
                it.currentMode == PressureMode.RUBY &&
                    it.activeProfile(PressureMode.RUBY)?.id == savedProfileId &&
                    it.historyFor(savedProfileId).size == 1
            }
        } finally {
            firstJob.cancelAndJoin()
            firstDatabase.close()
        }

        val reopenedDatabase = Room.databaseBuilder(
            context,
            DacDatabase::class.java,
            databaseName,
        ).build()
        val reopenedJob = SupervisorJob()
        val reopenedScope = CoroutineScope(reopenedJob + Dispatchers.IO)
        val reopenedRepository = RoomDacRepository(
            database = reopenedDatabase,
            applicationScope = reopenedScope,
        )
        try {
            val restored = awaitSnapshot(reopenedRepository) {
                it.currentMode == PressureMode.RUBY
            }
            assertEquals(savedProfileId, restored.activeProfile(PressureMode.RUBY)?.id)
            assertEquals(
                "Persistent Ruby",
                restored.activeProfile(PressureMode.RUBY)?.name,
            )
            assertEquals(
                PressureMode.RUBY.defaultValueCenti + 1,
                restored.activeProfile(PressureMode.RUBY)?.measuredCenti,
            )
            assertEquals(1, restored.historyFor(savedProfileId).size)
        } finally {
            reopenedJob.cancelAndJoin()
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private suspend fun awaitSnapshot(
        source: DacRepository = repository,
        condition: (DacSnapshot) -> Boolean = { true },
    ): DacSnapshot = withTimeout(5_000) {
        source.dataState
            .filterIsInstance<DacDataState.Ready>()
            .first { condition(it.snapshot) }
            .snapshot
    }
}
