package com.clayboicardi.claudewidget

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Exercises the real launch chain: which Intent each tap builds, and the order it falls
 * back in when an attempt can't be started. Robolectric supplies real Intent/Uri classes;
 * [FakeDevice] decides which Intents "exist" on the device.
 *
 * What this cannot cover: whether Claude lands on the right screen, and whether the system
 * allows the launch. Those are on-device checks (docs/route-probe.md).
 */
@RunWith(RobolectricTestRunner::class)
class ClaudeLauncherTest {

    private val claude = Destinations.CLAUDE_PKG
    private val app: Context = RuntimeEnvironment.getApplication()

    /** Records every startActivity call; only Intents accepted by [resolves] start. */
    private class FakeDevice(
        base: Context,
        private val resolves: (Intent) -> Boolean,
        private val denies: (Intent) -> Boolean = { false },
    ) : ContextWrapper(base) {
        val attempted = mutableListOf<Intent>()
        val started = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            attempted += intent
            if (denies(intent)) throw SecurityException("denied by test")
            if (!resolves(intent)) throw ActivityNotFoundException("no handler in test")
            started += intent
        }
    }

    private fun isClaudeLink(i: Intent) = i.action == Intent.ACTION_VIEW && i.scheme == "claude"
    private fun isLauncher(i: Intent) = i.action == Intent.ACTION_MAIN
    private fun isMarket(i: Intent) = i.scheme == "market"
    private fun isHttps(i: Intent) = i.scheme == "https"

    /** Gives Claude a launcher activity so getLaunchIntentForPackage returns non-null. */
    private fun installClaudeLauncher() {
        val component = ComponentName(claude, "$claude.MainActivity")
        val pm = shadowOf(app.packageManager)
        pm.addActivityIfNotPresent(component)
        pm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
    }

    private fun tap(device: FakeDevice, destId: String?): Boolean = ClaudeLauncher.launch(device, destId)

    private val clearTask = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    @Test
    fun `chat tap opens a new chat, pinned to Claude, on a cleared task`() {
        val device = FakeDevice(app, resolves = ::isClaudeLink)
        tap(device, "chat")

        val intent = device.started.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("claude://claude.ai/new", intent.dataString)
        assertEquals(claude, intent.`package`)
        assertNull("the target stays implicit within Claude's package", intent.component)
        assertEquals(clearTask, intent.flags and clearTask)
        assertEquals("stops at the first attempt that starts", 1, device.attempted.size)
    }

    @Test
    fun `code tap opens claude code, pinned to Claude, on a cleared task`() {
        val device = FakeDevice(app, resolves = ::isClaudeLink)
        tap(device, "code")

        val intent = device.started.single()
        assertEquals("claude://code", intent.dataString)
        assertEquals(claude, intent.`package`)
        assertEquals(clearTask, intent.flags and clearTask)
    }

    @Test
    fun `deep links carry no extras and no URI grants`() {
        val device = FakeDevice(app, resolves = ::isClaudeLink)
        tap(device, "chat")

        val intent = device.started.single()
        assertNull(intent.extras)
        val grants = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        assertEquals(0, intent.flags and grants)
    }

    @Test
    fun `falls back to Claude's launcher when the deep link has no handler`() {
        installClaudeLauncher()
        val device = FakeDevice(app, resolves = ::isLauncher)
        tap(device, "code")

        val intent = device.started.single()
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertEquals(claude, intent.component?.packageName ?: intent.`package`)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(listOf("claude://code", null), device.attempted.map { it.dataString })
    }

    @Test
    fun `falls back to the Play Store app when Claude is not installed`() {
        val device = FakeDevice(app, resolves = ::isMarket)
        tap(device, "chat")

        val intent = device.started.single()
        assertEquals("market://details?id=$claude", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        // No launcher attempt is recorded: getLaunchIntentForPackage returns null first.
        assertEquals(listOf("claude://claude.ai/new", "market://details?id=$claude"), device.attempted.map { it.dataString })
    }

    @Test
    fun `falls back to the Play Store web page when there is no store app`() {
        val device = FakeDevice(app, resolves = ::isHttps)
        tap(device, "chat")

        val intent = device.started.single()
        assertEquals("https://play.google.com/store/apps/details?id=$claude", intent.dataString)
        assertNull("the web fallback is left to the user's browser", intent.`package`)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `does nothing and does not throw when nothing can be started`() {
        val device = FakeDevice(app, resolves = { false })
        val started = tap(device, "chat")

        assertFalse(started)
        assertTrue(device.started.isEmpty())
        assertEquals(3, device.attempted.size) // deep link, market, https
    }

    @Test
    fun `an unknown destination never fires a deep link`() {
        installClaudeLauncher()
        val device = FakeDevice(app, resolves = { true })
        tap(device, "not-a-real-destination")

        assertEquals(Intent.ACTION_MAIN, device.started.single().action)
        assertTrue(device.attempted.none(::isClaudeLink))
    }

    @Test
    fun `a missing destination parameter never fires a deep link`() {
        installClaudeLauncher()
        val device = FakeDevice(app, resolves = { true })
        tap(device, null)

        assertEquals(Intent.ACTION_MAIN, device.started.single().action)
        assertTrue(device.attempted.none(::isClaudeLink))
    }

    @Test
    fun `a denied deep link falls through to the launcher`() {
        installClaudeLauncher()
        val device = FakeDevice(app, resolves = { true }, denies = ::isClaudeLink)
        tap(device, "chat")

        assertEquals(Intent.ACTION_MAIN, device.started.single().action)
    }

    @Test
    fun `a denied Play Store app link still falls through to the web page`() {
        val device = FakeDevice(app, resolves = { true }, denies = { isClaudeLink(it) || isMarket(it) })
        tap(device, "chat")

        assertEquals(
            "https://play.google.com/store/apps/details?id=$claude",
            device.started.single().dataString,
        )
    }
}
