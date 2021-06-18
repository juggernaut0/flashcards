package flashcards.api.v1

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.api.ApiRoute
import multiplatform.api.Method
import multiplatform.api.pathOf
import multiplatform.graphql.GraphQLRequest
import multiplatform.graphql.GraphQLResponse

@Serializable
data class IdParam(@Serializable(with = UUIDSerializer::class) val id: UUID)

@Serializable
data class ReviewParam(@Serializable(with = UUIDSerializer::class) val sourceId: UUID, @Serializable(with = IntAsStringSerializer::class) val iid: Int)

const val APP_CONTEXT = "/flashcards"
val query = ApiRoute(Method.POST, pathOf(Unit.serializer(), "$APP_CONTEXT/graphql"), GraphQLResponse.serializer(), GraphQLRequest.serializer())
val createSource = ApiRoute(Method.POST, pathOf(Unit.serializer(), "$APP_CONTEXT/api/v1/sources"), UUIDSerializer, CardSourceRequest.serializer())
val updateSource = ApiRoute(Method.PUT, pathOf(IdParam.serializer(), "$APP_CONTEXT/api/v1/sources/{id}"), Unit.serializer(), CardSourceRequest.serializer())
val submitReview = ApiRoute(Method.POST, pathOf(ReviewParam.serializer(), "$APP_CONTEXT/api/v1/sources/{sourceId}/{iid}"), Unit.serializer(), ReviewRequest.serializer())
val createDeck = ApiRoute(Method.POST, pathOf(Unit.serializer(), "$APP_CONTEXT/api/v1/decks"), UUIDSerializer, DeckRequest.serializer())
val updateDeck = ApiRoute(Method.PUT, pathOf(IdParam.serializer(), "$APP_CONTEXT/api/v1/decks/{id}"), Unit.serializer(), DeckRequest.serializer())
