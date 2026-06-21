package com.clayboicardi.claudewidget

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchUriTest {

    @Test
    fun `no query returns base unchanged`() {
        assertEquals("claude://code", LaunchUri.build("claude://code"))
    }

    @Test
    fun `empty query returns base unchanged`() {
        assertEquals("claude://code/new", LaunchUri.build("claude://code/new", ""))
    }

    @Test
    fun `spaces encode as percent-20`() {
        assertEquals(
            "claude://code/new?q=fix%20the%20bug",
            LaunchUri.build("claude://code/new", "fix the bug"),
        )
    }

    @Test
    fun `reserved characters are encoded`() {
        assertEquals(
            "claude://code/new?q=a%26b%3Dc",
            LaunchUri.build("claude://code/new", "a&b=c"),
        )
    }
}
