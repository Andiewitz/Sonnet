package com.example.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "playlist_track",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long
)
