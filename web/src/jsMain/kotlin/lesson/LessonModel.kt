@file:UseSerializers(UUIDSerializer::class)

package lesson

import FlashcardsService
import WanikaniService
import flashcards.api.v1.ReviewRequest
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLQuery
import review.Reviewer
import wanikani.toCardGroup

class LessonModel(
    private val flashcardsService: FlashcardsService,
    private val wanikaniService: WanikaniService,
    private val deckId: UUID,
) {
    suspend fun getData(): LessonScreenData {
        val deck = flashcardsService.query(LessonScreenQuery(deckId)).deck
        val totalLessons = deck.sources.sumOf {
            when (it) {
                is LessonScreenQueryResponse.CardSource.CustomCardSource -> it.lessons
                is LessonScreenQueryResponse.CardSource.WanikaniCardSource -> wanikaniService.forSource(it.id).getLessons().size
            }
        }
        // TODO number of items based on deck settings
        val items = deck.sources
            .flatMapTake(totalLessons.coerceAtMost(5)) { source ->
                when(source) {
                    is LessonScreenQueryResponse.CardSource.CustomCardSource -> getCustomLessons(source.id)
                    is LessonScreenQueryResponse.CardSource.WanikaniCardSource -> {
                        val wkAccount = wanikaniService.forSource(source.id)
                        val itemSource = Reviewer.Source(source.id, source.name, "WanikaniCardSource")
                        wkAccount.getLessons().mapNotNull { assignment ->
                            val subjectId = assignment.data.subjectId
                            val subject = wkAccount.getSubject(subjectId) ?: return@mapNotNull null
                            val studyMaterial = wkAccount.getStudyMaterial(subjectId)
                            val group = toCardGroup(assignment, subject, studyMaterial)
                            Reviewer.ReviewItem(itemSource, group)
                        }
                    }
                }
            }
        return LessonScreenData(totalLessons = totalLessons, items = items)
    }

    private inline fun <T, U> Iterable<T>.flatMapTake(n: Int, mapping: (T) -> Iterable<U>): List<U> {
        val result = mutableListOf<U>()
        val tIter = iterator()
        while (result.size < n && tIter.hasNext()) {
            val uIter = mapping(tIter.next()).iterator()
            while (result.size < n && uIter.hasNext()) {
                result.add(uIter.next())
            }
        }
        return result
    }

    private suspend fun getCustomLessons(sourceId: UUID): List<Reviewer.ReviewItem> {
        return flashcardsService.query(LessonItemQuery(sourceId))
            .source
            .let { it as LessonItemQueryResponse.CardSource.CustomCardSource }
            .lessonItems
    }

    suspend fun submit(result: Reviewer.ReviewResult) {
        when(val typename = result.item.source.__typename) {
            "CustomCardSource" -> {
                flashcardsService.submitReview(
                    sourceId = result.item.source.id,
                    iid = result.item.cardGroup.iid.toInt(),
                    request = ReviewRequest(List(result.timesIncorrect.size) { 0 })
                )
            }
            "WanikaniCardSource" -> {
                wanikaniService.forSource(result.item.source.id).startAssignment(result.item.cardGroup.iid)
            }
            else -> console.warn("Unknown card source type '$typename'")
        }
    }
}

class LessonScreenData(val totalLessons: Int, val items: List<Reviewer.ReviewItem>)

class LessonScreenQuery(id: UUID) : GraphQLQuery<LessonScreenQueryResponse> {
    override val queryString = """
        query(${'$'}id: String!) {
            deck(id: ${'$'}id) {
                sources {
                    id
                    type: __typename
                    ... on CustomCardSource { lessons }
                    ... on WanikaniCardSource { name }
                }
            }
        }
    """.trimIndent()

    override val variables = mapOf("id" to id.toString())

    override val responseDeserializer = LessonScreenQueryResponse.serializer()

}

@Serializable
class LessonScreenQueryResponse(val deck: Deck) {
    @Serializable
    class Deck(val sources: List<CardSource>)
    @Serializable
    sealed class CardSource {
        abstract val id: UUID
        @Serializable
        @SerialName("CustomCardSource")
        class CustomCardSource(override val id: UUID, val lessons: Int) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(override val id: UUID, val name: String) : CardSource()
    }
}

class LessonItemQuery(sourceId: UUID) : GraphQLQuery<LessonItemQueryResponse> {
    override val queryString = """
        query(${'$'}sourceId: String!) {
            source(id: ${'$'}sourceId) {
                type: __typename
                ... on CustomCardSource {
                    lessonItems {
                        source { id name __typename }
                        cardGroup {
                            iid
                            cards { front back prompt synonyms blockList closeList notes }
                        }
                    }
                }
            }
        }
    """.trimIndent()

    override val variables: Map<String, Any?> = mapOf("sourceId" to sourceId.toString())

    override val responseDeserializer = LessonItemQueryResponse.serializer()
}

@Serializable
class LessonItemQueryResponse(val source: CardSource) {
    @Serializable
    sealed class CardSource {
        @Serializable
        @SerialName("CustomCardSource")
        class CustomCardSource(val lessonItems: List<Reviewer.ReviewItem>) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        object WanikaniCardSource : CardSource()
    }
}
