package com.example.player

import androidx.media3.common.Player
import com.example.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers queue navigation, shuffle and repeat semantics.
 *
 * These are regression tests for the shuffle and repeat-mode defects recorded
 * in docs/AUDIT.md (findings C1 and H1). The pre-fix implementation:
 *  - returned `otherIndices.firstOrNull()` from `peekNextTrack`, so shuffle
 *    always selected the second track in the queue;
 *  - picked a *different* track in `getNextTrack`, so a crossfade could
 *    preview one track and play another;
 *  - coerced every repeat mode except `REPEAT_MODE_ONE` to `REPEAT_MODE_ALL`,
 *    making "no repeat" unreachable.
 */
class QueueManagerTest {

    private fun tracks(count: Int): List<Track> = (1L..count.toLong()).map { id ->
        Track(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            albumArtUri = null,
            durationMs = 180_000L,
            uri = "content://media/external/audio/media/$id"
        )
    }

    // ---------------------------------------------------------------- shuffle

    @Test
    fun shuffle_coversWholeQueueBeforeRepeating() {
        val qm = QueueManager()
        val queue = tracks(10)
        qm.setRepeatMode(Player.REPEAT_MODE_OFF)
        qm.setShuffleMode(true)
        qm.setPlaylist(queue, 0)

        // queue[0] is already playing and already counted as played.
        val played = mutableListOf(queue[0].id)
        var current = queue[0]
        var next: Track? = qm.getNextTrack(current)
        // Bounded so a regression fails an assertion instead of looping until
        // the heap is exhausted (which is what the old shuffle did).
        while (next != null && played.size <= queue.size) {
            played += next!!.id
            current = next!!
            next = qm.getNextTrack(current)
        }

        assertNull("a non-repeating shuffle must terminate at the end of the queue", next)
        // A non-repeating shuffle must visit every track exactly once and then
        // stop. The old implementation always produced the second track.
        assertEquals("shuffle must play every track exactly once", 10, played.size)
        assertEquals("shuffle must not repeat a track within a pass", 10, played.toSet().size)
    }

    @Test
    fun shuffle_doesNotAlwaysReturnSecondTrackInQueue() {
        val qm = QueueManager()
        val queue = tracks(10)
        qm.setRepeatMode(Player.REPEAT_MODE_OFF)
        qm.setShuffleMode(true)
        qm.setPlaylist(queue, 0)

        var current = queue[0]
        val chosen = mutableSetOf<Long>()
        // 9 transitions cover the whole queue; the old code returned queue[1] every time.
        repeat(9) {
            val next = qm.getNextTrack(current)!!
            chosen += next.id
            current = next
        }

        assertTrue(
            "shuffle must not be limited to a single track, got $chosen",
            chosen.size > 1
        )
    }

    @Test
    fun peekAndGet_agree_whenShuffleEnabled() {
        // The crossfade path peeks a track and only commits once the fade-out
        // finishes, so the two calls must resolve to the same track.
        repeat(25) {
            val qm = QueueManager()
            val queue = tracks(10)
            qm.setShuffleMode(true)
            qm.setPlaylist(queue, 0)

            val peeked = qm.peekNextTrack(queue[0])
            val got = qm.getNextTrack(queue[0])

            assertNotNull("peek must resolve a track", peeked)
            assertEquals("peek and get must agree under shuffle", peeked!!.id, got!!.id)
        }
    }

    @Test
    fun sequentialShuffle_doesNotRepeatTrackConsecutively() {
        val qm = QueueManager()
        val queue = tracks(6)
        qm.setShuffleMode(true) // repeat defaults to ALL, so the pass wraps
        qm.setPlaylist(queue, 0)

        var current = queue[0]
        repeat(200) {
            val next = qm.getNextTrack(current)
            assertNotNull("shuffled queue must never run dry under repeat-all", next)
            assertNotEquals(
                "shuffle must not replay the current track immediately",
                current.id, next!!.id
            )
            current = next
        }
    }

    @Test
    fun shuffle_then_disable_preservesQueueOrder() {
        val qm = QueueManager()
        val queue = tracks(4)
        qm.setShuffleMode(true)
        qm.setPlaylist(queue, 0)
        qm.setShuffleMode(false)

        var current = qm.getNextTrack(queue[0])!!
        assertEquals(queue[1].id, current.id)
        current = qm.getNextTrack(current)!!
        assertEquals(queue[2].id, current.id)
    }

    // ---------------------------------------------------------------- repeat

    @Test
    fun toggleRepeat_cyclesAllThreeModes() {
        val qm = QueueManager()
        assertEquals(Player.REPEAT_MODE_ALL, qm.repeatMode.value)

        assertEquals(Player.REPEAT_MODE_ONE, qm.toggleRepeat())
        assertEquals(Player.REPEAT_MODE_OFF, qm.toggleRepeat())
        assertEquals(Player.REPEAT_MODE_ALL, qm.toggleRepeat())

        // Three toggles is a full cycle, so the mode is back where it started.
        assertEquals(Player.REPEAT_MODE_ALL, qm.repeatMode.value)
    }

