@file:UseSerializers(UUIDSerializer::class)

package components

import FlashcardsService
import asynclite.async
import flashcards.api.v1.ReviewRequest
import kana.isKana
import kana.kanaToRomaji
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kui.*
import multiplatform.FetchException
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable
import org.w3c.dom.HTMLInputElement

class ReviewScreen(private val service: FlashcardsService, private val deckId: UUID) : Component() {
    private lateinit var cards: Iterator<ReviewCard>
    private var currentItem: ReviewCard? = null
    private var input: String = ""
    private var errored = false
    private var inputState = InputState.WAITING
    private var notesShown = false
    private var reviewMistakeText = ""
    private val summary = mutableListOf<List<Pair<String, Boolean>>>()

    enum class InputState { WAITING, CORRECT, INCORRECT }

    init {
        async {
            cards = service.query(Query.serializer(), "id" to deckId).deck.reviewItems.makeClumps()
            currentItem = cards.next()
            render()
        }
    }

    private fun List<ReviewItem>.makeClumps(): Iterator<ReviewCard> {
        val clumpSize = 10 // how many concurrent unfinished items are being reviewed
        val reviewItems = ArrayDeque(shuffled())
        return iterator {
            val workingGroups = mutableListOf<ReviewGroup>()
            while (reviewItems.isNotEmpty() || workingGroups.isNotEmpty()) {
                while (workingGroups.size < clumpSize && reviewItems.isNotEmpty()) {
                    workingGroups.add(ReviewGroup(reviewItems.removeFirst()))
                }

                val item = workingGroups.random()
                val card = item.cards.filterNot { it.finished }.random()

                yield(card)

                if (item.isFinished()) {
                    workingGroups.remove(item)
                    async {
                        try {
                            summary.add(item.cards.map { "${it.card.front} - ${it.card.prompt}" to (it.timesIncorrect == 0) })
                            service.submitReview(
                                sourceId = item.sourceId,
                                iid = item.iid,
                                request = ReviewRequest(item.cards.map { it.timesIncorrect })
                            )
                        } catch (e: FetchException) {
                            console.error(e)
                            errored = true
                            //render() TODO render a better error message
                            window.alert("An error occurred submitting a review, please reload the page")
                        }
                    }
                }
            }
        }
    }

    private var timeoutHandle: Int? = null
    private fun setMistakeText(s: String) {
        timeoutHandle?.let { window.clearTimeout(it) }
        reviewMistakeText = s
        if (s.isNotEmpty()) {
            timeoutHandle = window.setTimeout({
                reviewMistakeText = ""
                timeoutHandle = null
                render()
            }, 6*1000)
        } else {
            timeoutHandle = null
        }
    }

    private fun checkReview(item: ReviewCard) {
        if (errored) {
            window.alert("An error occurred submitting a review, please reload the page")
        }
        when (inputState) {
            InputState.WAITING -> {
                val result = (sequenceOf(item.card.back) + item.card.synonyms.orEmpty())
                    .map { fuzzyMatch(input, it) }
                    .reduce { a, b -> a.reduce(b)}
                when (result) {
                    FuzzyMatchResult.ALLOW, FuzzyMatchResult.ALLOW_WITH_TYPO -> {
                        item.finished = true
                        inputState = InputState.CORRECT
                        if (result == FuzzyMatchResult.ALLOW_WITH_TYPO) {
                            setMistakeText("Check your answer for typos.")
                        } else {
                            setMistakeText("")
                        }
                    }
                    FuzzyMatchResult.REJECT -> {
                        item.timesIncorrect += 1
                        inputState = InputState.INCORRECT
                        if (item.timesIncorrect > 1) {
                            notesShown = true
                        }
                        setMistakeText("")
                    }
                    FuzzyMatchResult.CLOSE -> {
                        setMistakeText("So close! Double check your answer for typos.")
                    }
                    FuzzyMatchResult.KANA_EXPECTED -> {
                        setMistakeText("Give your answer in hiragana.")
                    }
                }
                render()
            }
            else -> {
                if (cards.hasNext()) {
                    input = ""
                    currentItem = cards.next()
                    inputState = InputState.WAITING
                    notesShown = false
                    render()
                    setInputFocus()
                } else {
                    FlashcardsApp.pushState(ReviewSummary(service, summary))
                }
            }
        }
    }

    private enum class FuzzyMatchResult {
        ALLOW, ALLOW_WITH_TYPO, REJECT, CLOSE, KANA_EXPECTED;

