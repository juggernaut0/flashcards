package flashcards.db

import flashcards.db.jooq.Tables.CARD_SOURCE
import flashcards.db.jooq.Tables.CUSTOM_CARD_SOURCE_CARDS
import flashcards.db.jooq.tables.records.CardSourceRecord
import flashcards.db.jooq.tables.records.CustomCardSourceCardsRecord
import flashcards.graphql.CardGroup
import kotlinx.coroutines.future.await
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jooq.DSLContext
import org.jooq.JSONB
import java.util.*
import javax.inject.Inject

class CardSourceDao @Inject constructor() {
    suspend fun createSource(dsl: DSLContext, accountId: UUID, name: String, type: String, customCards: List<CardGroup>? = null): UUID {
        val sourceId = UUID.randomUUID()
        dsl.insertInto(CARD_SOURCE)
            .set(CARD_SOURCE.ID, sourceId)
            .set(CARD_SOURCE.OWNER_ID, accountId)
            .set(CARD_SOURCE.NAME, name)
            .set(CARD_SOURCE.TYPE, type)
            .executeAsync()
            .await()

        if (customCards != null) {
            val (version, contents) = CustomCardSerializer.toJson(customCards)
            dsl.insertInto(CUSTOM_CARD_SOURCE_CARDS)
                .set(CUSTOM_CARD_SOURCE_CARDS.ID, UUID.randomUUID())
                .set(CUSTOM_CARD_SOURCE_CARDS.SOURCE_ID, sourceId)
                .set(CUSTOM_CARD_SOURCE_CARDS.VERSION, version)
                .set(CUSTOM_CARD_SOURCE_CARDS.CONTENTS, JSONB.valueOf(contents))
                .executeAsync()
                .await()
        }

        return sourceId
    }

    suspend fun getSources(dsl: DSLContext, accountId: UUID): List<CardSourceData> {
        return dsl.selectFrom(CARD_SOURCE.leftJoin(CUSTOM_CARD_SOURCE_CARDS).onKey())
            .where(CARD_SOURCE.OWNER_ID.eq(accountId))
            .fetchAsync()
            .await()
            .map {
                val cardSourceRecord = it.into(CARD_SOURCE)
                val customCards = it.into(CUSTOM_CARD_SOURCE_CARDS)
                CardSourceData(cardSourceRecord, customCards)
            }
    }

    suspend fun getSource(dsl: DSLContext, accountId: UUID, sourceId: UUID, lock: Boolean = false): CardSourceData? {
        return dsl.selectFrom(CARD_SOURCE.leftJoin(CUSTOM_CARD_SOURCE_CARDS).onKey())
            .where(CARD_SOURCE.OWNER_ID.eq(accountId))
            .and(CARD_SOURCE.ID.eq(sourceId))
            .let { if (lock) it.forUpdate().of(CARD_SOURCE) else it }
            .fetchAsync()
            .await()
            .firstOrNull()
            ?.let {
                val cardSourceRecord = it.into(CARD_SOURCE)
                val customCards = it.into(CUSTOM_CARD_SOURCE_CARDS)
                CardSourceData(cardSourceRecord, customCards)
            }
    }

    suspend fun updateSource(
        dsl: DSLContext,
        accountId: UUID,
        sourceId: UUID,
        name: String? = null,
        customCards: List<CardGroup>? = null,
    ) {
        if (name != null) {
            dsl.update(CARD_SOURCE)
                .set(CARD_SOURCE.NAME, name)
                .where(CARD_SOURCE.OWNER_ID.eq(accountId))
                .and(CARD_SOURCE.ID.eq(sourceId))
                .executeAsync()
                .await()
        }

        if (customCards != null) {
            val (version, contents) = CustomCardSerializer.toJson(customCards)
            dsl.update(CUSTOM_CARD_SOURCE_CARDS)
                .set(CUSTOM_CARD_SOURCE_CARDS.VERSION, version)
                .set(CUSTOM_CARD_SOURCE_CARDS.CONTENTS, JSONB.valueOf(contents))
                .where(CUSTOM_CARD_SOURCE_CARDS.SOURCE_ID.eq(sourceId))
                .executeAsync()
                .await()
        }
    }
}

data class CardSourceData(val cardSource: CardSourceRecord, val customCards: CustomCardSourceCardsRecord?) {
    val groups: List<CardGroup>? get() {
        return customCards?.let { CustomCardSerializer.fromJson(it.version, it.contents.data()) }
    }
}

object CustomCardSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(groups: List<CardGroup>): Pair<Int, String> {
        return 1 to json.encodeToString(ListSerializer(CardGroup.serializer()), groups)
    }

    fun fromJson(version: Int, string: String): List<CardGroup> {
        return json.decodeFromString(ListSerializer(CardGroup.serializer()), string)
    }
}
