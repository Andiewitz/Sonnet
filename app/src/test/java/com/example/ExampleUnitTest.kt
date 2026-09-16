package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.datastore.UserPreferencesDataStore
import com.example.domain.model.Track
import com.example.player.AudioPlayer
import com.example.player.FadeController
import com.example.player.QueueManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    @Test
    fun testQueueManagerAdvanceAndPeek() {
        val qm = QueueManager()
        val tracks = listOf(
            Track(1, "Song 1", "Artist", "Album", null, 10000, "content://1"),
            Track(2, "Song 2", "Artist", "Album", null, 10000, "content://2")
        )
        qm.setPlaylist(tracks, 0)

        // Peek should not advance index
        val peeked = qm.peekNextTrack(tracks[0])
        assertNotNull(peeked)
        assertEquals(2L, peeked?.id)

        // Advance
        val next = qm.getNextTrack(tracks[0])
        assertNotNull(next)
        assertEquals(2L, next?.id)
    }

    @Test
    fun testFadeControllerCancelAndRestore() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ds = UserPreferencesDataStore(context)
        val player = AudioPlayer(context, ds)
        val track1 = Track(1, "Song 1", "Artist", "Album", null, 10000, "content://1")
        player.setPlaylist(listOf(track1), 0)

        player.fadeController.startFadeIn(500L)
        assertTrue(player.fadeController.isTransitioning.value)

        player.fadeController.cancelAndRestore()
        assertFalse(player.fadeController.isTransitioning.value)
        assertEquals(1.0f, player.player.volume, 0.01f)

        player.release()
    }
}
