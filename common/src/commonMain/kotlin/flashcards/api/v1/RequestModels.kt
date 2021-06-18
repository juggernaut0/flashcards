@file:UseSerializers(UUIDSerializer::class)

package flashcards.api.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import multiplatform.UUID
import multiplatform.UUIDSerializer

@Serializable
sealed class CardSourceRequest {
    abstract val name: String?
}

@Serializable
@SerialName("custom")
data class CustomCardSourceRequest(override val name: String? = null, val groups: List<CardGroup>? = null) : CardSourceRequest()

@Serializable
data class CardGroup(val cards: List<Card>, val iid: Int)

@Serializable
data class Card(
    val front: String,
    val back: String,
    val prompt: String? = null,
    val synonyms: List<String>? = null,
    val notes: String? = null,
)

@Serializable
@SerialName("wanikani")
data class WanikaniCardSourceRequest(override val name: String? = null) : CardSourceRequest()

@Serializable
data class DeckRequest(val name: String? = null, val sources: List<UUID>? = null)

@Serializable
data class ReviewRequest(val timesIncorrect: List<Int>)
