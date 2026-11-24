package com.clayboicardi.claudewidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {

    @Test
    fun `code destination is documented with ordered attempts`() {
        val code = Destinations.byIdOrFallback("code")
        assertEquals(RouteConfidence.DOCUMENTED, code.confidence)
        assertEquals(
            listOf(
                LaunchAttempt.ViewUri("claude://code", "com.anthropic.claude"),
                LaunchAttempt.ClaudeHome,
                LaunchAttempt.PlayStore,
            ),
            code.attempts,
        )
    }

    @Test
    fun `chat destination is tried before falling back to ClaudeHome`() {
        val chat = Destinations.byIdOrFallback("chat")
        assertTrue(chat.attempts.first() is LaunchAttempt.ViewUri)
        assertEquals(LaunchAttempt.PlayStore, chat.attempts.last())
    }

    @Test
    fun `unknown id fails closed with no ViewUri`() {
        val d = Destinations.byIdOrFallback("does-not-exist")
        assertEquals(LaunchAttempt.ClaudeHome, d.attempts.first())
        assertTrue(d.attempts.none { it is LaunchAttempt.ViewUri })
    }

    @Test
    fun `null id fails closed`() {
        assertEquals("fallback", Destinations.byIdOrFallback(null).id)
    }

    // Guards the registry as a whole, so a ClaudeAction added later can't silently fall
    // through to the "Open Claude" fallback, and no route can point outside Claude.
    @Test
    fun `every action has a route that deep-links into Claude and ends at the Play Store`() {
        for (action in ClaudeAction.entries) {
            val destination = Destinations.byIdOrFallback(action.id)
            assertEquals("${action.name} is not registered", action.id, destination.id)

            val first = destination.attempts.first()
            assertTrue("${action.name} must try a deep link first", first is LaunchAttempt.ViewUri)
            first as LaunchAttempt.ViewUri
            assertEquals(Destinations.CLAUDE_PKG, first.packageName)
            assertTrue("${first.uri} is not a claude:// link", first.uri.startsWith("claude://"))

            assertEquals(
                listOf(LaunchAttempt.ClaudeHome, LaunchAttempt.PlayStore),
                destination.attempts.drop(1),
            )
        }
    }
}
