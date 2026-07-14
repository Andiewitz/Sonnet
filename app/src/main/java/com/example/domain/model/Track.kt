package com.example.domain.model

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtUri: String?,
    val durationMs: Long,
    val uri: String
)
