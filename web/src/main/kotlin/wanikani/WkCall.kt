package wanikani

import asynclite.await
import asynclite.delay
import kotlinx.browser.window
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit


class HttpWkCall(private val apiKey: String) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchUser(): WkObject<User> {
        val resp = request("https://api.wanikani.com/v2/user")
        return json.decodeFromString(WkObject.serializer(User.serializer()), resp)
    }

    suspend fun fetchSubjects(): List<WkObject<Subject>> {
        return getAll("https://api.wanikani.com/v2/subjects", JsonObject.serializer()).map { convertSubject(it) }
    }

    suspend fun fetchNewSubjects(lastUpdated: Instant): List<WkObject<Subject>> {
        return getAll("https://api.wanikani.com/v2/subjects?updated_after=$lastUpdated", JsonObject.serializer()).map { convertSubject(it) }
    }

    private fun convertSubject(subject: WkObject<JsonObject>): WkObject<Subject> {
        val ser = when (subject.`object`) {
            "radical" -> RadicalSubject.serializer()
            "kanji" -> KanjiSubject.serializer()
            "vocabulary" -> VocabularySubject.serializer()
            else -> error("unknown subject type ${subject.`object`}")
        }
        return WkObject(subject.id, subject.`object`, json.decodeFromJsonElement(ser, subject.data))
    }

    suspend fun fetchAssignments(): List<WkObject<Assignment>> {
        return getAll("https://api.wanikani.com/v2/assignments", Assignment.serializer())
    }

    suspend fun fetchNewAssignments(lastUpdated: Instant): List<WkObject<Assignment>> {
        return getAll("https://api.wanikani.com/v2/assignments?updated_after=$lastUpdated", Assignment.serializer())
    }

    suspend fun startAssignment(assignmentId: Long, started_at: Instant? = null): WkObject<Assignment> {
        val body = if (started_at == null) "{}" else "{\"started_at\": \"$started_at\"}"
        val resp = request("https://api.wanikani.com/v2/assignments/$assignmentId/start", method = "PUT", body = body)
        return json.decodeFromString(WkObject.serializer(Assignment.serializer()), resp)
    }

    suspend fun createReview(assignmentId: Long, meaningIncorrect: Int, readingIncorrect: Int, createdAt: Instant? = null): WkObject<Review> {
        val body = buildJsonObject {
            putJsonObject("review") {
                put("assignment_id", assignmentId)
                put("incorrect_meaning_answers", meaningIncorrect)
                put("incorrect_reading_answers", readingIncorrect)
                if (createdAt != null) {
                    put("created_at", createdAt.toString())
                }
            }
        }.toString()

        val resp = request("https://api.wanikani.com/v2/reviews", method = "POST", body = body)
        return json.decodeFromString(WkObject.serializer(Review.serializer()), resp)
    }

    suspend fun fetchStudyMaterials(): List<WkObject<StudyMaterial>> {
        return getAll("https://api.wanikani.com/v2/study_materials", StudyMaterial.serializer())
    }

    suspend fun fetchNewStudyMaterials(lastUpdated: Instant): List<WkObject<StudyMaterial>> {
        return getAll("https://api.wanikani.com/v2/study_materials?updated_after=$lastUpdated", StudyMaterial.serializer())
    }

    private suspend fun <T> getAll(url: String, serializer: KSerializer<T>): List<WkObject<T>> {
        val result = mutableListOf<WkObject<T>>()
        var collection = json.decodeFromString(WkCollection.serializer(serializer), request(url))
        while (true) {
            result.addAll(collection.data)
            val nextUrl = collection.pages.nextUrl
            if (nextUrl != null) {
                delay(100)
                collection = json.decodeFromString(WkCollection.serializer(serializer), request(nextUrl))
            } else {
                break
            }
        }

        return result
    }

    private suspend fun request(url: String, method: String = "GET", body: String? = null): String {
        val reqInit = RequestInit(
            method = method,
            headers = Headers(listOfNotNull(
                arrayOf("Authorization", "Bearer $apiKey"),
                arrayOf("Wanikani-Revision", "20170710"),
                arrayOf("Content-Type", "application/json; charset=utf-8").takeIf { method != "GET" }
            ).toTypedArray())
        )
        if (body != null) {
            reqInit.body = body
        }
        val resp = retry {
            val resp = window.fetch(url, reqInit).await()
            if (resp.status.toInt() == 429) {
                delay(1000)
                null
            }
            else {
                resp
            }
        }
        if (!resp.ok) throw RuntimeException("Received non-ok status code: ${resp.status}")
        return resp.text().await()
    }

    private inline fun <T> retry(body: () -> T?): T {
        while (true) {
            body()?.also { return it }
        }
    }
}
