package com.clayboicardi.claudewidget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.testing.unit.hasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import org.junit.Test

class ClaudeWidgetUiTest {

    // Asserts attachment only (per the spec): the pill + both icon buttons are three
    // independent click targets, the pill shows its hint, and each button has a name a
    // screen reader can speak. Actual launch + Claude's screen are on-device acceptance checks.
    //
    // The harness's default size is wide enough for labels, so here the name is the visible
    // text. (This used to assert an icon content description at this size; that duplicated
    // the label for screen readers. The icons-only case is asserted in the compact test.)
    @Test
    fun renders_pill_and_two_buttons_each_clickable() = runGlanceAppWidgetUnitTest {
        provideComposable { ClaudeWidgetContent() }

        onAllNodes(hasClickAction()).assertCountEquals(3)

        onNode(hasText("Ask Claude…")).assertExists()
        onNode(hasText("Chat")).assertExists()
        onNode(hasText("Code")).assertExists()
    }

    // Each target must run LaunchActionCallback with its own destination id. A copy-paste
    // slip that pointed Code at "chat" would still pass the click-count test above.
    @Test
    fun each_target_routes_to_its_own_destination() = runGlanceAppWidgetUnitTest {
        provideComposable { ClaudeWidgetContent() }

        fun routesTo(action: ClaudeAction) =
            hasRunCallbackClickAction<LaunchActionCallback>(actionParametersOf(ActionKeys.DEST to action.id))

        // Pill + Chat button both open a new chat; only the Code button opens Code.
        onAllNodes(routesTo(ClaudeAction.CHAT)).assertCountEquals(2)
        onAllNodes(routesTo(ClaudeAction.CODE)).assertCountEquals(1)
        onNode(hasText("Ask Claude…") and routesTo(ClaudeAction.CHAT)).assertDoesNotExist()
    }

    @Test
    fun standard_width_shows_text_labels() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(250.dp, 168.dp))
        provideComposable { ClaudeWidgetContent() }

        onNode(hasText("Chat")).assertExists()
        onNode(hasText("Code")).assertExists()
        // With a visible label the icon is decorative: one name per button, not two.
        onAllNodes(hasContentDescriptionEqualTo("Chat")).assertCountEquals(0)
        onAllNodes(hasContentDescriptionEqualTo("Code")).assertCountEquals(0)
    }

    @Test
    fun compact_width_hides_labels_but_keeps_all_three_targets() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(180.dp, 140.dp))
        provideComposable { ClaudeWidgetContent() }

        onNode(hasText("Chat")).assertDoesNotExist()
        onNode(hasText("Code")).assertDoesNotExist()
        onAllNodes(hasClickAction()).assertCountEquals(3)
        // Icons-only buttons still announce themselves to screen readers.
        onNode(hasContentDescriptionEqualTo("Chat")).assertExists()
        onNode(hasContentDescriptionEqualTo("Code")).assertExists()
    }
}
