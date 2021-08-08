package flashcards.db

import flashcards.db.jooq.Tables.SRS_SYSTEM
import flashcards.db.jooq.tables.records.SrsSystemRecord
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject

class SrsSystemDao @Inject constructor() {
    suspend fun getSrsSystem(dsl: DSLContext, id: UUID): SrsSystemRecord? {
        return dsl.selectFrom(SRS_SYSTEM)
            .where(SRS_SYSTEM.ID.eq(id))
            .asFlow()
            .firstOrNull()
    }
}
