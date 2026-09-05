package com.example.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val artworkUris: List<String> = emptyList()
)
