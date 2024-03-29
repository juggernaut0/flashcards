@file:UseSerializers(UUIDSerializer::class)

package review

import asynclite.async
import components.FlashcardsApp
import kotlinx.serialization.UseSerializers
import kui.*
import multiplatform.UUIDSerializer

class ReviewScreen(private val model: ReviewModel) : Component() {
    private var items: List<Reviewer.ReviewItem> = emptyList()

    init {
        async {
            items = model.getItems()
            render()
        }
    }

    override fun render() {
        if (items.isEmpty()) {
            markup().p { +"Loading..." }
        } else {
            markup().component(
                Reviewer(
                    items = items,
                    onSubmit = { model.submit(it) },
                    onComplete = { FlashcardsApp.pushReviewSummary(it) }
                )
            )
        }
    }
}
