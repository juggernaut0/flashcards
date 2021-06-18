package flashcards.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.jooq.ExecutorProvider
import org.jooq.SQLDialect
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

@Singleton
class Database @Inject constructor(private val dataSource: DataSource) {
    suspend fun <T> transaction(block: suspend CoroutineScope.(DSLContext) -> T): T {
        return try {
            dataSource.connection.use { conn ->
                val dsl = DSL.using(conn, SQLDialect.POSTGRES).also {
                    it.configuration().set(ExecutorProvider { Dispatchers.IO.asExecutor() })
                }
                dsl.transactionResultAsync { config ->
                    runBlocking { block(DSL.using(config)) }
                }.await()
            }
        } catch (dae: DataAccessException) {
            log.warn("Caught: $dae")
            throw (dae.cause ?: dae)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Database::class.java)
    }
}