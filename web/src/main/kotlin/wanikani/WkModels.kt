package wanikani

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class WkCollection<T>(val pages: Pages, val data: List<WkObject<T>>)

@Serializable
class Pages(@SerialName("next_url") val nextUrl: String?)

@Serializable
class WkObject<out T>(val id: Long = 0, val `object`: String, val data: T, val resources_updated: ResourcesUpdated? = null)

@Serializable
class ResourcesUpdated(val assignment: WkObject<Assignment>? = null)

@Serializable
class Assignment(
    @SerialName("available_at") val availableAt: Instant?,
    val hidden: Boolean,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("subject_type") val subjectType: String,
    @SerialName("srs_stage") val srsStage: Int,
    @SerialName("started_at") val startedAt: Instant?
)

@Serializable // WARNING you cannot use this serializer to deserialize from WK API (missing type discriminator)
sealed class Subject {
    abstract val level: Int
}

@Serializable
class RadicalSubject(
    val auxiliary_meanings: List<AuxiliaryMeaning> = emptyList(),
    val characters: String?,
    @SerialName("character_images") val characterImages: List<Resource>,
    override val level: Int,
    val meanings: List<SubjectMeaning>,
    @SerialName("meaning_mnemonic") val meaningMnemonic: String,
    val slug: String,
) : Subject()

@Serializable
class KanjiSubject(
    val auxiliary_meanings: List<AuxiliaryMeaning> = emptyList(),
    val characters: String,
    override val level: Int,
    val meanings: List<SubjectMeaning>,
    val meaning_mnemonic: String,
    val meaning_hint: String? = null,
    val readings: List<KanjiReading>,
    val reading_mnemonic: String,
    val reading_hint: String? = null,
) : Subject()

@Serializable
class KanjiReading(
    val reading: String,
    val primary: Boolean,
    val accepted_answer: Boolean,
    val type: String,
)

@Serializable
class VocabularySubject(
    val auxiliary_meanings: List<AuxiliaryMeaning> = emptyList(),
    val characters: String,
    override val level: Int,
    val meanings: List<SubjectMeaning>,
    val meaning_mnemonic: String,
    val meaning_hint: String? = null,
    val pronunciation_audios: List<Resource>,
    val readings: List<VocabReading>,
    val reading_mnemonic: String,
    val reading_hint: String? = null,
) : Subject()

@Serializable
class KanaVocabularySubject(
    val auxiliary_meanings: List<AuxiliaryMeaning> = emptyList(),
    val characters: String,
    override val level: Int,
    val meanings: List<SubjectMeaning>,
    val meaning_mnemonic: String,
    val meaning_hint: String? = null,
) : Subject()

@Serializable
class VocabReading(
    val reading: String,
    val primary: Boolean,
    val accepted_answer: Boolean,
)

@Serializable
class Resource(
    val url: String,
    @SerialName("content_type") val contentType: String,
    val metadata: JsonObject,
)

@Serializable
class SubjectMeaning(val meaning: String, val primary: Boolean)

@Serializable
class AuxiliaryMeaning(val meaning: String, val type: String)

@Serializable
class Review(
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("starting_srs_stage") val startingSrsStage: Int,
    @SerialName("ending_srs_stage") val endingSrsStage: Int
)

@Serializable
class StudyMaterial(
    val meaning_synonyms: List<String>,
    val subject_id: Long,
)

@Serializable
class User(
    val level: Int,
)
