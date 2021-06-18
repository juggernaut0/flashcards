package flashcards.db

import flashcards.db.jooq.Tables.FLASHCARDS_ACCOUNT
import flashcards.db.jooq.tables.records.FlashcardsAccountRecord
import kotlinx.coroutines.future.await
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject

class AccountDao @Inject constructor() {
    suspend fun getAccount(dsl: DSLContext, userId: UUID): FlashcardsAccountRecord? {
        return dsl.selectFrom(FLASHCARDS_ACCOUNT)
            .where(FLASHCARDS_ACCOUNT.USER_ID.eq(userId))
            .fetchAsync()
            .await()
            .firstOrNull()
    }

    fun createAccount(dsl: DSLContext, userId: UUID): FlashcardsAccountRecord {
        return dsl.insertInto(FLASHCARDS_ACCOUNT)
            .set(FLASHCARDS_ACCOUNT.ID, UUID.randomUUID())
            .set(FLASHCARDS_ACCOUNT.USER_ID, userId)
            .returning()
            .fetchOne()!!
    }
}