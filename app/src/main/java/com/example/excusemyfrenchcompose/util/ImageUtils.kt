package com.example.excusemyfrenchcompose.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.util.Base64

object ImageUtils {
    fun decodeImage(imageData: String?): Bitmap? {
        if (imageData.isNullOrEmpty()) return null
        return try {
            val decodedBytes = Base64.getDecoder().decode(imageData)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("Image Decoding", "Image decoding error: ${e.message}", e)
            null
        }
    }
}
