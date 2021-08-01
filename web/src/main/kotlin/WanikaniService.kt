import asynclite.await
import kotlinx.browser.localStorage
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic
import kotlinx.serialization.json.encodeToDynamic
import multiplatform.UUID
import org.w3c.dom.get
import org.w3c.dom.set
import wanikani.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class WanikaniService {
    // apiKey to account data
    private val cachedData: MutableMap<String?, WanikaniAccount> = mutableMapOf()

    suspend fun forSource(sourceId: UUID): WanikaniAccount {
        val apiKey = getApiKey(sourceId)
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

@OptIn(ExperimentalSerializationApi::class)
class WanikaniAccount private constructor(
    private val apiKey: String?,
    var lastUpdated: Instant?,
    private val assignments: MutableMap<Long, WkObject<Assignment>>,
    private val subjects: MutableMap<Long, WkObject<Subject>>,
    private val studyMaterials: MutableMap<Long, WkObject<StudyMaterial>>, // subject_id to study material
    user: WkObject<User>?,
) {
    private val wkCall = apiKey?.let { HttpWkCall(it) }
    private lateinit var user: WkObject<User>
    var error = apiKey == null
        private set

    init {
        if (user != null) {
            this.user = user
        }
    }

    // TODO loading progress indication in UI
    @OptIn(ExperimentalTime::class)
    suspend fun update(force: Boolean = false) {
        if (error || wkCall == null) return
        val lastUpdated = lastUpdated
        val now = Clock.System.now()
        if (!force && lastUpdated != null && now - lastUpdated < Duration.minutes(30)) return
        this.lastUpdated = now
        try {
            user = wkCall.fetchUser()
            if (lastUpdated == null) {
                wkCall.fetchAssignments().forEach { assignments[it.id] = it }
                wkCall.fetchSubjects().forEach { subjects[it.id] = it }
                wkCall.fetchStudyMaterials().forEach { studyMaterials[it.data.subject_id] = it }
            } else {
                wkCall.fetchNewAssignments(lastUpdated).forEach { assignments[it.id] = it }
                wkCall.fetchNewSubjects(lastUpdated).forEach { subjects[it.id] = it }
                wkCall.fetchNewStudyMaterials(lastUpdated).forEach { studyMaterials[it.data.subject_id] = it }
            }
            writeToStorage()
        } catch (e: Exception) {
            console.error(e)
            this.lastUpdated = lastUpdated
            error = true
        }
    }

    suspend fun getLessons(): List<WkObject<Assignment>> {
        update()
        return assignments.values.filter { it.data.srsStage == 0 }.sortedWith(assignmentComparator)
    }

    suspend fun getReviews(): List<WkObject<Assignment>> {
        update()
        if (error) return emptyList()
        val now = Clock.System.now()
        val userLevel = user.data.level
        return assignments.values.filter {
            val level = subjects[it.data.subjectId]?.data?.level ?: 61
            val availableAt = it.data.availableAt
            userLevel >= level && availableAt != null && availableAt <= now
        }
    }

    suspend fun getReviewForecast(): Map<Instant, Int> {
        update()
        val userLevel = user.data.level
        return assignments.values
            .filter {
                val level = subjects[it.data.subjectId]?.data?.level ?: 61
                val availableAt = it.data.availableAt
                userLevel >= level && availableAt != null
            }
            .groupingBy { it.data.availableAt!! }
            .eachCount()
    }

    suspend fun getSubject(id: Long): WkObject<Subject>? {
        update()
        return subjects[id]
    }

    suspend fun getStudyMaterial(subjectId: Long): WkObject<StudyMaterial>? {
        update()
        return studyMaterials[subjectId]
    }

    suspend fun startAssignment(assignmentId: Long) {
        if (error || wkCall == null) return
        val updatedAssign = wkCall.startAssignment(assignmentId, Clock.System.now())
        assignments[updatedAssign.id] = updatedAssign
    }

    suspend fun createReview(assignmentId: Long, meaningIncorrect: Int, readingIncorrect: Int) {
        if (error || wkCall == null) return
        val review = wkCall.createReview(assignmentId, meaningIncorrect, readingIncorrect, Clock.System.now())
        val assignment = review.resources_updated?.assignment!!
        assignments[assignment.id] = assignment
    }

    private suspend fun writeToStorage() {
        val data = Data(lastUpdated!!, user, assignments.values.toList(), subjects.values.toList(), studyMaterials.values.toList())
        console.log("start write")
        idbkeyval.set("flashcards-wk-$apiKey-data", Json.Default.encodeToDynamic(Data.serializer(), data)).await()
        console.log("end write")
    }

    companion object {
        suspend fun newFromStorage(apiKey: String?): WanikaniAccount {
            console.log("start read")
            val rawData = idbkeyval.get("flashcards-wk-$apiKey-data").await()
            console.log("end read")
            return if (rawData == null || rawData == undefined) {
                newEmpty(apiKey)
            } else {
                try {
                    console.log("start decode")
                    val data = Json.Default.decodeFromDynamic(Data.serializer(), rawData)
                    console.log("end decode")
                    WanikaniAccount(
                        apiKey = apiKey,
                        lastUpdated = data.lastUpdated,
                        assignments = data.assignments.associateByTo(mutableMapOf()) { it.id },
                        subjects = data.subjects.associateByTo(mutableMapOf()) { it.id },
                        studyMaterials = data.studyMaterials.associateByTo(mutableMapOf()) { it.data.subject_id },
                        user = data.user,
                    )
                } catch (e: SerializationException) {
                    console.error(e)
                    newEmpty(apiKey)
                }
            }
        }

        private fun newEmpty(apiKey: String?): WanikaniAccount {
            return WanikaniAccount(
                apiKey = apiKey,
                lastUpdated = null,
                assignments = mutableMapOf(),
                subjects = mutableMapOf(),
                studyMaterials = mutableMapOf(),
                user = null
            )
        }
    }

    @Serializable
    private class Data(
        val lastUpdated: Instant,
        val user: WkObject<User>,
        val assignments: List<WkObject<Assignment>>,
        val subjects: List<WkObject<Subject>>,
        val studyMaterials: List<WkObject<StudyMaterial>>,
    )

    private val assignmentComparator = Comparator<WkObject<Assignment>> { a, b ->
        SubjectComparator.compare(subjects[a.data.subjectId]!!, subjects[b.data.subjectId]!!)
    }

    object SubjectComparator : Comparator<WkObject<Subject>> {
        override fun compare(a: WkObject<Subject>, b: WkObject<Subject>): Int {
            a.data.level.compareTo(b.data.level).let { if (it != 0) return it }

            val aType = when (a.`object`) {
                "radical" -> 0
                "kanji" -> 1
                "vocabulary" -> 2
                else -> 3
            }
            val bType = when (a.`object`) {
                "radical" -> 0
                "kanji" -> 1
                "vocabulary" -> 2
                else -> 3
            }
            aType.compareTo(bType).let { if (it != 0) return it }

            return a.id.compareTo(b.id)
        }
    }
}
