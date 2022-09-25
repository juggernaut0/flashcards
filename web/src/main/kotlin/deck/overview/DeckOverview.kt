package deck.overview

import asynclite.async
import components.FlashcardsApp
import components.Header
import kotlinx.datetime.*
import kui.Component
import kui.Props
import kui.classes
import multiplatform.UUID

class DeckOverview(private val model: DeckOverviewModel) : Component() {
    private var data: DeckOverviewData? = null
    private var selectedSource: SourceView? = null

    init {
        async {
            updateJunk()
        }
    }

    private suspend fun updateJunk() {
        data = model.getData()
        render()
    }

    private fun updateSources(newSources: List<UUID>) {
        async {
            model.updateSources(newSources)
            updateJunk()
        }
    }

    private fun addSource() {
        val deck = data ?: return
        val selectedSource = selectedSource ?: return
        val newSources = (deck.sources + selectedSource).map { it.id }
        updateSources(newSources)
    }

    private fun removeSource(id: UUID) {
        val deck = data ?: return
        val newSources = deck.sources.filterNot { it.id == id }.map { it.id }
        updateSources(newSources)
    }

    private fun moveSourceUp(i: Int) {
        if (i == 0) return
        val deck = data ?: return
        val newSources = deck.sources.mapTo(mutableListOf()) { it.id }
        val tmp = newSources[i-1]
        newSources[i-1] = newSources[i]
        newSources[i] = tmp
        updateSources(newSources)
    }

    override fun render() {
        markup().div(classes("container")) {
            val deck = data
            if (deck == null) {
                p { +"Loading..." }
            } else {
                component(Header())
                h2 { +deck.name }
                button(Props(
                    classes = listOf("start-lesson-button"),
                    click = { FlashcardsApp.pushLessonScreen(model.deckId) },
                    disabled = deck.lessons == 0,
                )) { +if(deck.lessons == 0) "No lessons available" else "Start ${deck.lessons} lessons" }
                button(Props(
                    classes = listOf("start-review-button"),
                    click = { FlashcardsApp.pushReviewScreen(model.deckId) },
                    disabled = deck.reviews == 0
                )) { +if(deck.reviews == 0) "No reviews available" else "Start ${deck.reviews} reviews" }

                if (deck.reviews > 0) {
                    h4 { +"SRS Breakdown" }

                    div(classes("row")) {
                        +deck.reviewsPerStage
                            .entries
                            .sortedBy { it.key }
                            .joinToString(separator = ", ") { (stage, count) -> "Stage $stage: $count" }
                    }
                }

                h3 { +"Review Forecast" }

                var lastDate: LocalDate? = null
                for (entry in deck.reviewForecast) {
                    val localTime = entry.time.toLocalDateTime(TimeZone.currentSystemDefault())
                    val time = if (localTime.date != lastDate) localTime.dhm() else localTime.hm()
                    lastDate = localTime.date
                    div(classes("review-forecast-row")) {
                        div(classes("review-forecast-time")) { +time }
                        div(classes("review-forecast-count")) { +"+${entry.count}" }
                        div(classes("review-forecast-total")) { +"${entry.total}" }
                    }
                }
                if (deck.reviewForecast.isEmpty()) {
                    p { +"No upcoming reviews." }
                }

                h3 { +"Sources used in this deck" }
                ul {
                    for ((i, source) in deck.sources.withIndex()) {
                        li {
                            button(Props(
                                classes = listOf("source-list-button", "source-list-button-del"),
                                click = { removeSource(source.id) },
                            )) { +"\u00d7" }
                            if (i != 0) {
                                button(
                                    Props(
                                        classes = listOf("source-list-button", "source-list-button-move"),
                                        click = { moveSourceUp(i) },
                                    )
                                ) { +"\u25B4" }
                            } else {
                                span(classes("source-list-button", "source-list-button-blank")) {}
                            }
                            if (i != deck.sources.lastIndex) {
                                button(
                                    Props(
                                        classes = listOf("source-list-button", "source-list-button-move"),
                                        click = { moveSourceUp(i+1) },
                                    )
                                ) { +"\u25BE" }
                            } else {
                                span(classes("source-list-button", "source-list-button-blank")) {}
                            }
                            +source.name
                        }
                    }
                    val unaddedSources = deck.unaddedSources
                    if (unaddedSources.isNotEmpty()) {
                        li {
                            select(classes("deck-add-source-select"), options = unaddedSources, model = ::selectedSource)
                            button(Props(classes = listOf("deck-add-source-button"), click = ::addSource)) { +"Add source" }
                        }
                    }
                }
            }
        }
    }

    private fun Int.pad0(n: Int): String = toString().padStart(n, '0')

    private fun LocalDateTime.dhm(): String {
        val dow = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$dow $dayOfMonth ${hour.pad0(2)}:${minute.pad0(2)}"
    }

    private fun LocalDateTime.hm(): String {
        return "${hour.pad0(2)}:${minute.pad0(2)}"
    }
}
