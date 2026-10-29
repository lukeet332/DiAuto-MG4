package com.allgo.app.androidauto

import android.app.Activity
import android.os.Bundle
import com.allgo.app.androidauto.HubCommands.Command

/** The car's Android Auto home tile opens this by name; hand straight over to DiAuto. */
class ProjectionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.init(this)
        Bridge.command(Command.ShowProjection)
        finish()
    }
}
