package com.clayboicardi.claudewidget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.glance.action.ActionParameters

object ActionKeys {
    val DEST = ActionParameters.Key<String>("dest")
}

/**
 * Walks a destination's ordered [LaunchAttempt]s and starts the first that resolves.
 *
 * Every Intent is built here from the registry, keyed by a plain string id. Nothing received
 * from outside is ever forwarded as an Intent, and an unknown id fails closed to "open Claude".
 *
 * Call this from an Activity context ([LaunchActivity]). Starting activities from the widget's
 * broadcast instead is blocked as a background activity launch on some launchers; see
 * docs/route-probe.md (November 2026).
 */
object ClaudeLauncher {

    /** @return true if some attempt started. */
    fun launch(context: Context, destId: String?): Boolean {
        val destination = Destinations.byIdOrFallback(destId)
        return destination.attempts.any { launch(context, it) }
    }

    private fun launch(context: Context, attempt: LaunchAttempt): Boolean =
        when (attempt) {
            // NEW_TASK|CLEAR_TASK gives each button the same result every time: Claude starts
            // on that surface whatever it was showing before. In June 2026 it was also the only
            // way to make a running Claude follow the link; by November Claude re-navigated
            // with NEW_TASK alone, but clearing still works and is verified on both app
            // generations. See docs/route-probe.md.
            is LaunchAttempt.ViewUri -> start(
                context,
                Intent(Intent.ACTION_VIEW, attempt.uri.toUri())
                    .setPackage(attempt.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )

            LaunchAttempt.ClaudeHome -> {
                val home = context.packageManager
                    .getLaunchIntentForPackage(Destinations.CLAUDE_PKG)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                home != null && start(context, home)
            }

            LaunchAttempt.PlayStore -> launchPlayStore(context)
        }

    /** market:// first, then the https Play Store URL (browser fallback). */
    private fun launchPlayStore(context: Context): Boolean {
        val market = Intent(
            Intent.ACTION_VIEW,
            "market://details?id=${Destinations.CLAUDE_PKG}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(context, market)) return true

        val web = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=${Destinations.CLAUDE_PKG}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, web)
    }

    /**
     * Starts one Intent and reports whether it started. Each start handles its own failures,
     * so a refused attempt (no handler, or a SecurityException from a non-exported or
     * permission-guarded target) always falls through to the next one.
     *
     * A launch the system blocks as a background activity start does NOT throw, so it can't
     * be detected here; that is why launches are verified on a device (docs/route-probe.md).
     */
    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
