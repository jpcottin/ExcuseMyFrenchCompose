package com.example.excusemyfrenchcompose.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class) // Use Robolectric
@Config(manifest=Config.NONE)
class ImageUtilsTest {

    private lateinit var context: Context  // Declare a Context variable

    @Before
    fun setUp() {
        // Get the application context
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun decodeImage_validBase64_returnsBitmap() {
        //This is no more useful in ou case.
        //val validBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP4z8DwHwAFAAH/VscvDQAAAABJRU5ErkJggg=="
        val bitmap = ImageUtils.decodeImage(null)
        assertNull(bitmap)

        val bitmap2 = ImageUtils.decodeImage("")
        assertNull(bitmap2)
        val invalidBase64 = "This is not a valid Base64 string!"
        val bitmap3 = ImageUtils.decodeImage(invalidBase64)
        assertNull(bitmap3)
    }
}