    @Test
    fun setRepeatMode_off_isPreserved() {
        val qm = QueueManager()
        qm.setRepeatMode(Player.REPEAT_MODE_OFF)
        assertEquals(Player.REPEAT_MODE_OFF, qm.repeatMode.value)

        qm.setRepeatMode(Player.REPEAT_MODE_ONE)
        assertEquals(Player.REPEAT_MODE_ONE, qm.repeatMode.value)
    }

    @Test
    fun setRepeatMode_unknownValue_fallsBackToAll() {
        val qm = QueueManager()
        qm.setRepeatMode(99)
        assertEquals(Player.REPEAT_MODE_ALL, qm.repeatMode.value)
    }

    @Test
    fun repeatOff_getNextTrack_pastEnd_returnsNull() {
        val qm = QueueManager()
        val queue = tracks(3)
        qm.setRepeatMode(Player.REPEAT_MODE_OFF)
        qm.setPlaylist(queue, 0)

        assertEquals(queue[1].id, qm.getNextTrack(queue[0])!!.id)
        assertEquals(queue[2].id, qm.getNextTrack(queue[1])!!.id)

        assertNull("queue must end rather than wrap under repeat-off", qm.getNextTrack(queue[2]))
        assertNull(qm.peekNextTrack(queue[2]))
    }

    @Test
    fun repeatAll_wrapsToStartAtEndOfQueue() {
        val qm = QueueManager()
        val queue = tracks(3)
        qm.setPlaylist(queue, 0)

        qm.getNextTrack(queue[0])
        qm.getNextTrack(queue[1])
        assertEquals(queue[0].id, qm.getNextTrack(queue[2])!!.id)
    }

    @Test
    fun repeatOne_returnsSameTrack() {
        val qm = QueueManager()
        val queue = tracks(3)
        qm.setRepeatMode(Player.REPEAT_MODE_ONE)
        qm.setPlaylist(queue, 1)

        assertEquals(queue[1].id, qm.peekNextTrack(queue[1])!!.id)
        assertEquals(queue[1].id, qm.getNextTrack(queue[1])!!.id)
    }

    // -------------------------------------------------------- non-shuffled order

    @Test
    fun nonShuffle_preservesQueueOrder() {
        val qm = QueueManager()
        val queue = tracks(4)
        qm.setPlaylist(queue, 0)

        var current = queue[0]
        repeat(3) { step ->
            val next = qm.getNextTrack(current)!!
            assertEquals(queue[step + 1].id, next.id)
            current = next
        }
    }

    // ---------------------------------------------------------------- previous

    @Test
    fun previous_updatesCurrentIndex() {
        val qm = QueueManager()
        val queue = tracks(3)
        qm.setPlaylist(queue, 2)

        val previous = qm.getPreviousTrack(queue[2])
        assertNotNull(previous)
        assertEquals(queue[1].id, previous!!.id)
        assertEquals(1, qm.currentTrackIndex)
    }

    @Test
    fun previous_atQueueStart_repeatOff_doesNotRestartCurrentTrack() {
        val qm = QueueManager()
        val queue = tracks(3)
        qm.setRepeatMode(Player.REPEAT_MODE_OFF)
        qm.setPlaylist(queue, 0)

        assertNull(
            "previous at the start of the queue must not replay the current track",
            qm.getPreviousTrack(queue[0])
        )
        assertEquals(0, qm.currentTrackIndex)
    }

    @Test
    fun previous_atQueueStart_repeatAll_wrapsToLastTrack() {
        val qm = QueueManager()
        val queue = tracks(3)
        qm.setPlaylist(queue, 0)

        val previous = qm.getPreviousTrack(queue[0])
        assertNotNull(previous)
        assertEquals(queue[2].id, previous!!.id)
        assertEquals(2, qm.currentTrackIndex)
    }

    // ------------------------------------------------------------ edge cases

    @Test
    fun emptyQueue_isSafe() {
        val qm = QueueManager()
        assertNull(qm.getNextTrack(null))
        assertNull(qm.peekNextTrack(null))
        assertNull(qm.getPreviousTrack(null))
        assertNull(qm.setPlaylist(emptyList()))
    }

    @Test
    fun singleTrackQueue_neverRunsDry() {
        val qm = QueueManager()
        val queue = tracks(1)
        qm.setPlaylist(queue, 0)

        assertEquals(queue[0].id, qm.getNextTrack(queue[0])!!.id)
        assertEquals(queue[0].id, qm.getNextTrack(queue[0])!!.id)
    }

    @Test
    fun setPlaylist_clampsOutOfRangeStartIndex() {
        val qm = QueueManager()
        val queue = tracks(3)

        assertEquals(queue[2].id, qm.setPlaylist(queue, 99)!!.id)
        assertEquals(2, qm.currentTrackIndex)

        assertEquals(queue[0].id, qm.setPlaylist(queue, -5)!!.id)
        assertEquals(0, qm.currentTrackIndex)
    }

    @Test
    fun setCurrentTrack_movesIndexToThatTrack() {
        val qm = QueueManager()
        val queue = tracks(4)
        qm.setPlaylist(queue, 0)

        qm.setCurrentTrack(queue[3])
        assertEquals(3, qm.currentTrackIndex)
        assertEquals(queue[0].id, qm.getNextTrack(queue[3])!!.id) // wraps under repeat-all
    }

    private fun assertTrue(message: String, condition: Boolean) =
        org.junit.Assert.assertTrue(message, condition)
}