        fun reduce(other: FuzzyMatchResult): FuzzyMatchResult {
            return when {
                this == KANA_EXPECTED || other == KANA_EXPECTED -> KANA_EXPECTED
                this == ALLOW || other == ALLOW -> ALLOW
                this == ALLOW_WITH_TYPO || other == ALLOW_WITH_TYPO -> ALLOW_WITH_TYPO
                this == CLOSE || other == CLOSE -> CLOSE
                this == REJECT || other == REJECT -> REJECT
                else -> throw RuntimeException("unreachable")
            }
        }
    }
    @Suppress("NAME_SHADOWING")
    private fun fuzzyMatch(given: String, expected: String): FuzzyMatchResult {
        val given = given.trim().lowercase()
        val expected = expected.trim().lowercase()

        if (given == expected) return FuzzyMatchResult.ALLOW
        if (given.isBlank()) return FuzzyMatchResult.CLOSE
        if (!expected.isKana() && given.length <=2 && expected.length <= 2) return FuzzyMatchResult.REJECT

        if (expected.isKana() && given.any { it in 'a'..'z' || it.isWhitespace() }) return FuzzyMatchResult.KANA_EXPECTED

        val lev = if (expected.isKana()) {
            levenshtein(kanaToRomaji(given), kanaToRomaji(expected))
        } else {
            levenshtein(given, expected)
        }
        return if (expected.isKana()) {
            if (lev <= 1) FuzzyMatchResult.CLOSE
            else FuzzyMatchResult.REJECT
        } else {
            if (lev <= 2) FuzzyMatchResult.ALLOW_WITH_TYPO
            else FuzzyMatchResult.REJECT
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        var v0 = Array(b.length+1) { it }
        var v1 = Array(b.length+1) { 0 }

        for (i in a.indices) {
            v1[0] = i + 1
            for (j in b.indices) {
                v1[j+1] = minOf(
                    v0[j+1] + 1,
                    v1[j] + 1,
                    v0[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            val t = v0
            v0 = v1
            v1 = t
        }

        return v0.last()
    }

    private fun oops(item: ReviewCard) {
        if (inputState == InputState.WAITING) return

        if (inputState == InputState.INCORRECT) {
            item.timesIncorrect -= 1
        }
        inputState = InputState.WAITING
        notesShown = false
        render()
        setInputFocus()
    }

    private fun handleSpecialKey(event: KeyboardEventArgs, item: ReviewCard) {
        when (event.key) {
            "Enter" -> checkReview(item)
            "Backspace" -> oops(item)
        }
    }

    private fun reviewInputStyle() = when(inputState) {
        InputState.WAITING -> listOf("review-input")
        InputState.CORRECT -> listOf("review-input", "review-input-correct")
        InputState.INCORRECT -> listOf("review-input", "review-input-incorrect")
    }

    private fun String.isCjk() = any { it.code in 0x4E00..0x9FFF }
    private fun String.isKana() = any { it.isKana() }

    private fun setInputFocus() {
        (document.getElementById("review-input") as? HTMLInputElement)?.focus()
    }

    override fun render() {
        val item = currentItem
        if (item == null) {
            markup().p { +"Loading..." }
        } else {
            markup().div {
                div(classes("review-main")) {
                    val props = if (item.card.front.isCjk()) {
                        Props(attrs = mapOf("lang" to "ja"))
                    } else {
                        Props.empty
                    }
                    span(props) { +item.card.front }
                }
                val prompt = item.card.prompt
                if (prompt != null) {
                    div(classes("review-prompt")) {
                        +prompt
                    }
                }
                val inputProps = Props(
                    id = "review-input",
                    classes = reviewInputStyle(),
                    keyup = { handleSpecialKey(it, item) },
                    attrs = if (inputState != InputState.WAITING) mapOf("readonly" to "") else emptyMap()
                )
                div(classes("review-input-container")) {
                    if (item.card.back.isKana()) {
                        component(KanaInput(inputProps, model = ::input))
                    } else {
                        inputText(inputProps, model = ::input)
                    }
                    div(
                        Props(
                            classes = listOfNotNull(
                                "review-mistake-tooltip",
                                "review-mistake-hidden".takeIf { reviewMistakeText.isEmpty() })
                        )
                    ) {
                        +reviewMistakeText
                    }
                }
                div(classes("review-button-container")) {
                    button(Props(
                        classes = listOf("review-button", "review-oops"),
                        click = { oops(item) },
                        disabled = inputState == InputState.WAITING
                    )) { +"Oops" }
                    button(Props(
                        classes = listOf("review-button", "review-notes"),
                        click = { notesShown = !notesShown; render() },
                        disabled = inputState == InputState.WAITING
                    )) { +"Notes" }
                    button(Props(
                        classes = listOf("review-button", "review-next"),
                        click = { checkReview(item) }
                    )) { +">" }
                }
                if (notesShown) {
                    div(Props(classes = listOf("review-notes-panel"), keyup = { handleSpecialKey(it, item) })) {
                        h5 { +"Correct answer: ${item.card.back}" }
                        hr()
                        val notes = item.card.notes
                        if (notes != null) {
                            pre {
                                +notes
                            }
                        }
                    }
                }
            }
        }
    }

    @Serializable
    @GraphQLVariable("id", "String!")
    private class Query(@GraphQLArgument("id", "\$id") val deck: Deck)
    @Serializable
    private class Deck(val reviewItems: List<ReviewItem>)

    @Serializable
    private class ReviewItem(val source: Source, val cardGroup: CardGroup)

    @Serializable
    private class Source(val id: UUID, val __typename: String)

    @Serializable
    private data class CardGroup(val cards: List<Card>, val iid: Int)

    @Serializable
    private data class Card(
        val front: String,
        val back: String,
        val prompt: String? = null,
        val synonyms: List<String>? = null,
        val notes: String? = null,
    )

    private class ReviewGroup(reviewItem: ReviewItem) {
        val cards = reviewItem.cardGroup.cards.map { ReviewCard(it) }
        val sourceId = reviewItem.source.id
        val iid = reviewItem.cardGroup.iid
        fun isFinished() = cards.all { it.finished }
    }
    private class ReviewCard(val card: Card) {
        var timesIncorrect = 0
        var finished = false
    }
}
