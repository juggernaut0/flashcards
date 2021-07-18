@file:UseSerializers(UUIDSerializer::class)

package deck.overview

import FlashcardsService
import WanikaniService
import flashcards.api.v1.DeckRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable

class DeckOverviewModel(
    private val flashcardsService: FlashcardsService,
    private val wanikaniService: WanikaniService,
    val deckId: UUID,
) {
    suspend fun getData(): DeckOverviewData {
        val query = flashcardsService.query(DeckOverviewQuery.serializer(), "id" to deckId)
        val unadded = query.sources.toMutableList()
        val added = mutableListOf<DeckOverviewQuery.CardSource>()
        for (sourceId in query.deck.sourceIds) {
            added.add(unadded.removeFirst { it.id == sourceId })
        }
        val lessons = added.sumOf {
            when (it) {
                is DeckOverviewQuery.CardSource.CustomCardSource -> it.lessons
                is DeckOverviewQuery.CardSource.WanikaniCardSource -> wanikaniService.forSource(it.id).getLessons().size
            }
        }
        val reviews = added.sumOf {
            when (it) {
                is DeckOverviewQuery.CardSource.CustomCardSource -> it.reviews
                is DeckOverviewQuery.CardSource.WanikaniCardSource -> wanikaniService.forSource(it.id).getReviews().size
            }
        }
        return DeckOverviewData(
            name = query.deck.name,
            sources = added.map { SourceView(it.name, it.id) },
            unaddedSources = unadded.map { SourceView(it.name, it.id) },
            lessons = lessons,
            reviews = reviews,
        )
    }

    private inline fun <T> MutableList<T>.removeFirst(predicate: (T) -> Boolean): T {
        val iter = listIterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (predicate(e)) {
                iter.remove()
                return e
            }
        }
        throw NoSuchElementException()
    }

    suspend fun updateSources(newSources: List<UUID>) {
        flashcardsService.updateDeck(deckId, DeckRequest(sources = newSources))
    }
}

class DeckOverviewData(
    val name: String,
    val sources: List<SourceView>,
    val unaddedSources: List<SourceView>,
    val lessons: Int,
    val reviews: Int,
)
class SourceView(val name: String, val id: UUID) {
    override fun toString(): String {
        return name
    }
}

@Serializable
@GraphQLVariable("id", "String!")
class DeckOverviewQuery(@GraphQLArgument("id", "\$id") val deck: Deck, val sources: List<CardSource>) {
    @Serializable
    data class Deck(val name: String, val sourceIds: List<UUID>)
    @Serializable
    sealed class CardSource {
        abstract val name: String
        abstract val id: UUID

        @Serializable
        @SerialName("CustomCardSource")
        class CustomCardSource(override val name: String, override val id: UUID, val lessons: Int, val reviews: Int) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(override val name: String, override val id: UUID) : CardSource()
    }
}
