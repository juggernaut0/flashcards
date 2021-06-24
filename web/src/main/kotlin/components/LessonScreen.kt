package components

import FlashcardsService
import asynclite.async
import kotlinx.serialization.Serializable
import kui.Component
import kui.Props
import kui.classes
import kui.componentOf
import multiplatform.UUID
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable

class LessonScreen(private val service: FlashcardsService, private val deckId: UUID) : Component() {
    private var items: List<Reviewer.ReviewItem> = emptyList()
    private var remainingItems = 0
    private var currentItem = 0
    private var learnMode = true
    private val summaryData: ReviewSummaryData = mutableListOf()

    init {
        async {
            items = getItems()
            render()
        }
    }

    private suspend fun getItems(): List<Reviewer.ReviewItem> {
        val deck = service.query(Query.serializer(), "id" to deckId).deck
        remainingItems = deck.lessons
        return deck.lessonItems
    }

    private fun nextItem() {
        if (currentItem == items.lastIndex) {
            learnMode = false
            currentItem = 0
            render()
        } else {
            currentItem++
            render()
        }
    }

    private fun nextBatch() {
        learnMode = true
        if (remainingItems == 0) {
            FlashcardsApp.pushState(ReviewSummary(service, summaryData))
            return
        }
        async {
            if (Modal.suspendShow("Continue Lessons", componentOf { it.p { +"Continue with more lessons?" } })) {
                val items = getItems()
                check(items.isNotEmpty()) { "items must not be empty" }
                this.items = items
                render()
            } else {
                FlashcardsApp.pushState(ReviewSummary(service, summaryData))
            }
        }
    }

    override fun render() {
        val items = items
        when {
            items.isEmpty() -> markup().p { +"Loading..." }
            learnMode -> {
                val item = items[currentItem]
                markup().div(classes("container")) {
                    component(Header(service))
                    div(classes("row")) {
                        for (card in item.cardGroup.cards) {
                            h3 { +"${card.front} - ${card.prompt}" }
                            h4 { +card.back }
                            if (!card.notes.isNullOrBlank()) {
                                p { +card.notes }
                            }
                            hr()
                        }
                    }
                    button(Props(
                        classes = listOf("button-next-lesson", "button-confirm"),
                        click = { nextItem() }
                    )) {
                        if (currentItem == items.lastIndex) {
                            +"Quiz"
                        } else {
                            +"Next Item"
                        }
                    }
                }
            }
            else -> {
                markup().component(Reviewer(service, items, onComplete = { summaryData.addAll(it); remainingItems -= it.size; nextBatch() }, lessonMode = true))
            }
        }
    }

    @Serializable
    @GraphQLVariable("id", "String!")
    private class Query(@GraphQLArgument("id", "\$id") val deck: Deck)
    @Serializable
    private class Deck(@GraphQLArgument("limit", "5") val lessonItems: List<Reviewer.ReviewItem>, val lessons: Int)
}