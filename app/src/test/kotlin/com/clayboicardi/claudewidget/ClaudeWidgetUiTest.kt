package com.clayboicardi.claudewidget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import org.junit.Test

class ClaudeWidgetUiTest {

    // Asserts attachment only (per the spec): the pill + both icon buttons are three
    // independent click targets, the pill shows its hint, and each button carries its
    // content description. Actual launch + Claude's screen are on-device acceptance checks.
    @Test
    fun renders_pill_and_two_buttons_each_clickable() = runGlanceAppWidgetUnitTest {
        provideComposable { ClaudeWidgetContent() }

        onAllNodes(hasClickAction()).assertCountEquals(3)

        onNode(hasText("Ask Claude…")).assertExists()
        onNode(hasContentDescriptionEqualTo("Chat")).assertExists()
        onNode(hasContentDescriptionEqualTo("Code")).assertExists()
    }
}
