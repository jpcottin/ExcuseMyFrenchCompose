package io.github.jpcottin.excusemyfrench.appfunctions

import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import io.github.jpcottin.excusemyfrench.data.model.Insult
import io.github.jpcottin.excusemyfrench.data.model.InsultResponse
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// The AppFunction exception types touch Android statics in their constructors,
// so this test needs Robolectric rather than the plain-JVM android.jar stubs.
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class InsultFunctionsTest {

    private val context = mockk<AppFunctionContext>(relaxed = true)

    private class FakeRepository(private val response: InsultResponse?) : InsultRepository {
        var lastRequestedLevel: Int? = null
        override suspend fun fetchInsult(level: Int): InsultResponse? {
            lastRequestedLevel = level
            return response
        }
    }

    private fun response(text: String, level: Int) =
        InsultResponse(insult = Insult(text = text, index = 1, level = level))

    @Test
    fun `returns insult text and level from repository`() = runTest {
        val functions = InsultFunctions().apply { repository = FakeRepository(response("Espèce d'idiot !", 2)) }

        val result = functions.getFrenchInsult(context, maxLevel = 2)

        assertEquals("Espèce d'idiot !", result.text)
        assertEquals(2, result.level)
    }

    @Test
    fun `omitted maxLevel defaults to level 1`() = runTest {
        val repository = FakeRepository(response("Bougre !", 1))
        val functions = InsultFunctions().apply { this.repository = repository }

        functions.getFrenchInsult(context)

        assertEquals(1, repository.lastRequestedLevel)
    }

    @Test
    fun `maxLevel zero is treated as the default level 1`() = runTest {
        val repository = FakeRepository(response("Bougre !", 1))
        val functions = InsultFunctions().apply { this.repository = repository }

        functions.getFrenchInsult(context, maxLevel = 0)

        assertEquals(1, repository.lastRequestedLevel)
    }

    @Test
    fun `invalid maxLevel throws AppFunctionInvalidArgumentException`() {
        val functions = InsultFunctions().apply { repository = FakeRepository(response("x", 1)) }

        assertThrows(AppFunctionInvalidArgumentException::class.java) {
            runTest { functions.getFrenchInsult(context, maxLevel = 7) }
        }
    }

    @Test
    fun `unreachable service throws AppFunctionAppUnknownException`() {
        val functions = InsultFunctions().apply { repository = FakeRepository(response = null) }

        assertThrows(AppFunctionAppUnknownException::class.java) {
            runTest { functions.getFrenchInsult(context, maxLevel = 1) }
        }
    }
}
