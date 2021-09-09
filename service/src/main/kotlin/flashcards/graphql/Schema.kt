@file:UseSerializers(UUIDSerializer::class)

package flashcards.graphql

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer

@Serializable
sealed class CardSource {
    @Serializable
    class WanikaniCardSource(override val id: UUID, override val name: String) : CardSource()
    @Serializable
    class CustomCardSource(override val id: UUID, override val name: String, val groups: List<CardGroup>) : CardSource()
    abstract val id: UUID
    abstract val name: String
}

@Serializable
data class CardGroup(val cards: List<Card>, val iid: Int, val srsStage: Int, val lastReviewed: Instant?)
typealias Card = flashcards.api.v1.Card

@Serializable
class Deck(val id: UUID, val name: String, val sourceIds: List<UUID> = emptyList())

@Serializable
class ReviewItem(val sourceId: UUID, val cardGroup: CardGroup)

@Serializable
data class IdParam(@Serializable(with = UUIDSerializer::class) val id: UUID)

@Serializable
data class ReviewForecastItem(val time: Instant, val count: Int)
