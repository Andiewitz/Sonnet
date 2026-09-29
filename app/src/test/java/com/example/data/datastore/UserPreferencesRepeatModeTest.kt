package com.example.data.datastore

import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the repeat-mode persistence defect in
 * docs/AUDIT.md (finding H1).
 *
 * `UserPreferencesDataStore` previously coerced anything that was not
 * `REPEAT_MODE_ONE` to `REPEAT_MODE_ALL`, on both write and read. A user who
 * selected "no repeat" therefore got "repeat all" back on the next launch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesRepeatModeTest {

    private fun store(): UserPreferencesDataStore =
        UserPreferencesDataStore(ApplicationProvider.getApplicationContext())

    @Test
    fun repeatOff_roundTripsThroughPersistence() = runTest {
        val ds = store()
        ds.setRepeatMode(Player.REPEAT_MODE_OFF)
        assertEquals(Player.REPEAT_MODE_OFF, ds.repeatMode.first())
    }

    @Test
    fun repeatOne_roundTripsThroughPersistence() = runTest {
        val ds = store()
        ds.setRepeatMode(Player.REPEAT_MODE_ONE)
        assertEquals(Player.REPEAT_MODE_ONE, ds.repeatMode.first())
    }

    @Test
    fun repeatAll_roundTripsThroughPersistence() = runTest {
        val ds = store()
        ds.setRepeatMode(Player.REPEAT_MODE_ALL)
        assertEquals(Player.REPEAT_MODE_ALL, ds.repeatMode.first())
    }

    @Test
    fun unknownStoredValue_fallsBackToAll() = runTest {
        val ds = store()
        ds.setRepeatMode(42)
        assertEquals(Player.REPEAT_MODE_ALL, ds.repeatMode.first())
    }

    @Test
    fun queueManagerAcceptsEveryPersistedMode() = runTest {
        val ds = store()
        val qm = com.example.player.QueueManager()

        ds.setRepeatMode(Player.REPEAT_MODE_OFF)
        qm.setRepeatMode(ds.repeatMode.first())
        assertEquals(Player.REPEAT_MODE_OFF, qm.repeatMode.value)

        ds.setRepeatMode(Player.REPEAT_MODE_ONE)
        qm.setRepeatMode(ds.repeatMode.first())
        assertEquals(Player.REPEAT_MODE_ONE, qm.repeatMode.value)
    }
}
