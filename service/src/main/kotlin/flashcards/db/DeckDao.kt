package flashcards.db

import flashcards.db.jooq.Tables.*
import flashcards.db.jooq.tables.records.DeckRecord
import kotlinx.coroutines.future.await
import multiplatform.ktor.BadRequestException
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject

class DeckDao @Inject constructor() {
    suspend fun createDeck(dsl: DSLContext, accountId: UUID, name: String, sources: List<UUID>): UUID {
        val id = UUID.randomUUID()
        dsl.insertInto(DECK)
            .set(DECK.ID, id)
            .set(DECK.OWNER_ID, accountId)
            .set(DECK.NAME, name)
            .executeAsync()
            .await()

        if (sources.isNotEmpty()) {
            setSources(dsl, id, sources)
        }

        return id
    }

    suspend fun getDecks(dsl: DSLContext, accountId: UUID): List<DeckData> {
        return dsl.select(DECK.asterisk(), DECK_CARD_SOURCE.SOURCE_ID)
            .from(DECK.leftJoin(DECK_CARD_SOURCE).onKey())
            .where(DECK.OWNER_ID.eq(accountId))
            .fetchAsync()
            .await()
            .groupBy { it[DECK.ID] }
            .map { (_, records) ->
                val deckRecord = records.first().into(DECK)
                val sources = records.map { it[DECK_CARD_SOURCE.SOURCE_ID] }
                DeckData(deckRecord, sources)
            }
    }

    suspend fun getDeck(dsl: DSLContext, accountId: UUID, deckId: UUID): DeckData? {
        return dsl.select(DECK.asterisk(), DECK_CARD_SOURCE.SOURCE_ID)
            .from(DECK.leftJoin(DECK_CARD_SOURCE).onKey())
            .where(DECK.OWNER_ID.eq(accountId))
            .and(DECK.ID.eq(deckId))
            .fetchAsync()
            .await()
            .takeIf { it.isNotEmpty }
            ?.let { records ->
                val first = records.first()
                val deckRecord = first.into(DECK)
                val sources = if (first[DECK_CARD_SOURCE.SOURCE_ID] != null) { // first sourceId null means no sources
                    records.map { it[DECK_CARD_SOURCE.SOURCE_ID] }
                } else {
                    emptyList()
                }
                DeckData(deckRecord, sources)
            }
    }

    suspend fun updateDeck(dsl: DSLContext, accountId: UUID, deckId: UUID, name: String?, sources: List<UUID>?): Boolean {
        dsl.selectFrom(DECK)
            .where(DECK.OWNER_ID.eq(accountId))
            .and(DECK.ID.eq(deckId))
            .fetchAsync()
            .await()
            .firstOrNull()
            ?: throw BadRequestException("Deck with id [$deckId] not found")

        if (name != null) {
            dsl.update(DECK)
                .set(DECK.NAME, name)
                .where(DECK.OWNER_ID.eq(accountId))
                .and(DECK.ID.eq(deckId))
                .executeAsync()
                .await()
        }

        if (sources != null) {
            // check that all sources listed belong to me
            val allSources = dsl.select(CARD_SOURCE.ID).from(CARD_SOURCE)
                .where(CARD_SOURCE.OWNER_ID.eq(accountId))
                .fetchAsync()
                .await()
                .mapTo(mutableSetOf()) { it.value1() }

            if (sources.any { it !in allSources }) {
                throw BadRequestException("Unknown source id in $sources")
            }

            setSources(dsl, deckId, sources)
        }

        return true
    }

    private suspend fun setSources(dsl: DSLContext, deckId: UUID, sources: List<UUID>) {
        dsl.deleteFrom(DECK_CARD_SOURCE)
            .where(DECK_CARD_SOURCE.DECK_ID.eq(deckId))
            .executeAsync()
            .await()

        dsl.insertInto(DECK_CARD_SOURCE)
            .columns(DECK_CARD_SOURCE.DECK_ID, DECK_CARD_SOURCE.SOURCE_ID)
            .let { sources.fold(it) { sql, sourceId -> sql.values(deckId, sourceId) } }
            .executeAsync()
            .await()
    }
}

data class DeckData(val deckRecord: DeckRecord, val sources: List<UUID>)
