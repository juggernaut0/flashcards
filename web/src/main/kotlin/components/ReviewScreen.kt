@file:UseSerializers(UUIDSerializer::class)

package components

import FlashcardsService
import asynclite.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kui.*
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable

class ReviewScreen(private val service: FlashcardsService, private val deckId: UUID) : Component() {
    private var items: List<Reviewer.ReviewItem> = emptyList()

    init {
        async {
            items = service.query(Query.serializer(), "id" to deckId).deck.reviewItems
            render()
        }
    }

    override fun render() {
        if (items.isEmpty()) {
            markup().p { +"Loading..." }
        } else {
            markup().component(
                Reviewer(
                    service = service,
                    items = items,
                    onComplete = { FlashcardsApp.pushState(ReviewSummary(service, it)) })
            )
        }
    }

    @Serializable
    @GraphQLVariable("id", "String!")
    private class Query(@GraphQLArgument("id", "\$id") val deck: Deck)
    @Serializable
    private class Deck(val reviewItems: List<Reviewer.ReviewItem>)




}
