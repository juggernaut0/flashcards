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

@OptIn(ExperimentalSerializationApi::class)
class WanikaniAccount private constructor(
    val apiKey: String,
    var lastUpdated: Instant?,
    private val assignments: MutableMap<Long, dynamic>,
    private val subjects: MutableMap<Long, dynamic>,
    private val studyMaterials: MutableMap<Long, dynamic>, // subject_id to study material
    user: WkObject<User>?,
) {
    private val wkCall = HttpWkCall(apiKey)
    private lateinit var user: WkObject<User>

    init {
        if (user != null) {
            this.user = user
        }
    }

    // TODO loading progress indication in UI
    @OptIn(ExperimentalTime::class)
    suspend fun update(force: Boolean = false) {
        val lastUpdated = lastUpdated
        val now = Clock.System.now()
        if (!force && lastUpdated != null && now - lastUpdated < Duration.minutes(30)) return
        this.lastUpdated = now
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
    }

    suspend fun getLessons(): List<WkObject<Assignment>> {
        update()
        return assignments.values
            .filter { it.dynamicValue.data.srsStage == 0 }
            .map { it.value }
            .sortedWith(assignmentComparator)
    }

    suspend fun getReviews(): List<WkObject<Assignment>> {
        update()
        val now = Clock.System.now()
        val userLevel = user.data.level
        return assignments.values.filter {
            val data = it.dynamicValue.data
            val level = subject(data.subjectId)?.data?.level ?: 61
            val availableAt = data.availableAt
            userLevel >= level && availableAt != null && availableAt <= now
        }.map { it.value }
    }

    private fun subject(id: Long): WkObject<Subject>? = subjects[id]?.value

    suspend fun getSubject(id: Long): WkObject<Subject>? {
        update()
        return subject(id)
    }

    suspend fun getStudyMaterial(subjectId: Long): WkObject<StudyMaterial>? {
        update()
        //return studyMaterials[subjectId]
        return null
    }

    suspend fun startAssignment(assignmentId: Long) {
        val updatedAssign = wkCall.startAssignment(assignmentId, Clock.System.now())
        assignments[updatedAssign.id] = DynamicLazy.value(updatedAssign, assignmentSerializer)
    }

    suspend fun createReview(assignmentId: Long, meaningIncorrect: Int, readingIncorrect: Int) {
        val review = wkCall.createReview(assignmentId, meaningIncorrect, readingIncorrect, Clock.System.now())
        val assignment = review.resources_updated?.assignment!!
        assignments[assignment.id] = DynamicLazy.value(assignment, assignmentSerializer)
    }

    private suspend fun writeToStorage() {
        val data = js("{}")
        data.lastUpdated = lastUpdated?.toString()
        data.assignments = assignments.values.map { it.dynamicValue }.toTypedArray()
        data.subjects = subjects.values.map { it.dynamicValue }.toTypedArray()
        data.studyMaterials = emptyArray<dynamic>()
        data.user = user
        console.log("start write")
        idbkeyval.set("flashcards-wk-$apiKey-data", data).await()
        console.log("end write")
    }

    companion object {
        private val assignmentSerializer = WkObject.serializer(Assignment.serializer())
        private val subjectSerializer = WkObject.serializer(Subject.serializer())

        suspend fun newFromStorage(apiKey: String): WanikaniAccount {
            console.log("start read")
            val rawData = idbkeyval.get("flashcards-wk-$apiKey-data").await().asDynamic()
            console.log("end read")
            return if (rawData == null || rawData == undefined) {
                newEmpty(apiKey)
            } else {
                try {
                    WanikaniAccount(
                        apiKey = apiKey,
                        lastUpdated = Instant.parse(rawData.lastUpdated as String),
                        assignments = (rawData.assignments as Array<dynamic>).associateByTo(mutableMapOf(), { it.id }, { DynamicLazy.dynamic(it, assignmentSerializer) }),
                        subjects = (rawData.subjects as Array<dynamic>).associateByTo(mutableMapOf(), { it.id }, { DynamicLazy.dynamic(it, subjectSerializer) }),
                        studyMaterials = (rawData.studyMaterials as Array<dynamic>).associateByTo(mutableMapOf()) { it.data.subject_id },
                        user = Json.Default.decodeFromDynamic(WkObject.serializer(User.serializer()), rawData.user),
                    )
                } catch (e: SerializationException) {
                    console.error(e)
                    newEmpty(apiKey)
                }
            }
        }

        private fun newEmpty(apiKey: String): WanikaniAccount {
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

    private val assignmentComparator = Comparator<WkObject<Assignment>> { a, b ->
        SubjectComparator.compare(subject(a.data.subjectId)!!, subject(b.data.subjectId)!!)
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

@Suppress("UnsafeCastFromDynamic")
private fun subjectFromDynamic(value: dynamic): WkObject<Subject> {
    val type = value.`object`
    val data = when (type) {
        "radical" -> {
            RadicalSubject(
                characters = value.data.characters,

            )
        }
    }
    return WkObject(
        id = value.id,
        `object` = type,
    )
}

private val json = Json { ignoreUnknownKeys = true }
@OptIn(ExperimentalSerializationApi::class)
class DynamicLazy<T: Any> private constructor(
    val dynamicValue: dynamic,
    private val serializer: KSerializer<T>,
    private var _value: T?,
) {
    companion object {
        fun <T: Any> dynamic(dynamicValue: dynamic, serializer: KSerializer<T>): DynamicLazy<T> {
            return DynamicLazy(dynamicValue, serializer, null)
        }
        fun <T: Any> value(value: T, serializer: KSerializer<T>): DynamicLazy<T> {
            return DynamicLazy(json.encodeToDynamic(serializer, value), serializer, value)
        }
    }

    val value: T get() = _value ?: json.decodeFromDynamic(serializer, dynamicValue).also { _value = it }
}
