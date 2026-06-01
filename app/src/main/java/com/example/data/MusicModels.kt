package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded
import androidx.room.Junction

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String, // Can be UUID or Spotify or custom
    val title: String,
    val singer: String,
    val album: String = "Single",
    val durationSeconds: Int = 180,
    val genre: String = "Pop",
    val lyrics: String = "",
    val imageUrl: String = "",
    val synthPattern: String = "pop_beat", // pop_beat, ambient, synthwave, ballad_chords, lullaby
    val isPreloaded: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val creator: String = "Owner",
    val isShared: Boolean = false,
    val shareCode: String = "",
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Int,
    val songId: String
)

// Helper structure to fetch playlists with their songs
data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<Song>
)
