package com.allgo.app.androidauto

import android.os.Parcel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Read back exactly as AllGo's RemoteUIMediaPlaybackMetadata / RemoteUIMediaPlaybackInfo do. */
@RunWith(RobolectricTestRunner::class)
class MediaFeedParcelsTest {

    @Test
    fun `now playing is written in the order AllGo's media browser reads it`() {
        val art = byteArrayOf(1, 2, 3)
        val parcel = Parcel.obtain()
        try {
            MediaFeedParcels.writeMetadata(parcel, NowPlaying("Song", "Album", "Artist", art, "List", durationMs = 200_000, rating = 0))
            parcel.setDataPosition(0)
            assertEquals(1, parcel.readInt()) // non-null marker
            assertEquals("Song", parcel.readString())
            assertEquals("Album", parcel.readString())
            assertEquals("Artist", parcel.readString())
            assertEquals(3, parcel.readInt())
            val read = ByteArray(parcel.readInt())
            parcel.readByteArray(read)
            assertArrayEquals(art, read)
            assertEquals("List", parcel.readString())
            assertEquals(200_000, parcel.readInt())
            assertEquals(0, parcel.readInt())
            assertEquals(0, parcel.dataAvail())
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `missing art goes as an empty image, never null`() {
        // AllGo's reader sizes a byte array from the stream; a null array would crash it.
        val parcel = Parcel.obtain()
        try {
            MediaFeedParcels.writeMetadata(parcel, NowPlaying(null, null, null, null, null, 0, 0))
            parcel.setDataPosition(0)
            parcel.readInt()
            assertEquals("", parcel.readString())
            assertEquals("", parcel.readString())
            assertEquals("", parcel.readString())
            assertEquals(0, parcel.readInt())
            assertEquals(0, parcel.readInt())
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `play state is written in the order AllGo's media browser reads it`() {
        val parcel = Parcel.obtain()
        try {
            MediaFeedParcels.writePlaybackInfo(parcel, PlayState(state = 2, mediaSource = "Spotify", positionMs = 61_000, shuffle = 1, repeat = 0, repeatOne = 0))
            parcel.setDataPosition(0)
            assertEquals(1, parcel.readInt())
            assertEquals(2, parcel.readInt())
            assertEquals("Spotify", parcel.readString())
            assertEquals(61_000, parcel.readInt())
            assertEquals(1, parcel.readInt())
            assertEquals(0, parcel.readInt())
            assertEquals(0, parcel.readInt())
            assertEquals(1, parcel.readInt()) // elapsed time available
            assertEquals(0, parcel.dataAvail())
        } finally {
            parcel.recycle()
        }
    }
}
