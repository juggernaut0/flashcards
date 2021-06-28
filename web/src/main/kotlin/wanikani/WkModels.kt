package wanikani

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WkCollection<T>(val pages: Pages, val data: List<WkObject<T>>)

@Serializable
class Pages(@SerialName("next_url") val nextUrl: String?)

@Serializable
class WkObject<T>(val id: Long, val `object`: String, val data: T)

@Serializable
class Assignment(
    @SerialName("available_at") val availableAt: Instant?,
    val hidden: Boolean,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("subject_type") val subjectType: String,
    @SerialName("srs_stage") val srsStage: Int,
    @SerialName("started_at") val startedAt: Instant?
)

@Serializable
class Subject(val level: Int)

@Serializable
class Review(
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("starting_srs_stage") val startingSrsStage: Int,
    @SerialName("ending_srs_stage") val endingSrsStage: Int
)
