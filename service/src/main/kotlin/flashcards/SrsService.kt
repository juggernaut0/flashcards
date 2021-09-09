package flashcards

import flashcards.db.Database
import flashcards.db.SrsSystemDao
import flashcards.graphql.CardGroup
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.lang.IllegalStateException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SrsService @Inject constructor(
    private val database: Database,
    private val dao: SrsSystemDao,
) {
    private val defaultSystem = suspendLazy {
        database.transaction { dsl ->
            dao.getSrsSystem(dsl, UUID.fromString("68c9ed88-ce50-11eb-b8bc-0242ac130003"))
                ?: throw IllegalStateException("Cannot find default SRS system")
        }
    }

    private suspend fun getDefaultStages(): List<Int> {
        val srs = defaultSystem.getValue()
        return Json.Default.decodeFromString(ListSerializer(Int.serializer()), srs.stages.data())
    }

    suspend fun isUpForReview(cardGroup: CardGroup, now: Instant = Instant.now()): Boolean {
        val nextReview = availableAt(cardGroup) ?: return false
        return now >= nextReview
    }

    fun adjustStage(currentStage: Int, timesIncorrect: List<Int>): Int {
        val totalIncorrect = timesIncorrect.sum()
        return when {
            totalIncorrect == 0 -> currentStage + 1
            currentStage <= 1 -> currentStage
            currentStage > 1 -> currentStage - 1
            currentStage > 4 -> currentStage - 2
            else -> error("unreachable")
        }
    }

    fun isUpForLesson(cardGroup: CardGroup): Boolean {
        return cardGroup.srsStage == 0
    }

    suspend fun availableAt(cardGroup: CardGroup): Instant? {
        val stages = getDefaultStages()
        if (cardGroup.srsStage == 0) return null
        if (cardGroup.srsStage !in stages.indices) return null
        val lastReviewed = cardGroup.lastReviewed?.toJavaInstant() ?: Instant.MIN
        return (lastReviewed + Duration.ofSeconds(stages[cardGroup.srsStage].toLong()))
            .truncatedTo(ChronoUnit.HOURS)
    }
}
