package io.github.jpcottin.excusemyfrench.data.repository

import io.github.jpcottin.excusemyfrench.data.remote.InsultApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class InsultRepositoryImplTest {

    private class FakeApiService(private val body: String?) : InsultApiService {
        var lastRequestedLevel: Int? = null
        override suspend fun fetchInsult(level: Int): String? {
            lastRequestedLevel = level
            return body
        }
    }

    @Test
    fun `valid response is parsed into InsultResponse`() = runTest {
        val json = """
            {
              "insult": {"text": "Espèce d'idiot !", "index": 12, "level": 2},
              "image": {"data": "aGVsbG8=", "mimetype": "image/jpeg", "indexImg": 34}
            }
        """.trimIndent()
        val repository = InsultRepositoryImpl(FakeApiService(json))

        val response = repository.fetchInsult(2)

        assertEquals("Espèce d'idiot !", response?.insult?.text)
        assertEquals(2, response?.insult?.level)
        assertEquals("image/jpeg", response?.image?.mimetype)
    }

    @Test
    fun `unknown JSON keys are ignored`() = runTest {
        val json = """{"insult": {"text": "Con", "level": 1}, "unexpected": true}"""
        val repository = InsultRepositoryImpl(FakeApiService(json))

        assertEquals("Con", repository.fetchInsult(1)?.insult?.text)
    }

    @Test
    fun `missing fields fall back to defaults`() = runTest {
        val repository = InsultRepositoryImpl(FakeApiService("{}"))

        val response = repository.fetchInsult(1)

        assertEquals("No insult available", response?.insult?.text)
        assertEquals(-1, response?.insult?.level)
    }

    @Test
    fun `malformed JSON returns null`() = runTest {
        val repository = InsultRepositoryImpl(FakeApiService("not json at all"))

        assertNull(repository.fetchInsult(1))
    }

    @Test
    fun `null body returns null`() = runTest {
        val repository = InsultRepositoryImpl(FakeApiService(null))

        assertNull(repository.fetchInsult(1))
    }

    @Test
    fun `requested level is passed through to the API`() = runTest {
        val apiService = FakeApiService("{}")
        val repository = InsultRepositoryImpl(apiService)

        repository.fetchInsult(3)

        assertEquals(3, apiService.lastRequestedLevel)
    }
}
