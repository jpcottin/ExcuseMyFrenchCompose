package io.github.jpcottin.excusemyfrench.appfunctions

import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import io.github.jpcottin.excusemyfrench.data.remote.InsultApiServiceImpl
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepository
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepositoryImpl

/**
 * A French insult.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class FrenchInsult(
    /** The insult text, in French. */
    val text: String,
    /** The insult's level: 1 = family-friendly, 2 = vulgar, 3 = offensive. */
    val level: Int
)

/**
 * The app's [AppFunction]s.
 */
// The AppFunctions service instantiates this class through its no-arg constructor; the KSP
// compiler only generates the required factory when the no-arg constructor is the ONLY
// constructor, so tests inject a fake through the property instead of a constructor.
class InsultFunctions {
    private var _repository: InsultRepository? = null

    // Lazily created so tests that inject a fake never build the real OkHttp-backed stack.
    internal var repository: InsultRepository
        get() = _repository ?: InsultRepositoryImpl(InsultApiServiceImpl()).also { _repository = it }
        set(value) {
            _repository = value
        }

    /**
     * Fetch a random French insult whose level does not exceed [maxLevel].
     * Levels are cumulative: requesting level 2 can return level 1 or 2 insults.
     * Repeated calls return different random insults.
     *
     * @param appFunctionContext The execution context.
     * @param maxLevel The maximum insult level to allow: 1 = family-friendly,
     * 2 = adds vulgar, 3 = adds offensive. Must be 1, 2, or 3; omit (or pass 0)
     * to use the default of 1.
     * @return The fetched [FrenchInsult].
     * @throws AppFunctionInvalidArgumentException If [maxLevel] is not 0, 1, 2, or 3.
     * @throws AppFunctionAppUnknownException If the insult service is unreachable; suggest
     * the user check their internet connection and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getFrenchInsult(
        appFunctionContext: AppFunctionContext,
        maxLevel: Int = 0
    ): FrenchInsult {
        // Omitted parameters arrive as the type's empty value (0), which means "use the default".
        val effectiveLevel = if (maxLevel == 0) 1 else maxLevel
        if (effectiveLevel !in 1..3) {
            throw AppFunctionInvalidArgumentException("maxLevel must be 1, 2, or 3 (got $maxLevel)")
        }
        val response = repository.fetchInsult(effectiveLevel)
            ?: throw AppFunctionAppUnknownException("Could not reach the insult service")
        return FrenchInsult(
            text = response.insult.text,
            level = response.insult.level
        )
    }
}
