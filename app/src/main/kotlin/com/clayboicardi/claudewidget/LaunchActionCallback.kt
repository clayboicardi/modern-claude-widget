package com.clayboicardi.claudewidget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

object ActionKeys {
    val DEST = ActionParameters.Key<String>("dest")
}

/**
 * Walks a destination's ordered [LaunchAttempt]s and starts the first that resolves.
 *
 * PendingIntent identity is guaranteed by Glance keying each `actionRunCallback` on its
 * distinct [ActionKeys.DEST] parameter — we never mutate the route URI to force uniqueness
 * (that would break the Claude app's intent matching). Every Intent is built here from the
 * registry; we never forward a Parcelable Intent passed in extras.
 */
class LaunchActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val destination = Destinations.byIdOrFallback(parameters[ActionKeys.DEST])
        for (attempt in destination.attempts) {
            if (launch(context, attempt)) return
        }
    }

    private fun launch(context: Context, attempt: LaunchAttempt): Boolean = try {
        when (attempt) {
            // CLEAR_TASK is required, not just NEW_TASK: when Claude is already running, a
            // bare NEW_TASK (or NEW_TASK|CLEAR_TOP) just resumes the existing task on whatever
            // page it was on — the deep link is ignored. Only clearing the task makes Claude
            // re-run its deep-link routing, so each button deterministically lands on its
            // surface. Verified on-device (Pixel 10 Pro XL / API 37): see docs/route-probe.md.
            is LaunchAttempt.ViewUri -> start(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse(attempt.uri))
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
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** market:// first, then the https Play Store URL (browser fallback). */
    private fun launchPlayStore(context: Context): Boolean {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${Destinations.CLAUDE_PKG}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(context, market)) return true

        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${Destinations.CLAUDE_PKG}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, web)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
