@file:UseSerializers(UUIDSerializer::class)

package components

import FlashcardsService
import asynclite.async
import flashcards.api.v1.DeckRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kui.*
import multiplatform.UUID
import multiplatform.UUIDSerializer

class Dashboard(private val service: FlashcardsService) : Component() {
    private var data by renderOnSet(DashboardQuery(listOf(), listOf()))

    init {
        async {
            data = service.query(DashboardQuery.serializer())
        }
    }

    private fun createDeck() {
        val contents = object : Component() {
            var name: String = ""

            override fun render() {
                markup().div {
                    label {
                        +"Name"
                        inputText(model = ::name)
                    }
                }
            }
        }
        Modal.show("Create deck", contents) {
            async {
                val deck = service.createDeck(DeckRequest(contents.name, emptyList()))
                FlashcardsApp.pushState(DeckOverview(service, deck))
            }
        }
    }

    override fun render() {
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
                    click = { FlashcardsApp.pushState(SourceCreation(service)) }
                )) { +"+" }
            }
        }
    }

    private fun MarkupBuilder.deckTile(deck: Deck) {
        button(Props(classes = listOf("dash-tile"), click = { FlashcardsApp.pushState(DeckOverview(service, deck.id)) })) {
            div(classes("dash-tile-content")) {
                div(classes("dash-tile-title")) { +deck.name }
                div(classes("dash-tile-deck-indicators")) {
                    span(classes("indicator", "indicator-lessons")) { +"${deck.lessons}" }
                    span(classes("indicator", "indicator-reviews")) { +"${deck.reviews}" }
                }
            }
        }
    }

    private fun MarkupBuilder.sourceTile(source: CardSource) {
        button(Props(classes = listOf("dash-tile"), click = { FlashcardsApp.pushState(SourceEditor(service, source.id)) })) {
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

    @Serializable
    private class DashboardQuery(val decks: List<Deck>, val sources: List<CardSource>)
    @Serializable
    private class Deck(val id: UUID, val name: String, val lessons: Int, val reviews: Int)
    @Serializable
    private class CardSource(val id: UUID, val name: String, val __typename: String)
}
