package dashboard

import asynclite.async
import components.*
import flashcards.api.v1.DeckRequest
import kui.*

class Dashboard(private val model: DashboardModel) : Component() {
    private var data: DashboardData? = null

    init {
        async {
            data = model.getData()
            render()
        }
    }

    private fun createDeck() {
        val contents = object : Component() {
            var name: String = ""

            override fun render() {
                markup().div {
                    label {
                        +"Name"
                        inputText(classes("form-input"), model = ::name)
                    }
                }
            }
        }
        async {
            if(Modal.suspendShow("Create deck", contents)) {
                val deck = model.createDeck(DeckRequest(contents.name, emptyList()))
                FlashcardsApp.pushDeckOverview(deck)
            }
        }
    }

    override fun render() {
        val data = data
        if (data == null) {
            markup().p { +"Loading..." }
            return
        }
        val sources = data.sources
        val decks = data.decks
        markup().div(classes("container")) {
            h2 { +"Decks" }
            if (sources.isEmpty()) {
                span { +"You must add a source before creating a deck." }
            } else {
                component(DndReorderTileList(
                    decks,
                    contentType = "application/x-deck",
                    click = { FlashcardsApp.pushDeckOverview(it.id) },
                    add = { createDeck() },
                    reorder = { async { model.updateDeckOrder(it) } },
                    tileContent = { deck ->
                        div(classes("dash-tile-title")) { +deck.name }
                        div(classes("dash-tile-deck-indicators")) {
                            span(classes("indicator", "indicator-lessons")) { +"${deck.lessons}" }
                            span(classes("indicator", "indicator-reviews")) { +"${deck.reviews}" }
                        }
                    }
                ))
            }
            h2 { +"Card sources" }
            component(DndReorderTileList(
                sources,
                contentType = "application/x-card-source",
                click = { FlashcardsApp.pushSourceEditor(it.id) },
                add = { FlashcardsApp.pushSourceCreation() },
                reorder = { async { model.updateSourceOrder(it) } },
                tileContent = { source ->
                    div(classes("dash-tile-title")) { +source.name }
                    div {
                        if (source.error) {
                            span(classes("error-alert")) { +"⚠" }
                        }
                        +source.type
                    }
                }
            ))
        }
    }
}
