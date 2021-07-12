package review

import asynclite.async
import components.KanaInput
import components.cardDetails
import kana.isCjk
import kana.isKana
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kui.Component
import kui.KeyboardEventArgs
import kui.Props
import kui.classes
import multiplatform.FetchException
import multiplatform.UUID
import multiplatform.UUIDSerializer
import org.w3c.dom.Audio
import org.w3c.dom.HTMLInputElement

typealias ReviewSummaryData = MutableList<List<Pair<String, Boolean>>>

class Reviewer(
    items: List<ReviewItem>,
    private val onComplete: (ReviewSummaryData) -> Unit,
    private val onSubmit: suspend (ReviewResult) -> Unit,
) : Component() {
    init {
        require(items.isNotEmpty()) { "items must not be empty" }
    }

    private val totalItems = items.size
    private val cards: Iterator<ReviewCard> = items.makeClumps()
    private var currentItem: ReviewCard = cards.next()
    private var input: String = ""
    private var errored = false
    private var inputState = InputState.WAITING
    private var notesShown = false
    private var reviewMistakeText = ""
    private val summary = mutableListOf<List<Pair<String, Boolean>>>()

    enum class InputState { WAITING, CORRECT, INCORRECT }

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
                            summary.add(item.cards.map { it.card.toDisplayString() to (it.timesIncorrect == 0) })
                            onSubmit(ReviewResult(item))
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
                        item.card.audioUrls.orEmpty().find { it.text == input }?.let {
                            val audio = Audio(it.url)
                            audio.oncanplaythrough = { audio.play() } // TODO preload audio?
                        }
                        if (result == FuzzyMatchResult.ALLOW_WITH_TYPO) {
                            setMistakeText("Check your answer for typos.")
                        } else if (!item.card.synonyms.isNullOrEmpty()) {
                            setMistakeText("Check notes for additional answers.")
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
                    setMistakeText("")
                    render()
                    setInputFocus()
                } else {
                    onComplete(summary)
                }
            }
        }
    }

    private fun oops(item: ReviewCard) {
        when (inputState) {
            InputState.WAITING -> return
            InputState.INCORRECT -> item.timesIncorrect -= 1
            InputState.CORRECT -> item.finished = false
        }
        inputState = InputState.WAITING
        notesShown = false
        setMistakeText("")
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

    private fun setInputFocus() {
        (document.getElementById("review-input") as? HTMLInputElement)?.focus()
    }

    override fun render() {
        val item = currentItem
        markup().div {
            div(classes("review-main")) {
                div(classes("review-main-header")) {
                    span { +item.source.name }
                    span { +"${summary.size}/$totalItems (${(summary.size.toDouble() * 100 / totalItems).toInt()}%)" }
                }

                if (item.card.front.startsWith("https://")) {
                    img(Props(attrs = mapOf("width" to "96", "height" to "96")), src = item.card.front)
                } else {
                    val props = if (item.card.front.isCjk()) {
                        Props(attrs = mapOf("lang" to "ja"))
                    } else {
                        Props.empty
                    }
                    span(props) { +item.card.front }
                }
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
                button(
                    Props(
                    classes = listOf("review-button", "review-oops"),
                    click = { oops(item) },
                    disabled = inputState == InputState.WAITING
                )
                ) { +"Oops" }
                button(
                    Props(
                    classes = listOf("review-button", "review-notes"),
                    click = { notesShown = !notesShown; render() },
                    disabled = inputState == InputState.WAITING
                )
                ) { +"Notes" }
                button(Props(
                    classes = listOf("review-button", "review-next"),
                    click = { checkReview(item) }
                )) { +">" }
            }
            if (notesShown) {
                div(Props(classes = listOf("review-notes-panel"), keyup = { handleSpecialKey(it, item) })) {
                    cardDetails(item.card)
                }
            }
        }
    }

    @Serializable
    class ReviewItem(val source: Source, val cardGroup: CardGroup)
    @Serializable
    class Source(@Serializable(with = UUIDSerializer::class) val id: UUID, val name: String, val __typename: String)
    @Serializable
    data class CardGroup(val cards: List<Card>, val iid: Long)
    @Serializable
    data class Card(
        val front: String,
        val back: String,
        val prompt: String? = null,
        val synonyms: List<String>? = null,
        val notes: String? = null,
        @Transient val audioUrls: List<AudioUrl>? = null
    ) {
        fun toDisplayString(): String = buildString {
            append(front)
            if (prompt != null) {
                append(" - ", prompt)
            }
        }
    }
    class AudioUrl(val url: String, val text: String?)

    class ReviewResult(val item: ReviewItem, val timesIncorrect: List<Int>)

    private fun ReviewResult(group: ReviewGroup) = ReviewResult(group.reviewItem, group.cards.map { it.timesIncorrect })

    private class ReviewGroup(val reviewItem: ReviewItem) {
        val cards = reviewItem.cardGroup.cards.map { ReviewCard(it, reviewItem.source) }
        fun isFinished() = cards.all { it.finished }
    }
    private class ReviewCard(val card: Card, val source: Source) {
        var timesIncorrect = 0
        var finished = false
    }
}