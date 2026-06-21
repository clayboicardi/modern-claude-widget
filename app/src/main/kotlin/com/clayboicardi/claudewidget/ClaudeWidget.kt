package com.clayboicardi.claudewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// Original dark palette — inspired by, not copied from, the ChatGPT widget.
private val Container = Color(0xFF1C1C1E)
private val Pill = Color(0xFF2C2C2E)
private val IconBg = Color(0xFF3A3A3C)
private val OnSurface = Color(0xFFECECEC)
private val Muted = Color(0xFF8E8E93)

class ClaudeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { ClaudeWidgetContent() }
    }
}

private fun destClick(destId: String): Action =
    actionRunCallback<LaunchActionCallback>(actionParametersOf(ActionKeys.DEST to destId))

/**
 * Rounded dark container, a fake "Ask Claude…" input pill (-> new chat), and an icon-button
 * row (Chat, Code). Labels appear once the widget is wide enough (Standard 4x2); Compact 2x2
 * shows icons only. Each clickable container is its own >=48dp touch target.
 */
@Composable
internal fun ClaudeWidgetContent() {
    val showLabels = LocalSize.current.width >= 220.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Container))
            .cornerRadius(28.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Pill))
                .cornerRadius(22.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(destClick(ClaudeAction.CHAT.id)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ask Claude…",
                style = TextStyle(color = ColorProvider(Muted), fontSize = 15.sp),
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            IconAction(R.drawable.ic_chat, "Chat", showLabels, destClick(ClaudeAction.CHAT.id))
            Spacer(GlanceModifier.width(8.dp))
            IconAction(R.drawable.ic_code, "Code", showLabels, destClick(ClaudeAction.CODE.id))
        }
    }
}

@Composable
private fun RowScope.IconAction(
    iconRes: Int,
    label: String,
    showLabel: Boolean,
    onClick: Action,
) {
    Row(
        modifier = GlanceModifier
            .defaultWeight()
            .height(48.dp)
            .background(ColorProvider(IconBg))
            .cornerRadius(16.dp)
            .clickable(onClick)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(22.dp),
        )
        if (showLabel) {
            Spacer(GlanceModifier.width(8.dp))
            Text(text = label, style = TextStyle(color = ColorProvider(OnSurface), fontSize = 14.sp))
        }
    }
}
