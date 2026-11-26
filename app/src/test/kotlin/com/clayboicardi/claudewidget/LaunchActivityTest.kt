package com.clayboicardi.claudewidget

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * The hop a widget tap goes through. Checks that it reads the destination the way Glance
 * delivers it (a string extra named after [ActionKeys.DEST]), launches, and always finishes.
 */
@RunWith(RobolectricTestRunner::class)
class LaunchActivityTest {

    private val app = RuntimeEnvironment.getApplication()
    private val claude = Destinations.CLAUDE_PKG

    private fun tapIntent(destId: String?) = Intent(app, LaunchActivity::class.java).apply {
        if (destId != null) putExtra(ActionKeys.DEST.name, destId)
    }

    private fun installClaudeLauncher() {
        val component = ComponentName(claude, "$claude.MainActivity")
        val pm = shadowOf(app.packageManager)
        pm.addActivityIfNotPresent(component)
        pm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
    }

    @Test
    fun `a code tap starts the code deep link and finishes`() {
        val controller = Robolectric.buildActivity(LaunchActivity::class.java, tapIntent("code")).create()

        val started = shadowOf(controller.get()).nextStartedActivity
        assertEquals("claude://code", started.dataString)
        assertEquals(claude, started.`package`)
        assertTrue("must finish in onCreate: Theme.NoDisplay", controller.get().isFinishing)
    }

    @Test
    fun `a chat tap starts the new-chat deep link and finishes`() {
        val controller = Robolectric.buildActivity(LaunchActivity::class.java, tapIntent("chat")).create()

        assertEquals("claude://claude.ai/new", shadowOf(controller.get()).nextStartedActivity.dataString)
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `it starts exactly one activity per tap`() {
        val controller = Robolectric.buildActivity(LaunchActivity::class.java, tapIntent("chat")).create()

        val shadow = shadowOf(controller.get())
        shadow.nextStartedActivity
        assertNull("the chain must stop at the first attempt that starts", shadow.nextStartedActivity)
    }

    @Test
    fun `a tap with no destination opens Claude's launcher, never a deep link`() {
        installClaudeLauncher()
        val controller = Robolectric.buildActivity(LaunchActivity::class.java, tapIntent(null)).create()

        val started = shadowOf(controller.get()).nextStartedActivity
        assertEquals(Intent.ACTION_MAIN, started.action)
        assertNull(started.data)
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `an unknown destination opens Claude's launcher, never a deep link`() {
        installClaudeLauncher()
        val controller =
            Robolectric.buildActivity(LaunchActivity::class.java, tapIntent("not-a-real-destination")).create()

        assertEquals(Intent.ACTION_MAIN, shadowOf(controller.get()).nextStartedActivity.action)
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `the destination extra is the only thing it reads from the intent`() {
        // A foreign Intent smuggled in as an extra must never be started or forwarded.
        val hostile = Intent(Intent.ACTION_VIEW).setClassName("com.example.evil", "com.example.evil.Steal")
        val intent = tapIntent("code").putExtra("android.intent.extra.INTENT", hostile)
        val controller = Robolectric.buildActivity(LaunchActivity::class.java, intent).create()

        val shadow = shadowOf(controller.get())
        assertEquals("claude://code", shadow.nextStartedActivity.dataString)
        assertNull(shadow.nextStartedActivity)
    }
}
