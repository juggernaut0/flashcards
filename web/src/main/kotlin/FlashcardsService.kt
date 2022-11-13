import auth.AuthorizedClient
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

    suspend fun reorderSources(ids: List<UUID>) {
        client.callApi(reorderSources, Unit, ids)
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

    suspend fun reorderDecks(ids: List<UUID>) {
        client.callApi(reorderDecks, Unit, ids)
    }

    suspend fun updateDeck(id: UUID, deckRequest: DeckRequest) {
        client.callApi(updateDeck, IdParam(id), deckRequest)
    }

    suspend fun submitReview(sourceId: UUID, iid: Int, request: ReviewRequest) {
        client.callApi(submitReview, ReviewParam(sourceId, iid), request)
    }

    suspend fun resetReview(sourceId: UUID, iid: Int) {
        client.callApi(resetReview, ReviewParam(sourceId, iid))
    }
}
