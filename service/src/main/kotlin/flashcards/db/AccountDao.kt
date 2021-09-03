package flashcards.db

import flashcards.db.jooq.Tables.FLASHCARDS_ACCOUNT
import flashcards.db.jooq.tables.records.FlashcardsAccountRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject

class AccountDao @Inject constructor() {
    suspend fun getAccount(dsl: DSLContext, userId: UUID): FlashcardsAccountRecord? {
        return dsl.selectFrom(FLASHCARDS_ACCOUNT)
            .where(FLASHCARDS_ACCOUNT.USER_ID.eq(userId))
            .asFlow()
            .firstOrNull()
    }

    suspend fun createAccount(dsl: DSLContext, userId: UUID): FlashcardsAccountRecord {
        return dsl.insertInto(FLASHCARDS_ACCOUNT)
            .set(FLASHCARDS_ACCOUNT.ID, UUID.randomUUID())
            .set(FLASHCARDS_ACCOUNT.USER_ID, userId)
            .returningResult(FLASHCARDS_ACCOUNT.asterisk())
            .asFlow()
            .first()
            .into(FLASHCARDS_ACCOUNT) // returning() doesn't work properly in jooq 3.15, thus the asterisk & into
    }
}
