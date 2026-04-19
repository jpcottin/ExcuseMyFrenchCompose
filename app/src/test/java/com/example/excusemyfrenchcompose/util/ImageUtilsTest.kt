package com.example.excusemyfrenchcompose.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class ImageUtilsTest {

    @Test
    fun decodeImage_nullInput_returnsNull() {
        assertNull(ImageUtils.decodeImage(null))
    }

    @Test
    fun decodeImage_emptyInput_returnsNull() {
        assertNull(ImageUtils.decodeImage(""))
    }

    @Test
    fun decodeImage_invalidBase64_returnsNull() {
        assertNull(ImageUtils.decodeImage("This is not a valid Base64 string!"))
    }

    @Test
    fun decodeImage_validBase64_returnsBitmap() {
        // 1x1 red pixel PNG encoded as Base64
        val validBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI6QAAAABJRU5ErkJggg=="
        assertNotNull(ImageUtils.decodeImage(validBase64))
    }
}
