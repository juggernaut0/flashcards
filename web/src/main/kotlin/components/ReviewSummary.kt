package components

import FlashcardsService
import kui.Component
import kui.classes

class ReviewSummary(private val service: FlashcardsService, private val data: List<List<Pair<String, Boolean>>>) : Component() {
    override fun render() {
        markup().div(classes("container")) {
            component(Header(service))
            h2 { +"Review Summary" }
            div {
                hr()
                for (group in data) {
                    for ((card, correct) in group) {
                        val clazz = if (correct) "review-summary-correct" else "review-summary-incorrect"
                        div(classes(clazz)) { +card }
                    }
                    hr()
                }
            }
        }
    }
}
