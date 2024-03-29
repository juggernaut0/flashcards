package flashcards

import auth.ktor.ValidatedToken
import flashcards.api.v1.*
import flashcards.db.CardSourceDao
import flashcards.db.Database
import flashcards.db.DeckDao
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import multiplatform.ktor.BadRequestException
import multiplatform.ktor.handleApi
import java.util.*
import javax.inject.Inject
import flashcards.graphql.CardGroup as GraphqlCardGroup

fun Route.registerRoutes(handler: ApiHandler) {
    authenticate {
        handleApi(createSource) { handler.createSource(auth as ValidatedToken, it) }
        handleApi(reorderSources) { handler.reorderSources(auth as ValidatedToken, it) }
        handleApi(updateSource) { handler.updateSource(auth as ValidatedToken, params.id, it) }
        handleApi(submitReview) { handler.submitReview(auth as ValidatedToken, params.sourceId, params.iid, it) }
        handleApi(resetReview) { handler.resetReview(auth as ValidatedToken, params.sourceId, params.iid) }

        handleApi(createDeck) { handler.createDeck(auth as ValidatedToken, it) }
        handleApi(reorderDecks) { handler.reorderDecks(auth as ValidatedToken, it) }
        handleApi(updateDeck) { handler.updateDeck(auth as ValidatedToken, params.id, it) }
    }
}

