package com.iyes.dacpressuremanager

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iyes.dacpressuremanager.data.DacRepository
import com.iyes.dacpressuremanager.data.RoomDacRepository
import com.iyes.dacpressuremanager.data.local.DacDatabase
import com.iyes.dacpressuremanager.update.AppUpdateChecker
import com.iyes.dacpressuremanager.update.GitHubReleaseUpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: DacDatabase = Room.databaseBuilder(
        context = context.applicationContext,
        klass = DacDatabase::class.java,
        name = "dac-pressure-manager.db",
    ).setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .addMigrations(DacDatabase.MIGRATION_1_2)
        .build()

    val repository: DacRepository = RoomDacRepository(
        database = database,
        applicationScope = applicationScope,
        preferences = context.applicationContext.getSharedPreferences(
            "dac-ui-preferences",
            Context.MODE_PRIVATE,
        ),
    )

    val updateChecker: AppUpdateChecker = GitHubReleaseUpdateChecker()
}
