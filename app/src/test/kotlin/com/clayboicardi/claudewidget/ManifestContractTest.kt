package com.clayboicardi.claudewidget

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Pins the product's promises against the MERGED manifest, which includes whatever the
 * libraries add. A dependency bump that brings a new permission or a new unguarded exported
 * component fails here instead of shipping unnoticed.
 */
@RunWith(RobolectricTestRunner::class)
class ManifestContractTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun packageInfo(flags: Int): PackageInfo =
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(flags.toLong()),
        )

    @Test
    fun `the app never requests INTERNET`() {
        val requested = packageInfo(PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(
            "README promises no INTERNET permission; a dependency must not add it",
            requested.contains("android.permission.INTERNET"),
        )
    }

    @Test
    fun `requested permissions are exactly the ones Glance's WorkManager needs`() {
        // The app's own manifest requests nothing. These come from androidx.work (pulled in
        // by Glance) plus AndroidX Core's signature-level receiver permission.
        val expected = setOf(
            "android.permission.WAKE_LOCK",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.FOREGROUND_SERVICE",
            "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        val requested = packageInfo(PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        assertEquals(expected, requested)
    }

    @Test
    fun `the widget receiver is the only exported component without a permission guard`() {
        val info = packageInfo(
            PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS,
        )
        val unguarded = buildList {
            info.activities.orEmpty().filter { it.exported && it.permission == null }.mapTo(this) { it.name }
            info.receivers.orEmpty().filter { it.exported && it.permission == null }.mapTo(this) { it.name }
            info.services.orEmpty().filter { it.exported && it.permission == null }.mapTo(this) { it.name }
            info.providers.orEmpty()
                .filter { it.exported && it.readPermission == null && it.writePermission == null }
                .mapTo(this) { it.name }
        }
        assertEquals(listOf(ClaudeWidgetReceiver::class.java.name), unguarded)
    }
}
