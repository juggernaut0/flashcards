package lesson

import asynclite.async
import components.FlashcardsApp
import components.Header
import components.Modal
import components.cardDetails
import kui.*
import review.ReviewSummaryItem
import review.Reviewer

class LessonScreen(private val model: LessonModel) : Component() {
    private var items: List<Reviewer.ReviewItem> = emptyList()
    private var remainingItems = 0
    private var currentItem = 0
    private var learnMode = true
    private val summaryData: MutableList<ReviewSummaryItem> = mutableListOf()

    init {
        async {
            items = getItems()
            render()
        }
    }

    private suspend fun getItems(): List<Reviewer.ReviewItem> {
        val data = model.getData()
        val items = data.items
        remainingItems = data.totalLessons
        return items
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
            FlashcardsApp.pushReviewSummary(summaryData)
            return
        }
        async {
            if (Modal.suspendShow("Continue Lessons", componentOf { it.p { +"Continue with more lessons?" } })) {
                val items = getItems()
                check(items.isNotEmpty()) { "items must not be empty" }
                this.items = items
                render()
            } else {
                FlashcardsApp.pushReviewSummary(summaryData)
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
                    component(Header())
                    div(classes("row")) {
                        for (card in item.cardGroup.cards) {
                            h3 { +card.toDisplayString() }
                            cardDetails(card)
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
                markup().component(Reviewer(
                    items = items,
                    onSubmit = { model.submit(it) },
                    onComplete = { summaryData.addAll(it); remainingItems -= it.size; nextBatch() }
                ))
            }
        }
    }
}