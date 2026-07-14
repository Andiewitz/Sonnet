package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.db.dao.PlayHistoryDao
import com.example.data.db.dao.PlaylistDao
import com.example.data.db.dao.TrackDao
import com.example.data.db.entity.PlayHistoryEntity
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistTrackCrossRef
import com.example.data.db.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlayHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
}
