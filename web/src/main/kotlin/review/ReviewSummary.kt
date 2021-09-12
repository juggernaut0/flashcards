package review

import components.Header
import kui.Component
import kui.MarkupBuilder
import kui.classes

class ReviewSummary : Component() {
    private val correct: List<ReviewSummaryItem>
    private val incorrect: List<ReviewSummaryItem>

    init {
        val (correct, incorrect) = data.partition { item -> item.correct.all { it } }
        this.correct = correct
        this.incorrect = incorrect
    }

    override fun render() {
        markup().div(classes("container")) {
            component(Header())
            h2 { +"Review Summary" }
            column("Correct", correct)
            column("Incorrect", incorrect)
        }
    }

    private fun MarkupBuilder.column(title: String, items: List<ReviewSummaryItem>) {
        div(classes("review-summary-column")) {
            h4 { +title }
            hr()
            for (item in items) {
                for ((card, correct) in item.group.cards.zip(item.correct)) {
                    val clazz = if (correct) "review-summary-correct" else "review-summary-incorrect"
                    div(classes(clazz)) { +card.toDisplayString() }
                }
                hr()
            }
        }
    }

    companion object {
        var data: List<ReviewSummaryItem> = emptyList()
    }
}

class ReviewSummaryItem(val group: Reviewer.CardGroup, val correct: List<Boolean>)
