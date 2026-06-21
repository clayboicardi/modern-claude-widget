package com.clayboicardi.claudewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.text.Text

class ClaudeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { ClaudeWidgetContent() }
    }
}

/** A click action that routes to [destId] via [LaunchActionCallback]. Distinct DEST params
 *  give each button its own PendingIntent identity. */
private fun destClick(destId: String) =
    actionRunCallback<LaunchActionCallback>(actionParametersOf(ActionKeys.DEST to destId))

/**
 * Ugly first pass — pill + two text buttons. Top-level + internal so glance-testing can render
 * it directly. Styling (rounded container, icons, responsive sizing) lands in Task 6.
 */
@Composable
internal fun ClaudeWidgetContent() {
    Column(modifier = GlanceModifier) {
        Text(
            text = "Ask Claude…",
            modifier = GlanceModifier.clickable(destClick(ClaudeAction.CHAT.id)),
        )
        Row {
            Text(
                text = "Chat",
                modifier = GlanceModifier.clickable(destClick(ClaudeAction.CHAT.id)),
            )
            Text(
                text = "Code",
                modifier = GlanceModifier.clickable(destClick(ClaudeAction.CODE.id)),
            )
        }
    }
}
