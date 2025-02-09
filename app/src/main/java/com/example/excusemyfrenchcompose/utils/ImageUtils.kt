package com.example.excusemyfrenchcompose.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.util.Base64

object ImageUtils {
    fun decodeImage(imageData: String?): Bitmap? {
        return try {
            if (imageData != null && imageData.isNotEmpty()) {
                val decodedBytes = Base64.getDecoder().decode(imageData)
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } else {
                null
            }
        } catch (e: IllegalArgumentException) {
            Log.e("Image Decoding", "Base64 decoding error: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e("Image Decoding", "Image decoding error: ${e.message}", e)
            return null
        }
    }
}
