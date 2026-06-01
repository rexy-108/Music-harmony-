package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MusicViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Music Harmony", appName)
  }

  @Test
  fun `launch MainActivity`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `play a song in viewModel`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    val database = com.example.data.MusicDatabase.getInstance(context, dbScope)
    val repository = com.example.data.MusicRepository(database.musicDao())
    val geminiService = com.example.data.GeminiSongService()
    
    val viewModel = MusicViewModel(context as android.app.Application, repository, geminiService)
    
    val testSong = com.example.data.Song(
        id = "test_song",
        title = "Test Song",
        singer = "Test Singer",
        album = "Test Album",
        durationSeconds = 120,
        genre = "Pop",
        lyrics = "Test lyrics",
        imageUrl = "",
        synthPattern = "pop_beat"
    )
    
    viewModel.playSong(testSong, listOf(testSong))
    assertEquals("test_song", viewModel.currentPlayingSong.value?.id)
    assertEquals(true, viewModel.isPlaying.value)
  }
}

