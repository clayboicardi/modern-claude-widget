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

    /** Code opens the Code / Remote Control list — the documented mobile route. */
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

    // PROVISIONAL until the Task 1 on-device probe runs (Pixel not yet connected).
    // `claude://new` is the leading probe candidate; the ClaudeHome -> PlayStore tail keeps
    // this safe even if the URI is wrong. Finalize the URI + label/confidence
    // (Chat vs "Open Claude", PROBED vs FALLBACK_ONLY) from docs/route-probe.md.
    private val chat = Destination(
        id = ClaudeAction.CHAT.id,
        label = "Chat",
        confidence = RouteConfidence.PROBED,
        attempts = listOf(
            LaunchAttempt.ViewUri("claude://new", CLAUDE_PKG),
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
