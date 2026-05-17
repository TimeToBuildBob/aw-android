package net.activitywatch.android

import android.content.Intent
import android.view.View
import android.util.Log
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.IOException

private const val TAG = "ScreenshotTest"

/*
 * When this test is executed via gradle managed devices, the saved image files will be stored at
 * build/outputs/managed_device_android_test_additional_output/debugAndroidTest/managedDevice/nexusOneApi30/
 */
class ScreenshotTest {
    // a handy JUnit rule that stores the method name, so it can be used to generate unique
    // screenshot files per test method
    @get:Rule
    var permissionRule = GrantPermissionRule.grant(android.Manifest.permission.PACKAGE_USAGE_STATS)

    @get:Rule
    var nameRule = TestName()

    @Test
    fun testScreenshot() {
        saveDeviceScreenBitmap()
    }

    /**
     * Captures and saves an image of the entire device screen to storage.
     */
    @Throws(IOException::class)
    fun saveDeviceScreenBitmap() {
        Log.i(TAG, "Running saveDeviceScreenBitmap")
        AWPreferences(InstrumentationRegistry.getInstrumentation().targetContext).setFirstTimeRunFlag()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use {
            waitForForegroundUi()
            Log.i(TAG, "Taking screenshot")

            val bitmap = takeScreenshot()
            // Only supported on API levels >=28
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
            } else {
                Log.i(TAG, "Screenshot not saved to test storage, API level too low")
            }
        }
        Log.i(TAG, "Took screenshot!")
    }

    private fun waitForForegroundUi(timeoutMs: Long = 10_000, pollMs: Long = 100) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val activity = currentResumedActivity()
            val ready = when (activity) {
                is MainActivity -> isMainActivityReady(activity)
                null -> false
                else -> isViewLaidOut(activity.window?.decorView?.findViewById(android.R.id.content))
            }
            if (ready) {
                return
            }
            Thread.sleep(pollMs)
        }

        throw AssertionError("Timed out waiting for foreground UI to become ready")
    }

    private fun currentResumedActivity(): android.app.Activity? {
        var current: android.app.Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            current = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
        }
        return current
    }

    private fun isMainActivityReady(activity: MainActivity): Boolean {
        val webView = activity.findViewById<WebView?>(R.id.webview)
        return when {
            webView != null -> webView.progress == 100 && webView.contentHeight > 0 && !webView.url.isNullOrBlank()
            else -> isViewLaidOut(activity.window?.decorView?.findViewById(android.R.id.content))
        }
    }

    private fun isViewLaidOut(view: View?): Boolean {
        return view != null && view.isLaidOut && view.width > 0 && view.height > 0
    }
}
