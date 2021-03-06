@file:UseSerializers(UUIDSerializer::class)

package dashboard

import FlashcardsService
import WanikaniService
import flashcards.api.v1.DeckRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer

class DashboardModel(
    private val flashcardsService: FlashcardsService,
    private val wanikaniService: WanikaniService,
) {
    suspend fun getData(): DashboardData {
        val query = flashcardsService.query(DashboardQuery.serializer())
        return DashboardData(
            decks = query.decks.map { deck ->
                val lessons = deck.sources.sumOf {
                    when (it) {
                        is DashboardQuery.DeckCardSource.CustomCardSource -> it.lessons
                        is DashboardQuery.DeckCardSource.WanikaniCardSource -> wanikaniService.forSource(it.id).getLessons().size
                    }
                }
                val reviews = deck.sources.sumOf {
                    when (it) {
                        is DashboardQuery.DeckCardSource.CustomCardSource -> it.reviews
                        is DashboardQuery.DeckCardSource.WanikaniCardSource -> wanikaniService.forSource(it.id).getReviews().size
                    }
                }
                DashboardDeck(deck.id, deck.name, lessons, reviews)
            },
            sources = query.sources
        )
    }

    suspend fun createDeck(request: DeckRequest): UUID {
        return flashcardsService.createDeck(request)
    }
}

class DashboardData(val decks: List<DashboardDeck>, val sources: List<DashboardQuery.CardSource>)
class DashboardDeck(val id: UUID, val name: String, val lessons: Int, val reviews: Int)

@Serializable
class DashboardQuery(val decks: List<Deck>, val sources: List<CardSource>) {
    @Serializable
    class Deck(val id: UUID, val name: String, val sources: List<DeckCardSource>)
    @Serializable
    class CardSource(val id: UUID, val name: String, val __typename: String)
    @Serializable
    sealed class DeckCardSource {
        @Serializable
        @SerialName("CustomCardSource")
        class CustomCardSource(val lessons: Int, val reviews: Int) : DeckCardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(val id: UUID) : DeckCardSource()
    }
}
