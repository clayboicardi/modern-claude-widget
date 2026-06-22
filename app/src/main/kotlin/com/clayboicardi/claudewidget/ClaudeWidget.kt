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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// Phosphor-green brand accent used for all pill/button text + glyph tint.
private val BrandGreen = ColorProvider(Color(0xFF08FF08))

class ClaudeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { ClaudeWidgetContent() }
    }
}

/** A click action routed through [LaunchActionCallback]'s ordered-attempt registry walk
 *  (preserves fail-closed + Play Store fallback + the verified BAL launch). */
private fun destClick(destId: String): Action =
    actionRunCallback<LaunchActionCallback>(actionParametersOf(ActionKeys.DEST to destId))

/**
 * Clayboicardi-brand reskin: blue-black shell with purple->green gradients, phosphor-green
 * text/glyphs, CC logo in the pill. Glance has no gradient modifier, so gradients ship as
 * layer-list shape drawables applied via ImageProvider backgrounds (corners baked in for
 * pre-API-31 clipping; .cornerRadius() kept as belt-and-suspenders on 31+). Launch wiring is
 * unchanged — only the visuals changed.
 */
@Composable
internal fun ClaudeWidgetContent() {
    val showLabels = LocalSize.current.width >= 220.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_shell_bg))
            .cornerRadius(28.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Input pill -> new chat
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(48.dp)
                .background(ImageProvider(R.drawable.widget_pill_bg))
                .cornerRadius(22.dp)
                .padding(horizontal = 16.dp)
                .clickable(destClick(ClaudeAction.CHAT.id)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.cc_logo),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "Ask Claude…",
                style = TextStyle(color = BrandGreen, fontSize = 15.sp),
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            LaunchButton(R.drawable.ic_chat, "Chat", ClaudeAction.CHAT.id, showLabels, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            LaunchButton(R.drawable.ic_code, "Code", ClaudeAction.CODE.id, showLabels, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun LaunchButton(
    iconRes: Int,
    label: String,
    destId: String,
    showLabel: Boolean,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .background(ImageProvider(R.drawable.widget_button_bg))
            .cornerRadius(16.dp)
            .clickable(destClick(destId)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Glyph is green-tinted inside the vector (android:tint), so no per-call tint needed.
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            modifier = GlanceModifier.size(22.dp),
        )
        if (showLabel) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = label,
                style = TextStyle(color = BrandGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}
