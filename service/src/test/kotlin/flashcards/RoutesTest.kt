package flashcards

import flashcards.api.v1.ReviewParam
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalSerializationApi
class RoutesTest {
    @Test
    fun paramDe() {
        val map = mapOf("sourceId" to UUID.randomUUID().toString(), "iid" to "0")
        val params = Properties.decodeFromMap(ReviewParam.serializer(), map)
        assertEquals(0, params.iid)
    }
}