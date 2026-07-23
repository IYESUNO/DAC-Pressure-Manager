package com.iyes.dacpressuremanager

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iyes.dacpressuremanager.data.DacRepository
import com.iyes.dacpressuremanager.data.RoomDacRepository
import com.iyes.dacpressuremanager.data.local.DacDatabase
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
        .build()

    val repository: DacRepository = RoomDacRepository(
        database = database,
        applicationScope = applicationScope,
    )
}
