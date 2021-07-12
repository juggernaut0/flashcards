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
                div(classes("buttons")) {
                    for (deck in decks) {
                        deckTile(deck)
                    }
                    button(Props(
                        classes = listOf("dash-tile", "dash-tile-add"),
                        click = ::createDeck
                    )) { +"+" }
                }
            }
            h2 { +"Card sources" }
            div(classes("buttons")) {
                for (source in sources) {
                    sourceTile(source)
                }
                button(Props(
                    classes = listOf("dash-tile", "dash-tile-add"),
                    click = { FlashcardsApp.pushSourceCreation() }
                )) { +"+" }
            }
        }
    }

    private fun MarkupBuilder.deckTile(deck: DashboardDeck) {
        button(Props(classes = listOf("dash-tile"), click = { FlashcardsApp.pushDeckOverview(deck.id) })) {
            div(classes("dash-tile-content")) {
                div(classes("dash-tile-title")) { +deck.name }
                div(classes("dash-tile-deck-indicators")) {
                    span(classes("indicator", "indicator-lessons")) { +"${deck.lessons}" }
                    span(classes("indicator", "indicator-reviews")) { +"${deck.reviews}" }
                }
            }
        }
    }

    private fun MarkupBuilder.sourceTile(source: DashboardQuery.CardSource) {
        button(Props(classes = listOf("dash-tile"), click = {
            FlashcardsApp.pushSourceEditor(source.id)
        })) {
            div(classes("dash-tile-content")) {
                div(classes("dash-tile-title")) { +source.name }
                div {
                    when (source.__typename) {
                        "CustomCardSource" -> +"Custom"
                        "WanikaniCardSource" -> +"WaniKani"
                    }
                }
            }
        }
    }
}
