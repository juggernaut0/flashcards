@file:UseSerializers(UUIDSerializer::class)

package review

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
import wanikani.toCardGroup

class ReviewModel(
    private val flashcardsService: FlashcardsService,
    private val wanikaniService: WanikaniService,
    private val deckId: UUID,
) {
    suspend fun getItems(): List<Reviewer.ReviewItem> {
        val sources = flashcardsService.query(Query(deckId)).deck.sources
        return sources.flatMap { source ->
            when(source) {
                is CardSource.CustomCardSource -> source.reviewItems
                is CardSource.WanikaniCardSource -> {
                    val wkAccount = wanikaniService.forSource(source.id)
                    val itemSource = Reviewer.Source(source.id, source.name, "WanikaniCardSource")
                    wkAccount.getReviews().mapNotNull { assignment ->
                        val subjectId = assignment.data.subjectId
                        val subject = wkAccount.getSubject(subjectId) ?: return@mapNotNull null
                        val studyMaterial = wkAccount.getStudyMaterial(subjectId)
                        val group = toCardGroup(assignment, subject, studyMaterial)
                        Reviewer.ReviewItem(itemSource, group)
                    }
                }
            }
        }
    }

    suspend fun submit(result: Reviewer.ReviewResult) {
        when(val typename = result.item.source.__typename) {
            "CustomCardSource" -> {
                flashcardsService.submitReview(
                    sourceId = result.item.source.id,
                    iid = result.item.cardGroup.iid.toInt(),
                    request = ReviewRequest(result.timesIncorrect)
                )
            }
            "WanikaniCardSource" -> {
                val meaningIncorrect = result.timesIncorrect[0]
                val readingIncorrect = result.timesIncorrect.getOrElse(1) { 0 }
                wanikaniService.forSource(result.item.source.id)
                    .createReview(result.item.cardGroup.iid, meaningIncorrect, readingIncorrect)
            }
            else -> console.warn("Unknown card source type '$typename'")
        }
    }

    private class Query(id: UUID) : GraphQLQuery<QueryResponse> {
        override val queryString = """
            query(${'$'}id: String!) {
                deck(id: ${'$'}id) {
                    sources {
                        type: __typename
                        ... on WanikaniCardSource { id name }
                        ... on CustomCardSource {
                            reviewItems {
                                source { id name __typename }
                                cardGroup {
                                    iid
                                    cards { front back prompt synonyms blockList closeList notes }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        override val variables = mapOf("id" to id.toString())

        override val responseDeserializer = QueryResponse.serializer()
    }

    @Serializable
    private class QueryResponse(val deck: Deck)
    @Serializable
    class Deck(val sources: List<CardSource>)
    @Serializable
    sealed class CardSource {
        @Serializable
        @SerialName("CustomCardSource")
        class CustomCardSource(val reviewItems: List<Reviewer.ReviewItem>) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(val id: UUID, val name: String) : CardSource()
    }
}
