package com.allgo.app.androidauto

import com.allgo.app.androidauto.HubCommands.Command
import org.junit.Assert.assertEquals
import org.junit.Test

class HubCommandsTest {

    @Test
    fun `dock buttons open the matching Android Auto screen`() {
        // SAIC launcher/SystemUI call launchApp(1 music, 2 maps, 3 phone) through the hub; these are
        // the Android Auto keys stock AllGo sends (MEDIA, NAVIGATION, TEL).
        assertEquals(listOf(Command.ShowProjection, Command.Key(65537)), HubCommands.launchApp(1))
        assertEquals(listOf(Command.ShowProjection, Command.Key(65538)), HubCommands.launchApp(2))
        assertEquals(listOf(Command.ShowProjection, Command.Key(65540)), HubCommands.launchApp(3))
        assertEquals(listOf(Command.ShowProjection, Command.Key(3)), HubCommands.launchApp(0))
        assertEquals(listOf(Command.ShowProjection), HubCommands.launchApp(99))
    }

    @Test
    fun `car media controls become Android Auto media keys, once per press`() {
        assertEquals(Command.Key(126), HubCommands.mediaKey(1))
        assertEquals(Command.Key(127), HubCommands.mediaKey(2))
        assertEquals(Command.Key(87), HubCommands.mediaKey(3))
        assertEquals(Command.Key(88), HubCommands.mediaKey(4))
        assertEquals(Command.Key(87), HubCommands.mediaKey(7))
        assertEquals(Command.Key(88), HubCommands.mediaKey(8))
        assertEquals(Command.Key(85), HubCommands.mediaKey(19))
        assertEquals(Command.Key(85), HubCommands.mediaKey(20))
        // Releases: the press already sent a full key, so nothing more.
        assertEquals(null, HubCommands.mediaKey(11))
        assertEquals(null, HubCommands.mediaKey(12))
        assertEquals(null, HubCommands.mediaKey(21))
        assertEquals(null, HubCommands.mediaKey(42))
    }

    @Test
    fun `the assistant request toggles Google Assistant both ways, as stock`() {
        // Stock sends the search key for start and stop alike; the car's "close mic" did nothing here.
        assertEquals(Command.Key(84), HubCommands.assistant(start = true))
        assertEquals(Command.Key(84), HubCommands.assistant(start = false))
    }
}
