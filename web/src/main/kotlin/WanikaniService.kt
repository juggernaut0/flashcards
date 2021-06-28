import asynclite.await
import kotlinx.browser.localStorage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import multiplatform.UUID
import org.w3c.dom.get
import org.w3c.dom.set
import wanikani.Assignment
import wanikani.HttpWkCall
import wanikani.WkObject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class WanikaniService {
    // apiKey to account data
    private val cachedData: MutableMap<String, WanikaniAccount> = mutableMapOf()

    suspend fun forSource(sourceId: UUID): WanikaniAccount {
        // TODO proper error reporting
        val apiKey = getApiKey(sourceId) ?: throw IllegalStateException("API key is null")
        return cachedData.getOrPut(apiKey) { WanikaniAccount.newFromStorage(apiKey) }
    }

    fun saveApiKey(sourceId: UUID, apiKey: String) {
        localStorage["$storageKeyPrefix$sourceId"] = apiKey
    }

    fun getApiKey(sourceId: UUID): String? {
        return localStorage["$storageKeyPrefix$sourceId"]
    }

    private companion object {
        private const val storageKeyPrefix = "flashcards-wk-"
    }
}

class WanikaniAccount private constructor(
    val apiKey: String,
    var lastUpdated: Instant?,
    private val assignments: MutableMap<Long, WkObject<Assignment>>
) {
    private val wkCall = HttpWkCall(apiKey)

    // TODO loading progress indication in UI
    @OptIn(ExperimentalTime::class)
    private suspend fun update() {
        val lastUpdated = lastUpdated
        val now = Clock.System.now()
        if (lastUpdated != null && now - lastUpdated < Duration.minutes(30)) return
        this.lastUpdated = now
        if (lastUpdated == null) {
            wkCall.fetchAssignments().forEach { assignments[it.id] = it }
        } else {
            wkCall.fetchNewAssignments(lastUpdated).forEach { assignments[it.id] = it }
        }
        writeToStorage()
    }

    suspend fun getLessons(): List<WkObject<Assignment>> {
        update()
        return assignments.values.filter { it.data.srsStage == 0 }
    }

    suspend fun getReviews(): List<WkObject<Assignment>> {
        update()
        val now = Clock.System.now()
        return assignments.values.filter {
            val availableAt = it.data.availableAt
            availableAt != null && availableAt <= now
        }
    }

    private suspend fun writeToStorage() {
        val data = Json.Default.encodeToString(Data.serializer(), Data(lastUpdated!!, assignments.values.toList()))
        idbkeyval.set("flashcards-wk-$apiKey-data", data).await()
    }

    companion object {
        suspend fun newFromStorage(apiKey: String): WanikaniAccount {
            val rawData = idbkeyval.get("flashcards-wk-$apiKey-data").await()
            return if (rawData == null || rawData == undefined) {
                WanikaniAccount(apiKey, null, mutableMapOf())
            } else {
                val data = Json.Default.decodeFromString(Data.serializer(), rawData)
                WanikaniAccount(
                    apiKey = apiKey,
                    lastUpdated = data.lastUpdated,
                    assignments = data.assignments.associateByTo(mutableMapOf()) { it.id }
                )
            }
        }
    }

    @Serializable
    private class Data(val lastUpdated: Instant, val assignments: List<WkObject<Assignment>>)
}
