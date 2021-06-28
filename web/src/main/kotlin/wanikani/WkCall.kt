package wanikani

import asynclite.await
import asynclite.delay
import kotlinx.browser.window
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit


interface WkCall {
    suspend fun fetchAssignments(): List<WkObject<Assignment>>

    suspend fun fetchSubjects(): List<WkObject<Subject>>

    suspend fun fetchReviews(): List<WkObject<Review>>
}

class HttpWkCall(private val apiKey: String) : WkCall {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchSubjects(): List<WkObject<Subject>> {
        return getAll("https://api.wanikani.com/v2/subjects", Subject.serializer())
    }

    override suspend fun fetchAssignments(): List<WkObject<Assignment>> {
        return getAll("https://api.wanikani.com/v2/assignments", Assignment.serializer())
    }

    suspend fun fetchNewAssignments(lastUpdated: Instant): List<WkObject<Assignment>> {
        return getAll("https://api.wanikani.com/v2/assignments?updated_after=$lastUpdated", Assignment.serializer())
    }

    override suspend fun fetchReviews(): List<WkObject<Review>> {
        return getAll("https://api.wanikani.com/v2/reviews", Review.serializer())
    }

    suspend fun fetchNewReviews(lastUpdated: Instant): List<WkObject<Review>> {
        return getAll("https://api.wanikani.com/v2/reviews?updated_after=$lastUpdated", Review.serializer())
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

    private suspend fun request(url: String): String {
        val resp = retry {
            val resp = window.fetch(url, RequestInit(method = "GET", headers = Headers(arrayOf(
                arrayOf("Authorization", "Bearer $apiKey"),
                arrayOf("Wanikani-Revision", "20170710")
            )))).await()
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
