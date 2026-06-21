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
}
