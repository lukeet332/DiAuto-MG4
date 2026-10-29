package com.andrerinas.openheadunit.bridge

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.utils.AppLog
import java.io.File

/**
 * Installs the bundled AllGo bridge (assets/allgo-bridge.apk) as an update over the car's
 * AllGo Android Auto app, or removes that update when "Native car integration" is off.
 * The AllGo hub only binds the app when it starts, so either change takes effect after a restart.
 */
object AllGoBridgeInstaller {
    private const val PACKAGE = "com.allgo.app.androidauto"
    private const val ASSET = "allgo-bridge.apk"
    private const val BRIDGE_MARKER = "com.drivehub.diauto.bridge"
    private const val ACTION_RESULT = "com.drivehub.diauto.BRIDGE_INSTALL_RESULT"

    fun sync(context: Context) {
        val app = context.applicationContext
        Thread({
            try {
                syncNow(app)
            } catch (e: Exception) {
                AppLog.w("AllGoBridge: install sync failed: ${e.message}")
            }
        }, "allgo-bridge-install").start()
    }

    private fun syncNow(context: Context) {
        val enabled = App.provide(context).settings.nativeCarIntegration
        val bundled = bundledApk(context)
        val bundledVersion = bundled?.let { archiveVersionCode(context, it) }
        val action = BridgeInstallPolicy.decide(enabled, installed(context), bundledVersion)
        AppLog.i("AllGoBridge: native integration=$enabled bundled=$bundledVersion -> $action")
        when (action) {
            BridgeInstallPolicy.Action.INSTALL -> install(context, bundled!!)
            BridgeInstallPolicy.Action.UNINSTALL -> context.packageManager.packageInstaller.uninstall(PACKAGE, resultSender(context, 0))
            BridgeInstallPolicy.Action.NONE -> Unit
        }
        bundled?.delete()
    }

    private fun installed(context: Context): BridgeInstallPolicy.Installed? = try {
        val info = context.packageManager.getPackageInfo(PACKAGE, PackageManager.GET_META_DATA)
        val isBridge = info.applicationInfo?.metaData?.getBoolean(BRIDGE_MARKER) == true
        BridgeInstallPolicy.Installed(info.longVersionCode, isBridge)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun bundledApk(context: Context): File? = try {
        val out = File(context.cacheDir, ASSET)
        context.assets.open(ASSET).use { input -> out.outputStream().use { input.copyTo(it) } }
        out
    } catch (_: Exception) {
        null // phone builds carry no bridge
    }

    private fun archiveVersionCode(context: Context, apk: File): Long? =
        context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?.takeIf { it.packageName == PACKAGE }
            ?.longVersionCode

    private fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setSize(apk.length())
        // The MG4 refuses "non-system apps on internal storage" unless installFlags carries the hidden
        // INSTALL_INTERNAL (0x10), as ADK's installer found; setInstallLocation is not enough.
        try {
            val field = params.javaClass.getField("installFlags")
            field.setInt(params, field.getInt(params) or 0x10)
        } catch (e: Exception) {
            AppLog.w("AllGoBridge: cannot request an internal install: ${e.message}")
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            session.commit(resultSender(context, sessionId))
        }
    }

    private fun resultSender(context: Context, requestCode: Int) = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ResultReceiver::class.java).setAction(ACTION_RESULT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    ).intentSender

    class ResultReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            AppLog.i("AllGoBridge: install/uninstall result status=$status $message (takes effect after the car restarts)")
        }
    }
}
