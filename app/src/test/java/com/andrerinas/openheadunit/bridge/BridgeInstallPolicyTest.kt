package com.andrerinas.openheadunit.bridge

import com.andrerinas.openheadunit.bridge.BridgeInstallPolicy.Action
import com.andrerinas.openheadunit.bridge.BridgeInstallPolicy.Installed
import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeInstallPolicyTest {

    private val stockAllGo = Installed(versionCode = 1, isBridge = false)

    @Test
    fun `turned on, the bundled bridge replaces stock AllGo Android Auto`() {
        assertEquals(Action.INSTALL, BridgeInstallPolicy.decide(enabled = true, installed = stockAllGo, bundledVersionCode = 3))
    }

    @Test
    fun `an older bridge is upgraded and the same or newer one is left alone`() {
        assertEquals(Action.INSTALL, BridgeInstallPolicy.decide(true, Installed(2, isBridge = true), bundledVersionCode = 3))
        assertEquals(Action.NONE, BridgeInstallPolicy.decide(true, Installed(3, isBridge = true), bundledVersionCode = 3))
        assertEquals(Action.NONE, BridgeInstallPolicy.decide(true, Installed(4, isBridge = true), bundledVersionCode = 3))
    }

    @Test
    fun `turned off, an installed bridge is removed so AllGo comes back`() {
        assertEquals(Action.UNINSTALL, BridgeInstallPolicy.decide(enabled = false, installed = Installed(3, isBridge = true), bundledVersionCode = 3))
        assertEquals(Action.NONE, BridgeInstallPolicy.decide(enabled = false, installed = stockAllGo, bundledVersionCode = 3))
    }

    @Test
    fun `a car without AllGo Android Auto or a build without the bridge does nothing`() {
        // Installing it fresh would need no system app underneath to fall back to.
        assertEquals(Action.NONE, BridgeInstallPolicy.decide(enabled = true, installed = null, bundledVersionCode = 3))
        assertEquals(Action.NONE, BridgeInstallPolicy.decide(enabled = true, installed = stockAllGo, bundledVersionCode = null))
    }
}
