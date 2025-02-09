package com.example.excusemyfrenchcompose.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InsultApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchInsult(): String? { // Now suspend
        val request = Request.Builder()
            .url("https://excusemyfrench.herokuapp.com/api/v1/img")
            .build()

        return try {
            withContext(Dispatchers.IO) { // NOW the network call is on Dispatchers.IO
                val response: Response = client.newCall(request).execute()
                Log.d("API Response", "Response Code: ${response.code}")
                Log.d("API Response", "Response Message: ${response.message}")
                Log.d("API Response", "Final URL: ${response.request.url}")

                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e("API Error", "API request failed with code: ${response.code}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e("API Request", "IO Exception: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("API Request", "Exception: ${e.message}", e)
            null
        }
    }
}