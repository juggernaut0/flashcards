package components

import kui.MarkupBuilder
import kui.Props
import kui.classes
import review.Reviewer

fun MarkupBuilder.cardDetails(card: Reviewer.Card) {
    h5 { +"Correct answer: ${card.back}" }
    if (!card.synonyms.isNullOrEmpty()) {
        h5 { +"Synonyms: ${card.synonyms.joinToString()}" }
    }
    if (!card.notes.isNullOrBlank()) {
        notes(card.notes)
    }
}

// assumes properly nested tags
private val tagRegex = Regex("<.+?>")
private fun MarkupBuilder.notes(notes: String) {
    var startIndex = 0
    val tags: MutableList<String> = mutableListOf()
    for (match in tagRegex.findAll(notes)) {
        val matchStart = match.range.first
        val matchEnd = match.range.last + 1
        val value = match.value
        if (matchStart != startIndex) {
            span(tags.fold(Props.empty) { props, tag -> props + tagProps(tag) }) { +notes.substring(startIndex, matchStart) }
        }
        if (value[1] == '/') tags.removeLast() else tags.add(value.substring(1, value.length - 1))
        startIndex = matchEnd
    }
    if (startIndex != notes.length) {
        span { +notes.substring(startIndex) }
    }
}

private fun tagProps(tag: String?): Props {
    return when (tag) {
        "ja" -> Props(attrs = mapOf("lang" to "ja"))
        "radical", "kanji", "vocabulary" -> classes("em")
        else -> Props.empty
    }
}

private operator fun Props.plus(other: Props): Props {
    if (this == Props.empty) return other
    if (other == Props.empty) return this
    return Props(
        classes = this.classes + other.classes,
        attrs = this.attrs + other.attrs,
    )
}
