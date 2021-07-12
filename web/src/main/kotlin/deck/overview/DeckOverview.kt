package deck.overview

import asynclite.async
import components.FlashcardsApp
import components.Header
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

    private fun addSource() {
        val deck = data ?: return
        val selectedSource = selectedSource ?: return
        val newSources = (deck.sources + selectedSource).map { it.id }
        async {
            model.updateSources(newSources)
            updateJunk()
        }
    }

    private fun removeSource(id: UUID) {
        val deck = data ?: return
        val newSources = deck.sources.filterNot { it.id == id }.map { it.id }
        async {
            model.updateSources(newSources)
            updateJunk()
        }
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
                h3 { +"Sources used in this deck" }
                ul {
                    for (source in deck.sources) {
                        li {
                            button(Props(
                                classes = listOf("card-button", "card-button-del"),
                                click = { removeSource(source.id) },
                            )) { +"\u00d7" }
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
}
