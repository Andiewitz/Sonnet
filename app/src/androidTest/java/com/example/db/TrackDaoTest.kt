package com.example.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.AppDatabase
import com.example.data.db.dao.TrackDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var trackDao: TrackDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        trackDao = database.trackDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetTrack() = runBlocking {
        // Stub
    }
}
