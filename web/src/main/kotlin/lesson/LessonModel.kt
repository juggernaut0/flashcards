@file:UseSerializers(UUIDSerializer::class)

package lesson

import FlashcardsService
import WanikaniService
import components.Reviewer
import flashcards.api.v1.ReviewRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable
import wanikani.KanjiSubject
import wanikani.RadicalSubject
import wanikani.VocabularySubject

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
        val items = deck.sources
            .flatMapTake(totalLessons.coerceAtMost(5)) { source ->
                when(source) {
                    is LessonScreenQuery.CardSource.CustomCardSource -> getCustomLessons(source.id)
                    is LessonScreenQuery.CardSource.WanikaniCardSource -> {
                        val wkAccount = wanikaniService.forSource(source.id)
                        val itemSource = Reviewer.Source(source.id, source.name, "WanikaniCardSource")
                        wkAccount.getLessons().mapNotNull { assignment ->
                            val subject = wkAccount.getSubject(assignment.data.subjectId) ?: return@mapNotNull null
                            // TODO for notes, remove or process WK markup
                            val cards = when (val data = subject.data) {
                                is RadicalSubject -> {
                                    val front = data.characters
                                        ?: data.characterImages.find {
                                            it.contentType == "image/svg+xml" && it.metadata.inline_styles == true
                                        }?.url
                                        ?: data.slug
                                    val (pri, syn) = data.meanings.partition { it.primary }
                                    listOf(Reviewer.Card(
                                        front = front,
                                        back = pri.first().meaning,
                                        prompt = "Radical Meaning",
                                        synonyms = syn.map { it.meaning },
                                        notes = data.meaningMnemonic
                                    ))
                                }
                                is KanjiSubject -> {
                                    val front = data.characters
                                    val (reading, readingSyn) = data.readings.filter { it.accepted_answer }.partition { it.primary }
                                    val readingPrompt = when (reading.first().type) {
                                        "kunyomi" -> "Kanji Kun'yomi"
                                        "onyomi" -> "Kanji On'yomi"
                                        "nanori" -> "Kanji Nanori"
                                        else -> "Kanji Reading"
                                    }
                                    val readingNotes = data.reading_mnemonic + data.reading_hint?.let { "\n\n$it" }.orEmpty()
                                    val (meaning, meaningSyn) = data.meanings.partition { it.primary }
                                    val meaningNotes = data.meaning_mnemonic + data.meaning_hint?.let { "\n\n$it" }.orEmpty()
                                    listOf(
                                        Reviewer.Card(
                                            front = front,
                                            back = meaning.first().meaning,
                                            prompt = "Kanji Meaning",
                                            synonyms = meaningSyn.map { it.meaning },
                                            notes = meaningNotes
                                        ),
                                        Reviewer.Card(
                                            front = front,
                                            back = reading.first().reading,
                                            prompt = readingPrompt,
                                            synonyms = readingSyn.map { it.reading },
                                            notes = readingNotes,
                                        ),
                                    )
                                }
                                is VocabularySubject -> {
                                    val front = data.characters
                                    val (reading, readingSyn) = data.readings.filter { it.accepted_answer }.partition { it.primary }
                                    val readingNotes = data.reading_mnemonic + data.reading_hint?.let { "\n\n$it" }.orEmpty()
                                    val (meaning, meaningSyn) = data.meanings.partition { it.primary }
                                    val meaningNotes = data.meaning_mnemonic + data.meaning_hint?.let { "\n\n$it" }.orEmpty()
                                    listOf(
                                        Reviewer.Card(
                                            front = front,
                                            back = meaning.first().meaning,
                                            prompt = "Vocab Meaning",
                                            synonyms = meaningSyn.map { it.meaning },
                                            notes = meaningNotes
                                        ),
                                        Reviewer.Card(
                                            front = front,
                                            back = reading.first().reading,
                                            prompt = "Vocab Reading",
                                            synonyms = readingSyn.map { it.reading },
                                            notes = readingNotes,
                                        ),
                                    )
                                }
                            }
                            val group = Reviewer.CardGroup(cards, iid = assignment.id)
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
class LessonItemQuery(@GraphQLArgument("sourceId", "\$id")  val source: CardSource) {
    @Serializable
    class CardSource(val lessonItems: List<Reviewer.ReviewItem>)
}
