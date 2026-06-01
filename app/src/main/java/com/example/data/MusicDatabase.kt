package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_harmony_db"
                )
                .addCallback(MusicDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class MusicDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.musicDao()
                    preloadSongs(dao)
                    preloadPlaylists(dao)
                }
            }
        }

        private suspend fun preloadSongs(dao: MusicDao) {
            val hits = listOf(
                Song(
                    id = "hit_shape_of_you",
                    title = "Shape of You",
                    singer = "Ed Sheeran",
                    album = "÷ (Divide)",
                    durationSeconds = 233,
                    genre = "Pop",
                    lyrics = "The club isn't the best place to find a lover\nSo the bar is where I go\nMe and my friends at the table doing shots\nDrinking fast and then we talk slow...\n\nI'm in love with the shape of you\nWe push and pull like a magnet do\nAlthough my heart is falling too\nI'm in love with your body...",
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                    synthPattern = "pop_beat",
                    isPreloaded = true
                ),
                Song(
                    id = "hit_blinding_lights",
                    title = "Blinding Lights",
                    singer = "The Weeknd",
                    album = "After Hours",
                    durationSeconds = 200,
                    genre = "Synthwave",
                    lyrics = "Yeah, I've been on my own for long enough\nMaybe you can show me how to love, maybe\nI'm going through withdrawals\nYou don't even have to do too much...\n\nI look around and Sin City's cold and empty\nNo one's around to judge me\nI can't see clearly when you're gone\nI said, ooh, I'm blinded by the lights...",
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                    synthPattern = "synthwave",
                    isPreloaded = true
                ),
                Song(
                    id = "hit_someone_like_you",
                    title = "Someone Like You",
                    singer = "Adele",
                    album = "21",
                    durationSeconds = 285,
                    genre = "Ballad",
                    lyrics = "I heard that you're settled down\nThat you found a girl and you're married now\nI heard that your dreams came true\nGuess she gave you things I didn't give to you...\n\nNever mind, I'll find someone like you\nI wish nothing but the best for you, too\nDon't forget me, I beg, I remember you said\n'Sometimes it lasts in love, but sometimes it hurts instead'...",
                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
                    synthPattern = "ballad_chords",
                    isPreloaded = true
                ),
                Song(
                    id = "hit_river_flows",
                    title = "River Flows in You",
                    singer = "Yiruma",
                    album = "First Love",
                    durationSeconds = 188,
                    genre = "Classical",
                    lyrics = "[Instrumental masterpiece]\nA peaceful melody of flowing piano keys. Let the music flow through your mind like a tranquil river.",
                    imageUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=300",
                    synthPattern = "lullaby",
                    isPreloaded = true
                ),
                Song(
                    id = "hit_butter",
                    title = "Butter",
                    singer = "BTS",
                    album = "Butter",
                    durationSeconds = 164,
                    genre = "K-Pop",
                    lyrics = "Smooth like butter, like a criminal undercover\nGon' pop like trouble breaking into your heart like that\nCool shade, stunner, yeah, I owe it all to my mother\nHot like summer, yeah, I'm making you sweat like that...\n\nGet it, let it, roll!",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                    synthPattern = "pop_beat",
                    isPreloaded = true
                ),
                Song(
                    id = "hit_bohemian_rhapsody",
                    title = "Bohemian Rhapsody",
                    singer = "Queen",
                    album = "A Night at the Opera",
                    durationSeconds = 354,
                    genre = "Rock",
                    lyrics = "Is this the real life?\nIs this just fantasy?\nCaught in a landslide, no escape from reality\nOpen your eyes, look up to the skies and see...\n\nMama, oooh, didn't mean to make you cry\nIf I'm not back again this time tomorrow\nCarry on, carry on as if nothing really matters...",
                    imageUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7a4c3?w=300",
                    synthPattern = "ballad_chords",
                    isPreloaded = true
                ),
                Song(
                    id = "hit_ambient_space",
                    title = "Cosmic Meditation",
                    singer = "Siri Ambient",
                    album = "Nebula Dream",
                    durationSeconds = 300,
                    genre = "Ambient",
                    lyrics = "[Deep meditation sounds]\nFloat through space and stars. Experience weightless harmony as synthesizers play beautiful waves of sound.",
                    imageUrl = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=300",
                    synthPattern = "ambient",
                    isPreloaded = true
                )
            )
            dao.insertSongs(hits)
        }

        private suspend fun preloadPlaylists(dao: MusicDao) {
            val moodMix = Playlist(
                id = 1,
                name = "Global Hits Starter",
                description = "Unmissable hits and timeless melodies from around the world.",
                creator = "Music Harmony",
                isShared = true,
                shareCode = "PLAYLIST_EXPORT:Global Hits Starter|A curated mix for newcomers|Music Harmony|hit_shape_of_you,hit_blinding_lights,hit_someone_like_you,hit_butter,hit_bohemian_rhapsody"
            )
            val chillMix = Playlist(
                id = 2,
                name = "Midnight Chill & Relax",
                description = "Tranquil ambient soundscapes and gentle piano to help you wind down.",
                creator = "Harmony AI",
                isShared = true,
                shareCode = "PLAYLIST_EXPORT:Midnight Chill & Relax|Relaxing night vibes|Harmony AI|hit_river_flows,hit_ambient_space"
            )

            dao.insertPlaylist(moodMix)
            dao.insertPlaylist(chillMix)

            // Cross references
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(1, "hit_shape_of_you"))
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(1, "hit_blinding_lights"))
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(1, "hit_someone_like_you"))
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(1, "hit_butter"))
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(1, "hit_bohemian_rhapsody"))

            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(2, "hit_river_flows"))
            dao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(2, "hit_ambient_space"))
        }
    }
}