class ApiHandler @Inject constructor(
    private val accountService: AccountService,
    private val deckDao: DeckDao,
    private val sourceDao: CardSourceDao,
    private val database: Database,
    private val srsService: SrsService,
) {
    suspend fun createSource(token: ValidatedToken, request: CardSourceRequest): UUID {
        val accountId = accountService.ensureAccount(token)
        val name = request.name ?: throw BadRequestException("Missing required field: name")
        val type = when (request) {
            is CustomCardSourceRequest -> "custom"
            is WanikaniCardSourceRequest -> "wanikani"
        }
        val cards = (request as? CustomCardSourceRequest)?.let { req ->
            req.groups.orEmpty()
                .also { checkUniqueIids(it) }
                .map { GraphqlCardGroup(it.cards, it.iid, 0, Instant.DISTANT_PAST) }
        }
        return database.transaction { dsl ->
            sourceDao.createSource(dsl, accountId, name, type, cards)
        }
    }

    suspend fun reorderSources(token: ValidatedToken, newOrder: List<UUID>) {
        val accountId = accountService.ensureAccount(token)
        database.transaction { dsl ->
            val allSources = sourceDao.getSources(dsl, accountId).map { it.cardSource.id }
            if (!newOrder.sameContents(allSources)) {
                throw BadRequestException("Reordering list must contain every source ID exactly once")
            }
            sourceDao.reorderSources(dsl, accountId, newOrder)
        }
    }

    suspend fun updateSource(token: ValidatedToken, id: UUID, request: CardSourceRequest) {
        val accountId = accountService.ensureAccount(token)
        database.transaction { dsl ->
            val source = sourceDao.getSource(dsl, accountId, id, lock = true)
                ?: throw BadRequestException("Source with id [$id] not found")

            when {
                request is CustomCardSourceRequest && source.cardSource.type == "custom" -> {
                    val customCards = request.groups?.let { reqGroups ->
                        checkUniqueIids(reqGroups)
                        migrateGroups(reqGroups, source.groups!!)
                    }
                    sourceDao.updateSource(dsl, accountId, id,
                        name = request.name,
                        customCards = customCards
                    )
                }
                request is WanikaniCardSourceRequest && source.cardSource.type == "wanikani" -> {
                    sourceDao.updateSource(dsl, accountId, id,
                        name = request.name
                    )
                }
                else -> throw BadRequestException("Error: mismatch")
            }
        }
    }

    private fun checkUniqueIids(groups: List<CardGroup>) {
        val iids = groups.mapTo(mutableSetOf()) { it.iid }
        if (iids.size != groups.size) throw BadRequestException("Each card group must have a unique iid")
    }

    private fun migrateGroups(reqGroups: List<CardGroup>, dbGroups: List<GraphqlCardGroup>): List<GraphqlCardGroup> {
        val groupsByIid = dbGroups.associateBy { it.iid }
        return reqGroups.map { group: CardGroup ->
            val dbGroup = groupsByIid[group.iid]
            GraphqlCardGroup(
                cards = group.cards,
                iid = group.iid,
                srsStage = dbGroup?.srsStage ?: 0,
                lastReviewed = dbGroup?.lastReviewed,
            )
        }
    }

    suspend fun submitReview(token: ValidatedToken, sourceId: UUID, iid: Int, request: ReviewRequest) {
        val now = Clock.System.now()
        val accountId = accountService.ensureAccount(token)
        database.transaction { dsl ->
            val source = sourceDao.getSource(dsl, accountId, sourceId, lock = true)
                ?: throw BadRequestException("Source with id [$sourceId] not found")
            val groups = source.groups
                ?: throw BadRequestException("Can only submit reviews for custom card sources")
            val group = groups.find { it.iid == iid }
                ?: throw BadRequestException("Card Group with iid [$iid] not found")
            if (!srsService.isUpForLesson(group) && !srsService.isUpForReview(group))
                throw BadRequestException("Card Group with iid [$iid] is not up for review")

            val newStage = srsService.adjustStage(group.srsStage, request.timesIncorrect)
            val newGroups = groups.map {
                if (it.iid != iid) it
                else GraphqlCardGroup(cards = it.cards, iid = it.iid, srsStage = newStage, lastReviewed = now)
            }
            sourceDao.updateSource(dsl, accountId, sourceId, customCards = newGroups)
        }
    }

    suspend fun resetReview(token: ValidatedToken, sourceId: UUID, iid: Int) {
        val accountId = accountService.ensureAccount(token)
        database.transaction { dsl ->
            val source = sourceDao.getSource(dsl, accountId, sourceId, lock = true)
                ?: throw BadRequestException("Source with id [$sourceId] not found")
            val groups = source.groups
                ?: throw BadRequestException("Can only reset reviews for custom card sources")
            val groupIndex = groups.indexOfFirst { it.iid == iid }
                .takeUnless { it < 0 }
                ?: throw BadRequestException("Card Group with iid [$iid] not found")
            val newGroup = groups[groupIndex].let {
                GraphqlCardGroup(cards = it.cards, iid = it.iid, srsStage = 0, lastReviewed = null)
            }
            val newGroups = groups.toMutableList().also { it[groupIndex] = newGroup }
            sourceDao.updateSource(dsl, accountId, sourceId, customCards = newGroups)
        }
    }

    suspend fun createDeck(token: ValidatedToken, request: DeckRequest): UUID {
        val name = request.name ?: throw BadRequestException("Missing required field: name")
        val accountId = accountService.ensureAccount(token)
        return database.transaction { dsl ->
            deckDao.createDeck(dsl, accountId, name, request.sources.orEmpty())
        }
    }

    suspend fun reorderDecks(token: ValidatedToken, newOrder: List<UUID>) {
        val accountId = accountService.ensureAccount(token)
        database.transaction { dsl ->
            val allDecks = deckDao.getDecks(dsl, accountId).map { it.deckRecord.id }
            if (!newOrder.sameContents(allDecks)) {
                throw BadRequestException("Reordering list must contain every deck ID exactly once")
            }
            deckDao.reorderDecks(dsl, accountId, newOrder)
        }
    }

    suspend fun updateDeck(token: ValidatedToken, deckId: UUID, request: DeckRequest) {
        val accountId = accountService.ensureAccount(token)
        database.transaction { dsl ->
            deckDao.updateDeck(dsl, accountId, deckId, request.name, request.sources)
        }
    }
}

private fun <T> List<T>.sameContents(b: List<T>): Boolean = all { it in b } && b.all { it in this }