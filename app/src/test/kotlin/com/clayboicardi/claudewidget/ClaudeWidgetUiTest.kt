package com.clayboicardi.claudewidget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasText
import org.junit.Test

class ClaudeWidgetUiTest {

    // Asserts the pill + both buttons exist AND each carries a click action. Per the spec,
    // this verifies attachment only — actual launch + Claude's resulting screen stay an
    // on-device acceptance check (Task 5 Step 10).
    @Test
    fun renders_pill_and_two_buttons_each_with_a_click_action() = runGlanceAppWidgetUnitTest {
        provideComposable { ClaudeWidgetContent() }

        onNode(hasText("Ask Claude…")).assert(hasClickAction())
        onNode(hasText("Chat")).assert(hasClickAction())
        onNode(hasText("Code")).assert(hasClickAction())
    }
}
