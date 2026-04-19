package com.example.excusemyfrenchcompose.data.repository

import android.util.Log
import com.example.excusemyfrenchcompose.data.model.InsultResponse
import com.example.excusemyfrenchcompose.data.remote.InsultApiService
import kotlinx.serialization.json.Json

interface InsultRepository {
    suspend fun fetchInsult(): InsultResponse?
}

class InsultRepositoryImpl(
    private val apiService: InsultApiService
) : InsultRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchInsult(): InsultResponse? {
        val body = apiService.fetchInsult() ?: return null
        return try {
            json.decodeFromString<InsultResponse>(body)
        } catch (e: Exception) {
            Log.e("InsultRepository", "Failed to parse response: ${e.message}", e)
            null
        }
    }
}
