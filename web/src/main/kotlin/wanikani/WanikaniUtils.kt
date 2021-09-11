package wanikani

import kana.kanaToRomaji
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import review.Reviewer

fun toCardGroup(assignment: WkObject<Assignment>, subject: WkObject<Subject>, studyMaterial: WkObject<StudyMaterial>?): Reviewer.CardGroup {
    val userSynonyms = studyMaterial?.data?.meaning_synonyms.orEmpty()
    val cards = when (val data = subject.data) {
        is RadicalSubject -> {
            val front = data.characters
                ?: data.characterImages.find {
                    it.contentType == "image/svg+xml" && it.metadata["inline_styles"]?.jsonPrimitive?.booleanOrNull == true
                }?.url
                ?: data.slug
            val (pri, syn) = data.meanings.partition { it.primary }
            val aux = data.auxiliary_meanings.groupBy({ it.type }, { it.meaning })
            listOf(Reviewer.Card(
                front = front,
                back = pri.first().meaning,
                prompt = "Radical Meaning",
                synonyms = syn.map { it.meaning } + userSynonyms + aux["whitelist"].orEmpty(),
                blockList = aux["blacklist"],
                notes = data.meaningMnemonic,
            ))
        }
        is KanjiSubject -> {
            val front = data.characters
            val readings = data.readings.filter { it.accepted_answer }
            val reading = readings.first()
            val readingSyn = readings.subList(1, readings.size)
            val readingPrompt = when (reading.type) {
                "kunyomi" -> "Kanji Kun'yomi"
                "onyomi" -> "Kanji On'yomi"
                "nanori" -> "Kanji Nanori"
                else -> "Kanji Reading"
            }
            val readingNotes = data.reading_mnemonic + data.reading_hint?.let { "\n\n$it" }.orEmpty()
            val (meaning, meaningSyn) = data.meanings.partition { it.primary }
            val aux = data.auxiliary_meanings.groupBy({ it.type }, { it.meaning })
            val meaningNotes = data.meaning_mnemonic + data.meaning_hint?.let { "\n\n$it" }.orEmpty()
            listOf(
                Reviewer.Card(
                    front = front,
                    back = meaning.first().meaning,
                    prompt = "Kanji Meaning",
                    synonyms = meaningSyn.map { it.meaning } + userSynonyms + aux["whitelist"].orEmpty(),
                    blockList = aux["blacklist"],
                    closeList = readings.map { kanaToRomaji(it.reading) },
                    notes = meaningNotes,
                ),
                Reviewer.Card(
                    front = front,
                    back = reading.reading,
                    prompt = readingPrompt,
                    synonyms = readingSyn.map { it.reading },
                    notes = readingNotes,
                ),
            )
        }
        is VocabularySubject -> {
            val front = data.characters
            val readings = data.readings.filter { it.accepted_answer }
            val (reading, readingSyn) = readings.partition { it.primary }
            val readingNotes = data.reading_mnemonic + data.reading_hint?.let { "\n\n$it" }.orEmpty()
            val (meaning, meaningSyn) = data.meanings.partition { it.primary }
            val aux = data.auxiliary_meanings.groupBy({ it.type }, { it.meaning })
            val meaningNotes = data.meaning_mnemonic + data.meaning_hint?.let { "\n\n$it" }.orEmpty()
            val audioUrls = data.pronunciation_audios
                .filter {
                    val voiceActorId = it.metadata["voice_actor_id"]?.jsonPrimitive?.intOrNull
                    val format = it.contentType
                    voiceActorId == 1 && format == "audio/ogg"
                }
                .map {
                    Reviewer.AudioUrl(it.url, it.metadata["pronunciation"]?.jsonPrimitive?.content)
                }
            listOf(
                Reviewer.Card(
                    front = front,
                    back = meaning.first().meaning,
                    prompt = "Vocab Meaning",
                    synonyms = meaningSyn.map { it.meaning } + userSynonyms + aux["whitelist"].orEmpty(),
                    blockList = aux["blacklist"],
                    closeList = readings.map { kanaToRomaji(it.reading) },
                    notes = meaningNotes,
                ),
                Reviewer.Card(
                    front = front,
                    back = reading.first().reading,
                    prompt = "Vocab Reading",
                    synonyms = readingSyn.map { it.reading },
                    notes = readingNotes,
                    audioUrls = audioUrls
                ),
            )
        }
    }
    return Reviewer.CardGroup(cards, iid = assignment.id)
}
