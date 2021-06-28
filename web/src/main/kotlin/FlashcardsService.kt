import flashcards.api.v1.*
import kotlinx.serialization.KSerializer
import multiplatform.UUID
import multiplatform.api.*
import multiplatform.graphql.callGraphQL

class FlashcardsService {
    private val client = AuthorizedClient(FetchClient())

    suspend fun <T> query(ser: KSerializer<T>, vararg variables: Pair<String, Any>): T {
        return client.callGraphQL(query, ser, variables = variables.toMap())
    }

    suspend fun createSource(sourceRequest: CardSourceRequest): UUID {
        return client.callApi(createSource, Unit, sourceRequest)
    }

    suspend fun updateSource(id: UUID, sourceRequest: CardSourceRequest) {
        client.callApi(updateSource, IdParam(id), sourceRequest)
    }

    suspend fun deleteSource(id: UUID) {
        // TODO
    }

    suspend fun createDeck(deckRequest: DeckRequest): UUID {
        return client.callApi(createDeck, Unit, deckRequest)
    }

    suspend fun updateDeck(id: UUID, deckRequest: DeckRequest) {
        client.callApi(updateDeck, IdParam(id), deckRequest)
    }

    suspend fun submitReview(sourceId: UUID, iid: Int, request: ReviewRequest) {
        client.callApi(submitReview, ReviewParam(sourceId, iid), request)
    }
}

fun Iterable<Pair<String, String>>.asHeaders(): Headers = object : Headers, Iterable<Pair<String, String>> by this {}

class AuthorizedClient(private val delegate: ApiClient): ApiClient {
    override suspend fun <P, R> callApi(apiRoute: ApiRoute<P, R>, params: P, headers: Headers?): R {
        return delegate.callApi(apiRoute, params, headers.addAuthHeader())
    }

    override suspend fun <P, T, R> callApi(
        apiRoute: ApiRouteWithBody<P, T, R>,
        params: P,
        body: T,
        headers: Headers?
    ): R {
        return delegate.callApi(apiRoute, params, body, headers.addAuthHeader())
    }

    private fun Headers?.addAuthHeader(): Headers {
        return (this?.toMutableList() ?: mutableListOf())
            .also { it.add("Authorization" to "Bearer ${auth.getToken()}") }
            .asHeaders()
    }
}
