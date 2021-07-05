package wanikani

import review.Reviewer

fun toCardGroup(assignment: WkObject<Assignment>, subject: WkObject<Subject>): Reviewer.CardGroup {
    // TODO for notes, remove or process WK markup
    val cards = when (val data = subject.data) {
        is RadicalSubject -> {
            val front = data.characters
                ?: data.characterImages.find {
                    it.contentType == "image/svg+xml" && it.metadata.inline_styles == true
                }?.url
                ?: data.slug
            val (pri, syn) = data.meanings.partition { it.primary }
            listOf(Reviewer.Card(
                front = front,
                back = pri.first().meaning,
                prompt = "Radical Meaning",
                synonyms = syn.map { it.meaning },
                notes = data.meaningMnemonic
            ))
        }
        is KanjiSubject -> {
            val front = data.characters
            val (reading, readingSyn) = data.readings.filter { it.accepted_answer }.partition { it.primary }
            val readingPrompt = when (reading.first().type) {
                "kunyomi" -> "Kanji Kun'yomi"
                "onyomi" -> "Kanji On'yomi"
                "nanori" -> "Kanji Nanori"
                else -> "Kanji Reading"
            }
            val readingNotes = data.reading_mnemonic + data.reading_hint?.let { "\n\n$it" }.orEmpty()
            val (meaning, meaningSyn) = data.meanings.partition { it.primary }
            val meaningNotes = data.meaning_mnemonic + data.meaning_hint?.let { "\n\n$it" }.orEmpty()
            listOf(
                Reviewer.Card(
                    front = front,
                    back = meaning.first().meaning,
                    prompt = "Kanji Meaning",
                    synonyms = meaningSyn.map { it.meaning },
                    notes = meaningNotes
                ),
                Reviewer.Card(
                    front = front,
                    back = reading.first().reading,
                    prompt = readingPrompt,
                    synonyms = readingSyn.map { it.reading },
                    notes = readingNotes,
                ),
            )
        }
        is VocabularySubject -> {
            val front = data.characters
            val (reading, readingSyn) = data.readings.filter { it.accepted_answer }.partition { it.primary }
            val readingNotes = data.reading_mnemonic + data.reading_hint?.let { "\n\n$it" }.orEmpty()
            val (meaning, meaningSyn) = data.meanings.partition { it.primary }
            val meaningNotes = data.meaning_mnemonic + data.meaning_hint?.let { "\n\n$it" }.orEmpty()
            listOf(
                Reviewer.Card(
                    front = front,
                    back = meaning.first().meaning,
                    prompt = "Vocab Meaning",
                    synonyms = meaningSyn.map { it.meaning },
                    notes = meaningNotes
                ),
                Reviewer.Card(
                    front = front,
                    back = reading.first().reading,
                    prompt = "Vocab Reading",
                    synonyms = readingSyn.map { it.reading },
                    notes = readingNotes,
                ),
            )
        }
    }
    return Reviewer.CardGroup(cards, iid = assignment.id)
}