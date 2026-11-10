package com.luckyprospector.app.inference

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.Closeable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YoloxModelInstrumentedTest {
    @Test fun approvedBundleLoadsAndRunsOnDevice() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val detector = ModelBundleLoader(context).load()
            try {
                assertTrue(detector is YoloxTfliteDetector)
                detector.detect(
                    DetectionFrame(
                        luminance = ByteArray(4),
                        width = 2,
                        height = 2,
                        rotationDegrees = 0,
                        timestampMillis = 1L,
                        rgb = ByteArray(12),
                    ),
                )
            } finally {
                (detector as? Closeable)?.close()
            }
        }
    }
}
