@file:UseSerializers(UUIDSerializer::class)

package deck.overview

import FlashcardsService
import WanikaniService
import flashcards.api.v1.DeckRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable
import kotlin.time.Duration.Companion.days

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

        val reviewItems = added.flatMap { cs ->
            when (cs) {
                is DeckOverviewQuery.CardSource.CustomCardSource -> cs.reviewItems.map { it.cardGroup.srsStage }
                is DeckOverviewQuery.CardSource.WanikaniCardSource -> wanikaniService.forSource(cs.id).getReviews().map { it.data.srsStage }
            }
        }
        val reviews = reviewItems.size
        val reviewsPerStage = reviewItems
            .groupingBy { it }
            .eachCount()

        val reviewForecast = mutableMapOf<Instant, Int>()
        val now = Clock.System.now()
        val oneWeek = now + 7.days
        for (source in added) {
            val forecast = when (source) {
                is DeckOverviewQuery.CardSource.CustomCardSource -> source.reviewForecast.associate { it.time to it.count }
                is DeckOverviewQuery.CardSource.WanikaniCardSource -> wanikaniService.forSource(source.id).getReviewForecast()
            }.filter { (k, _) -> k > now && k <= oneWeek }
            reviewForecast.merge(forecast) { a, b -> a + b }
        }
        val forecastEntries = reviewForecast.entries
            .sortedBy { it.key }
            .mapWithAcc(reviews) { (time, count), total ->
                val newTotal = total + count
                ReviewForecastEntry(time, count, newTotal) to newTotal
            }

        return DeckOverviewData(
            name = query.deck.name,
            sources = added.map { SourceView(it.name, it.id) },
            unaddedSources = unadded.map { SourceView(it.name, it.id) },
            lessons = lessons,
            reviews = reviews,
            reviewsPerStage = reviewsPerStage,
            reviewForecast = forecastEntries,
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

    private inline fun <K, V> MutableMap<K, V>.merge(other: Map<K, V>, merger: (V, V) -> V) {
        for ((k, v) in other) {
            val newV = this[k]?.let { merger(it, v) } ?: v
            this[k] = newV
        }
    }

    private inline fun <T, U, A> List<T>.mapWithAcc(initial: A, mapper: (T, A) -> Pair<U, A>): List<U> {
        val result = mutableListOf<U>()
        var a = initial
        for (t in this) {
            val (u, newA) = mapper(t, a)
            result.add(u)
            a = newA
        }
        return result
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
    val reviewsPerStage: Map<Int, Int>, // srsStage to count
    val reviewForecast: List<ReviewForecastEntry>,
)
class SourceView(val name: String, val id: UUID) {
    override fun toString(): String {
        return name
    }
}
class ReviewForecastEntry(val time: Instant, val count: Int, val total: Int)

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
        class CustomCardSource(
            override val name: String,
            override val id: UUID,
            val lessons: Int,
            val reviewItems: List<ReviewItem>,
            val reviewForecast: List<ReviewForecastItem>,
        ) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(override val name: String, override val id: UUID) : CardSource()
    }

    @Serializable
    data class ReviewItem(val cardGroup: CardGroup)

    @Serializable
    data class CardGroup(val srsStage: Int)

    @Serializable
    data class ReviewForecastItem(val time: Instant, val count: Int)
}
