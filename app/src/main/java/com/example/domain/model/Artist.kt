package com.example.domain.model

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkUri: String?,
    val tracks: List<Track>
)
