package com.andrerinas.openheadunit.bridge

/** Keeps the installed com.allgo.app.androidauto in step with the "Native car integration" setting. */
object BridgeInstallPolicy {

    enum class Action { NONE, INSTALL, UNINSTALL }

    data class Installed(val versionCode: Long, val isBridge: Boolean)

    /**
     * [installed] is null when the car has no AllGo Android Auto app; the bridge only ever goes
     * on as an update over it, so uninstalling the update always leaves AllGo behind.
     */
    fun decide(enabled: Boolean, installed: Installed?, bundledVersionCode: Long?): Action {
        if (installed == null) return Action.NONE
        if (!enabled) return if (installed.isBridge) Action.UNINSTALL else Action.NONE
        if (bundledVersionCode == null) return Action.NONE
        if (installed.isBridge && installed.versionCode >= bundledVersionCode) return Action.NONE
        return Action.INSTALL
    }
}
