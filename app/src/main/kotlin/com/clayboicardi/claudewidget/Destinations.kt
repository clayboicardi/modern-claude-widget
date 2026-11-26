package com.clayboicardi.claudewidget

/** An ordered launch attempt. The UI does not care which kind a destination uses. */
sealed interface LaunchAttempt {
    data class ViewUri(val uri: String, val packageName: String) : LaunchAttempt
    data object ClaudeHome : LaunchAttempt   // getLaunchIntentForPackage
    data object PlayStore : LaunchAttempt    // market://details?id=...  (web fallback handled in launcher)
}

enum class RouteConfidence { DOCUMENTED, PROBED, FALLBACK_ONLY }

data class Destination(
    val id: String,
    val label: String,
    val confidence: RouteConfidence,
    val attempts: List<LaunchAttempt>,
)

object Destinations {
    const val CLAUDE_PKG = "com.anthropic.claude"

    /** Code opens the Code session list: the documented mobile route, re-probed 2026-11-26. */
    private val code = Destination(
        id = ClaudeAction.CODE.id,
        label = ClaudeAction.CODE.defaultLabel,
        confidence = RouteConfidence.DOCUMENTED,
        attempts = listOf(
            LaunchAttempt.ViewUri("claude://code", CLAUDE_PKG),
            LaunchAttempt.ClaudeHome,
            LaunchAttempt.PlayStore,
        ),
    )

    // Chat opens a new conversation composer in one tap (keyboard up). Probed on a Pixel 10
    // Pro XL, Android 17 QPR2, against Claude 1.261123.19 (2026-11-26). The shorter
    // claude://new worked in June 2026 but by November landed on the Code list instead.
    // This form is the one Anthropic documents (for Desktop). See docs/route-probe.md.
    private val chat = Destination(
        id = ClaudeAction.CHAT.id,
        label = "Chat",
        confidence = RouteConfidence.PROBED,
        attempts = listOf(
            LaunchAttempt.ViewUri("claude://claude.ai/new", CLAUDE_PKG),
            LaunchAttempt.ClaudeHome,
            LaunchAttempt.PlayStore,
        ),
    )

    private val byId: Map<String, Destination> =
        listOf(chat, code).associateBy { it.id }

    /** Fail closed: unknown/missing id resolves to a safe Claude-launcher destination. */
    fun byIdOrFallback(id: String?): Destination =
        byId[id] ?: Destination(
            id = "fallback",
            label = "Open Claude",
            confidence = RouteConfidence.FALLBACK_ONLY,
            attempts = listOf(LaunchAttempt.ClaudeHome, LaunchAttempt.PlayStore),
        )
}
