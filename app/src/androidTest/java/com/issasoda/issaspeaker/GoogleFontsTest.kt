package com.issasoda.issaspeaker

import android.graphics.Typeface
import android.util.Log
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.issasoda.issaspeaker.ui.theme.FontPool
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Asks the phone's Google Fonts provider for every family and weight of FontPool and fails with the
 * provider's reason (FontsContractCompat.FontRequestCallback.FAIL_REASON_*) for each one not served.
 *
 * Run it on the real S25, not the emulator: the emulator's font directory is stale (skill
 * android-google-fonts, "Emulator trap").
 *   ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.issasoda.issaspeaker.GoogleFontsTest
 */
@RunWith(AndroidJUnit4::class)
class GoogleFontsTest {

    @Test
    fun everyPoolFontIsServedByTheGoogleFontsProvider() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val executor = Executors.newSingleThreadExecutor()
        val failures = mutableListOf<String>()
        val queries = FontPool.families.flatMap { name -> FontPool.weights.map { "name=$name&weight=${it.weight}" } }
        for (query in queries) {
            val latch = CountDownLatch(1)
            var result = "timeout"
            FontsContractCompat.requestFont(
                context,
                FontRequest("com.google.android.gms.fonts", "com.google.android.gms", query, R.array.issa_fonts_certs),
                Typeface.NORMAL,
                executor,
                executor,
                object : FontsContractCompat.FontRequestCallback() {
                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        result = "OK"
                        latch.countDown()
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        result = "FAIL reason=$reason"
                        latch.countDown()
                    }
                },
            )
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            Log.i("IssaFonts", "$query -> $result")
            if (result != "OK") failures += "$query -> $result"
        }
        executor.shutdown()
        assertTrue("Fonts the provider did not serve (${failures.size}): $failures", failures.isEmpty())
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}
