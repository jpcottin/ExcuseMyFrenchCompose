package io.github.jpcottin.excusemyfrench.data.repository

import android.util.Log
import io.github.jpcottin.excusemyfrench.data.model.InsultResponse
import io.github.jpcottin.excusemyfrench.data.remote.InsultApiService
import kotlinx.serialization.json.Json

interface InsultRepository {
    suspend fun fetchInsult(level: Int): InsultResponse?
}

class InsultRepositoryImpl(
    private val apiService: InsultApiService
) : InsultRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchInsult(level: Int): InsultResponse? {
        val body = apiService.fetchInsult(level) ?: return null
        return try {
            json.decodeFromString<InsultResponse>(body)
        } catch (e: Exception) {
            Log.e("InsultRepository", "Failed to parse response: ${e.message}", e)
            null
        }
    }
}
