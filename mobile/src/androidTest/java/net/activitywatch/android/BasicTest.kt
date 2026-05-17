package net.activitywatch.android

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.json.JSONArray
import java.time.Instant
import java.util.UUID

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class BasicTest {
    private fun newBucketId(): String = "test-${UUID.randomUUID()}"

    private fun hasEventWithData(events: JSONArray, key: String, value: String): Boolean {
        for (i in 0 until events.length()) {
            val event = events.getJSONObject(i)
            if (event.optJSONObject("data")?.optString(key) == value) {
                return true
            }
        }
        return false
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("net.activitywatch.android.debug", appContext.packageName)
    }

    @Test
    fun getBuckets() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val ri = RustInterface(appContext)
        val bucketId = newBucketId()
        ri.createBucket("""{"id": "$bucketId", "type": "test", "hostname": "test", "client": "test"}""")
        assertTrue("Expected bucket $bucketId to be present", ri.getBucketsJSON().has(bucketId))
    }

    @Test
    fun createHeartbeat() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val ri = RustInterface(appContext)
        val bucketId = newBucketId()
        ri.createBucket("""{"id": "$bucketId", "type": "test", "hostname": "test", "client": "test"}""")
        assertTrue("Expected bucket $bucketId to be present", ri.getBucketsJSON().has(bucketId))
        ri.heartbeat(bucketId, """{"timestamp": "${Instant.now()}", "duration": 0, "data": {"key": "value"}}""", 1.0)
        assertTrue(
            "Expected a heartbeat with key=value in bucket $bucketId",
            hasEventWithData(ri.getEventsJSON(bucketId, 3), "key", "value")
        )
    }
}
