package com.clayboicardi.claudewidget

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle

/**
 * Invisible hop between a widget tap and Claude. It reads which button was tapped, runs the
 * ordered launch chain, and finishes at once: it never draws anything.
 *
 * Why an Activity and not Glance's `actionRunCallback`: a callback runs inside a broadcast,
 * and starting an activity from a broadcast is a background activity launch. On Android 17
 * QPR2 that launch was allowed when the tap came from the Pixel launcher but silently blocked
 * (`BAL_BLOCK`) when it came from Kvaesitso, so the widget did nothing there. A tap that
 * starts an Activity is the ordinary widget path, and an Activity the user just opened may
 * start other activities. Evidence: docs/route-probe.md, November 2026.
 *
 * Not exported. Only the widget's own PendingIntent can reach it, and it trusts nothing from
 * the Intent except a destination id that is looked up in [Destinations] (unknown ids fail
 * closed to "open Claude").
 */
// Lint reads "Launch" in the class name as a home-made splash screen. It is not one.
@SuppressLint("CustomSplashScreen")
class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ClaudeLauncher.launch(this, intent?.getStringExtra(ActionKeys.DEST.name))
        // Theme.NoDisplay requires finishing before onResume returns.
        finish()
    }
}
