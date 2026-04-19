package com.example.excusemyfrenchcompose.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InsultApiService {
    suspend fun fetchInsult(): String?
}

class InsultApiServiceImpl : InsultApiService {

    override suspend fun fetchInsult(): String? {
        val request = Request.Builder()
            .url("https://excusemyfrench.herokuapp.com/api/v1/img")
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            Log.d("API Response", "Response Code: ${response.code} — ${response.request.url}")
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                Log.e("API Error", "Request failed with code: ${response.code}")
                null
            }
        }
    }

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
