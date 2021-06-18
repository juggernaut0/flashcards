package flashcards.graphql

import auth.ValidatedToken
import flashcards.AccountService
import flashcards.SrsService
import flashcards.api.v1.*
import flashcards.db.*
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import io.ktor.auth.*
import io.ktor.routing.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.*
import multiplatform.ktor.handleApi
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun Route.registerRoutes(handler: GraphQLHandler) {
    authenticate {
        handleApi(query) { handler.handle(auth as ValidatedToken, it) }
    }
}

class GraphQLHandler @Inject constructor(
    private val accountService: AccountService,
    private val deckDao: DeckDao,
    private val database: Database,
    private val sourceDao: CardSourceDao,
    private val srsService: SrsService,
) {
    private val graphql = schema {
        query {
            field("sources", ListSerializer(CardSource.serializer())) {
                val ctx = coroutineContext[QueryContext]!!
                sourceDao.getSources(ctx.dslContext, ctx.accountId).map { it.toSchemaModel() }
            }

            field("source", CardSource.serializer().nullable, IdParam.serializer()) { (id) ->
                val ctx = coroutineContext[QueryContext]!!
                sourceDao.getSource(ctx.dslContext, ctx.accountId, id)?.toSchemaModel()
            }

            field("decks", ListSerializer(Deck.serializer())) {
                val ctx = coroutineContext[QueryContext]!!
                deckDao.getDecks(ctx.dslContext, ctx.accountId).map { it.toSchemaModel() }
            }

            field("deck", Deck.serializer().nullable, IdParam.serializer()) { (id) ->
                val ctx = coroutineContext[QueryContext]!!
                deckDao.getDeck(ctx.dslContext, ctx.accountId, id)?.toSchemaModel()
            }
        }

        `interface`(CardSource.serializer())
        type(CardGroup.serializer())
        type(Card.serializer())
        type(Deck.serializer()) {
            field("lessons", Int.serializer()) { 0 }
            field("reviews", Int.serializer()) {
                val ctx = coroutineContext[QueryContext]!!
                val now = Instant.now()
                sources.mapNotNull { sourceDao.getSource(ctx.dslContext, ctx.accountId, it)?.toSchemaModel() }
                    .sumOf { source ->
                        when (source) {
                            is CardSource.CustomCardSource -> source.groups.count { srsService.isUpForReview(it, now) }
                            else -> 0
                        }
                    }
            }
            field("sources", ListSerializer(CardSource.serializer())) {
                val ctx = coroutineContext[QueryContext]!!
                sources.mapNotNull { sourceDao.getSource(ctx.dslContext, ctx.accountId, it)?.toSchemaModel() }
            }
            field("reviewItems", ListSerializer(ReviewItem.serializer())) {
                val ctx = coroutineContext[QueryContext]!!
                val now = Instant.now()
                sources.mapNotNull { sourceDao.getSource(ctx.dslContext, ctx.accountId, it)?.toSchemaModel() }
                    .flatMap { source ->
                        when (source) {
                            is CardSource.CustomCardSource -> source.groups.filter { srsService.isUpForReview(it, now) }
                            else -> emptyList()
                        }.map { group -> ReviewItem(source.id, group) }
                    }
            }
        }
        type(ReviewItem.serializer()) {
            field("source", CardSource.serializer()) {
                val ctx = coroutineContext[QueryContext]!!
                sourceDao.getSource(ctx.dslContext, ctx.accountId, sourceId)!!.toSchemaModel()
            }
        }
    }.also { logSchema(it) }.let { graphQL(it) }

    private fun logSchema(schema: GraphQLSchema) {
        val log = LoggerFactory.getLogger(GraphQLHandler::class.java)
        log.debug(SchemaPrinter(SchemaPrinter.Options.defaultOptions().includeDirectives(false)).print(schema))
    }

    suspend fun handle(token: ValidatedToken, request: GraphQLRequest): GraphQLResponse {
        val accountId = accountService.ensureAccount(token)
        return database.transaction { dsl ->
            withContext(QueryContext(dsl, accountId)) {
                graphql.executeSuspend(request)
            }
        }
    }

    private fun CardSourceData.toSchemaModel(): CardSource {
        return when (val type = cardSource.type) {
            "custom" -> CardSource.CustomCardSource(cardSource.id, cardSource.name, groups!!)
            "wanikani" -> CardSource.WanikaniCardSource(cardSource.id, cardSource.name)
            else -> throw IllegalStateException("Unknown source type: $type")
        }
    }

    private fun DeckData.toSchemaModel(): Deck {
        return Deck(
            id = deckRecord.id,
            name = deckRecord.name,
            sources = sources,
        )
    }

    @Serializable
    data class IdParam(@Serializable(with = UUIDSerializer::class) val id: UUID)

    class QueryContext(val dslContext: DSLContext, val accountId: UUID) : CoroutineContext.Element {
        override val key: CoroutineContext.Key<QueryContext> = QueryContext

        companion object : CoroutineContext.Key<QueryContext>
    }
}
