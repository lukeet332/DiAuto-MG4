package com.allgo.app.androidauto

/** What the hub's requests (AllGo `AndroidAutoIdentifiers` values) ask DiAuto to do. */
object HubCommands {

    sealed class Command {
        object ShowProjection : Command()
        object HideProjection : Command()
        /** A full press and release of an Android Auto key code. */
        data class Key(val keyCode: Int) : Command()
    }

    private const val KEY_HOME = 3
    private const val KEY_SEARCH = 84
    private const val KEY_PLAY_PAUSE = 85
    private const val KEY_NEXT = 87
    private const val KEY_PREVIOUS = 88
    private const val KEY_PLAY = 126
    private const val KEY_PAUSE = 127
    // Android Auto's own screen keys, as stock AllGo sends them.
    private const val KEY_MEDIA = 65537
    private const val KEY_NAVIGATION = 65538
    private const val KEY_TEL = 65540

    fun launchApp(app: Int): List<Command> {
        val key = when (app) {
            0 -> KEY_HOME
            1 -> KEY_MEDIA
            2 -> KEY_NAVIGATION
            3 -> KEY_TEL
            else -> null
        }
        return listOfNotNull(Command.ShowProjection, key?.let { Command.Key(it) })
    }

    /** Presses act; releases (11, 12, 21) are dropped because each press sends a whole key. */
    fun mediaKey(key: Int): Command.Key? = when (key) {
        1, 5 -> Command.Key(KEY_PLAY)
        2, 6 -> Command.Key(KEY_PAUSE)
        3, 7 -> Command.Key(KEY_NEXT)
        4, 8 -> Command.Key(KEY_PREVIOUS)
        19, 20 -> Command.Key(KEY_PLAY_PAUSE)
        else -> null
    }

    /** Stock sends the search key for start and stop alike: Android Auto toggles its assistant. */
    @Suppress("UNUSED_PARAMETER")
    fun assistant(start: Boolean): Command.Key = Command.Key(KEY_SEARCH)
}
