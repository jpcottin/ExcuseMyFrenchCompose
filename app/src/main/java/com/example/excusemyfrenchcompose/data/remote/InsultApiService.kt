package com.example.excusemyfrenchcompose.data.remote

import android.util.Log
import com.example.excusemyfrenchcompose.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InsultApiService {
    /**
     * Fetches a random insult of at most [level] (1 = family-friendly, 2 = vulgar,
     * 3 = offensive; levels are cumulative server-side).
     */
    suspend fun fetchInsult(level: Int): String?
}

class InsultApiServiceImpl(
    private val url: String = BuildConfig.INSULT_API_URL
) : InsultApiService {

    override suspend fun fetchInsult(level: Int): String? {
        val request = Request.Builder()
            .url(
                url.toHttpUrl().newBuilder()
                    .addQueryParameter("level", level.toString())
                    .build()
            )
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                Log.d("API Response", "Response Code: ${response.code} — ${response.request.url}")
                if (response.isSuccessful) {
                    response.body.string()
                } else {
                    Log.e("API Error", "Request failed with code: ${response.code}")
                    null
                }
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
