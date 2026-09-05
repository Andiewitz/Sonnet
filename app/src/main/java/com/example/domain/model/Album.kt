package com.example.domain.model

data class Album(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val trackCount: Int,
    val tracks: List<Track>
)
