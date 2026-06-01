package com.example.data

import kotlinx.coroutines.flow.Flow

class MusicRepository(private val musicDao: MusicDao) {

    val allSongs: Flow<List<Song>> = musicDao.getAllSongs()
    val allPlaylists: Flow<List<PlaylistWithSongs>> = musicDao.getAllPlaylistsWithSongs()

    fun searchSongs(query: String): Flow<List<Song>> {
        return musicDao.searchSongs("%$query%")
    }

    suspend fun getSongById(id: String): Song? {
        return musicDao.getSongById(id)
    }

    suspend fun insertSong(song: Song) {
        musicDao.insertSong(song)
    }

    suspend fun insertSongs(songs: List<Song>) {
        musicDao.insertSongs(songs)
    }

    suspend fun deleteSong(song: Song) {
        musicDao.deleteSong(song)
    }

    suspend fun createPlaylist(name: String, description: String, creator: String, songIds: List<String>): Long {
        val playlist = Playlist(name = name, description = description, creator = creator)
        val playlistId = musicDao.insertPlaylist(playlist).toInt()
        musicDao.syncPlaylistSongs(playlistId, songIds)

        // Generate updated share code for local record
        val shareCode = generateShareCode(name, description, creator, songIds)
        musicDao.updatePlaylist(playlist.copy(id = playlistId, shareCode = shareCode))
        return playlistId.toLong()
    }

    suspend fun updatePlaylistWithSongs(
        playlistId: Int,
        name: String,
        description: String,
        creator: String,
        songIds: List<String>
    ) {
        val shareCode = generateShareCode(name, description, creator, songIds)
        val playlist = Playlist(
            id = playlistId,
            name = name,
            description = description,
            creator = creator,
            shareCode = shareCode
        )
        musicDao.updatePlaylist(playlist)
        musicDao.syncPlaylistSongs(playlistId, songIds)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        musicDao.deleteSongsFromPlaylist(playlist.id)
        musicDao.deletePlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: String) {
        musicDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String) {
        musicDao.deletePlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
    }

    fun generateShareCode(name: String, description: String, creator: String, songIds: List<String>): String {
        val cleanName = name.replace("|", " ").trim()
        val cleanDesc = description.replace("|", " ").trim().ifEmpty { "My Custom Playlist" }
        val cleanCreator = creator.replace("|", " ").trim().ifEmpty { "User" }
        return "PLAYLIST_SHARE:$cleanName|$cleanDesc|$cleanCreator|${songIds.joinToString(",")}"
    }

    suspend fun importPlaylistFromShareCode(shareCode: String): PlaylistWithSongs? {
        if (!shareCode.startsWith("PLAYLIST_SHARE:") && !shareCode.startsWith("PLAYLIST_EXPORT:")) {
            return null
        }
        val content = shareCode.substringAfter(":")
        val parts = content.split("|")
        if (parts.size < 4) return null

        val name = parts[0]
        val desc = parts[1]
        val creator = parts[2]
        val songIdsString = parts[3]
        val songIds = songIdsString.split(",").filter { it.isNotEmpty() }

        // Create the imported playlist
        val playlistId = createPlaylist(
            name = "[Imported] $name",
            description = desc,
            creator = creator,
            songIds = songIds
        )

        // Find the songs that we may have preloaded, otherwise we can wait for them
        return PlaylistWithSongs(
            playlist = Playlist(id = playlistId.toInt(), name = "[Imported] $name", description = desc, creator = creator, shareCode = shareCode),
            songs = emptyList() // The DAO relations will resolve this nicely when queried through Room
        )
    }
}
