package com.andrerinas.openheadunit.app

import android.content.Intent

/** Which boot broadcasts may start AapService. */
object BootStartPolicy {
    /** One boot sends all three; only the first to find the user unlocked should act. */
    private val ONCE_PER_BOOT = setOf(
        Intent.ACTION_LOCKED_BOOT_COMPLETED,
        Intent.ACTION_USER_UNLOCKED,
        Intent.ACTION_BOOT_COMPLETED,
    )

    fun isOncePerBoot(action: String): Boolean = action in ONCE_PER_BOOT

    fun shouldStart(action: String, startedThisBoot: Boolean): Boolean =
        !isOncePerBoot(action) || !startedThisBoot
}
