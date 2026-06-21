package com.clayboicardi.claudewidget

/** A widget action. Carries only identity + a default label; routing lives in [Destinations]. */
enum class ClaudeAction(val id: String, val defaultLabel: String) {
    CHAT("chat", "Chat"),
    CODE("code", "Code"),
}
