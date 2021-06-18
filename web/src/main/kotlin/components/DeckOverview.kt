package components

import FlashcardsService
import asynclite.async
import flashcards.api.v1.DeckRequest
import kotlinx.serialization.Serializable
import kui.Component
import kui.Props
import kui.classes
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable

class DeckOverview(private val service: FlashcardsService, private val deckId: UUID) : Component() {
    private var deck: Deck? = null
    private var allSources: List<CardSource> = emptyList()
    private var selectedSource: SourceView? = null

    init {
        async {
            updateJunk()
        }
    }

    private suspend fun updateJunk() {
        val queryResult = service.query(Query.serializer(), "id" to deckId)
        deck = queryResult.deck
        allSources = queryResult.sources
        render()
    }

    private fun addSource() {
        val deck = deck ?: return
        val selectedSource = selectedSource ?: return
        val newSources = deck.sources + selectedSource.cardSource
        this.deck = deck.copy(sources = newSources)
        render()
        async {
            service.updateDeck(deckId, DeckRequest(sources = newSources.map { it.id }))
            updateJunk()
        }
    }

    private fun removeSource(id: UUID) {
        val deck = deck ?: return
        val newSources = deck.sources.filterNot { it.id == id }
        this.deck = deck.copy(sources = newSources)
        render()
        async {
            service.updateDeck(deckId, DeckRequest(sources = newSources.map { it.id }))
            updateJunk()
        }
    }

    override fun render() {
        markup().div(classes("container")) {
            val deck = deck
            if (deck == null) {
                p { +"Loading..." }
            } else {
                component(Header(service))
                h2 { +deck.name }
                button(Props(
                    classes = listOf("start-lesson-button"),
                    disabled = deck.lessons == 0,
                )) { +if(deck.lessons == 0) "No lessons available" else "Start ${deck.lessons} lessons" }
                button(Props(
                    classes = listOf("start-review-button"),
                    click = { FlashcardsApp.pushState(ReviewScreen(service, deckId)) },
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
                    val sourceIds = deck.sources.mapTo(mutableSetOf()) { it.id }
                    val unaddedSources = allSources.filterNot { it.id in sourceIds }.map { SourceView(it) }
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

    private class SourceView(val cardSource: CardSource) {
        override fun toString(): String {
            return cardSource.name
        }
    }

    @Serializable
    @GraphQLVariable("id", "String!")
    private class Query(@GraphQLArgument("id", "\$id") val deck: Deck, val sources: List<CardSource>)
    @Serializable
    private data class Deck(val name: String, val sources: List<CardSource>, val reviews: Int, val lessons: Int)
    @Serializable
    private class CardSource(@Serializable(with = UUIDSerializer::class) val id: UUID, val name: String)
}