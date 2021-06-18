package flashcards

import auth.ValidatedToken
import flashcards.db.AccountDao
import flashcards.db.Database
import java.util.*
import javax.inject.Inject

class AccountService @Inject constructor(
    private val accountDao: AccountDao,
    private val database: Database,
) {
    suspend fun ensureAccount(token: ValidatedToken): UUID {
        val userId = token.userId
        return database.transaction { dsl ->
            val account = accountDao.getAccount(dsl, userId) ?: accountDao.createAccount(dsl, userId)
            account.id
        }
    }
}