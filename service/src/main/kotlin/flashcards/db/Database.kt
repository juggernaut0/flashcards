package flashcards.db

import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.reactivestreams.Publisher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Database @Inject constructor(private val connectionFactory: ConnectionFactory) {
    suspend fun <T> transaction(block: suspend CoroutineScope.(DSLContext) -> T): T {
        val conn = connectionFactory.create().awaitSingle()
        conn.setAutoCommit(false).await()
        conn.beginTransaction().await()
        val res = try {
            val dsl = DSL.using(conn, SQLDialect.POSTGRES)
            coroutineScope { block(dsl) }
        } catch (e: Exception) {
            conn.rollbackTransaction().await()
            conn.close().await()
            throw e
        }
        conn.commitTransaction().await()
        conn.close().await()
        return res
    }
}

private suspend fun Publisher<Void>.await() {
    asFlow().collect {  }
}
