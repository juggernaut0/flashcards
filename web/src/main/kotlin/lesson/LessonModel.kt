@file:UseSerializers(UUIDSerializer::class)

package lesson

import FlashcardsService
import WanikaniService
import flashcards.api.v1.ReviewRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable
import review.Reviewer
import wanikani.toCardGroup

class LessonModel(
    private val flashcardsService: FlashcardsService,
    private val wanikaniService: WanikaniService,
    private val deckId: UUID,
) {
    suspend fun getData(): LessonScreenData {
        val deck = flashcardsService.query(LessonScreenQuery.serializer(), "id" to deckId).deck
        val totalLessons = deck.sources.sumOf {
            when (it) {
                is LessonScreenQuery.CardSource.CustomCardSource -> it.lessons
                is LessonScreenQuery.CardSource.WanikaniCardSource -> wanikaniService.forSource(it.id).getLessons().size
            }
        }
        // TODO number of items based on deck settings
        val items = deck.sources
            .flatMapTake(totalLessons.coerceAtMost(5)) { source ->
                when(source) {
                    is LessonScreenQuery.CardSource.CustomCardSource -> getCustomLessons(source.id)
                    is LessonScreenQuery.CardSource.WanikaniCardSource -> {
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
        return flashcardsService.query(LessonItemQuery.serializer(), "sourceId" to sourceId)
            .source
            .let { it as LessonItemQuery.CardSource.CustomCardSource }
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

@Serializable
@GraphQLVariable("id", "String!")
class LessonScreenQuery(@GraphQLArgument("id", "\$id") val deck: Deck) {
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

@Serializable
@GraphQLVariable("sourceId", "String!")
class LessonItemQuery(@GraphQLArgument("id", "\$sourceId")  val source: CardSource) {
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
