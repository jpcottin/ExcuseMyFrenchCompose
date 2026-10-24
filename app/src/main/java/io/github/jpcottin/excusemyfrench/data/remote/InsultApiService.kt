package io.github.jpcottin.excusemyfrench.data.remote

import android.util.Log
import io.github.jpcottin.excusemyfrench.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Callback
import okhttp3.Call
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import java.io.IOException
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

        // Enqueued, not executed: a blocking execute() cannot be interrupted, so a request
        // stalled on a flaky network kept running (and kept the fetch lock) for up to the
        // connect plus read timeouts after the screen that asked for it had gone away.
        return client.newCall(request).await().use { response ->
            Log.d("API Response", "Response Code: ${response.code} — ${response.request.url}")
            if (response.isSuccessful) {
                withContext(Dispatchers.IO) { response.body.string() }
            } else {
                Log.e("API Error", "Request failed with code: ${response.code}")
                null
            }
        }
    }

    /** Runs the call on OkHttp's own threads; cancelling the coroutine cancels the call. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Closed by the cancellation handler if the coroutine is cancelled before it
                // can take the response.
                if (cont.isActive) cont.resume(response) { _, _, _ -> response.close() } else response.close()
            }
        })
    }

    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
