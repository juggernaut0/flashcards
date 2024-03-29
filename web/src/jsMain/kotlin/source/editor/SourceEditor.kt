@file:UseSerializers(UUIDSerializer::class)

package source.editor

import FlashcardsService
import WanikaniService
import asynclite.async
import asynclite.delay
import components.Header
import flashcards.api.v1.Card
import flashcards.api.v1.CardSourceRequest
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kui.Component
import kui.Props
import kui.classes
import multiplatform.UUID
import multiplatform.UUIDSerializer
import multiplatform.graphql.GraphQLQuery

class SourceEditor(private val service: FlashcardsService, private val wanikaniService: WanikaniService, private val sourceId: UUID) : Component() {
    private var source: CardSource? = null
    private lateinit var inner: Contents
    private var dirty = false
    private var saved = false

    init {
        async {
            val source = service.query(Query(sourceId)).source
            inner = when(source) {
                is CardSource.CustomCardSource -> CustomSourceEditor(service, source, ::makeDirty)
                is CardSource.WanikaniCardSource -> WanikaniSourceEditor(wanikaniService, source)
            }
            this.source = source
            render()
        }
    }

    private fun makeDirty() {
        if (dirty) return
        dirty = true
        render()
    }

    private fun delete() {
        async {
            service.deleteSource(sourceId)
        }
    }

    private fun save() {
        async {
            service.updateSource(sourceId, inner.toRequest())
            saved = true
            dirty = false
            render()
            delay(5000)
            saved = false
            render()
        }
    }

    override fun render() {
        markup().div(classes("container")) {
            val source = source
            if (source == null) {
                p { +"Loading..." }
            } else {
                component(Header())
                div(classes("sticky-top", "solid-row")) {
                    h2 { +source.name }
                    div(classes("gapped-row")) {
                        button(Props(classes = listOf("button-confirm"), click = ::save)) { +"Save" }
                        button(Props(classes = listOf("button-delete"), click = ::delete)) { +"Delete" }
                        val msg = when {
                            dirty -> "You have unsaved changes."
                            saved -> "Changes saved."
                            else -> ""
                        }
                        span { +msg }
                    }
                }
                component(inner)
            }
        }
    }

    abstract class Contents : Component() {
        abstract fun toRequest(): CardSourceRequest
    }

    private class Query(id: UUID) : GraphQLQuery<Response> {
        override val queryString: String = """
            query (${'$'}id: String!) {
                source(id: ${'$'}id) {
                    type: __typename
                    name id
                    ... on CustomCardSource {
                        groups {
                            iid srsStage lastReviewed nextReview
                            cards { front back prompt synonyms blockList closeList notes }
                        }
                    }
                }
            }
        """.trimIndent()

        override val variables = mapOf(
            "id" to id.toString()
        )

        override val responseDeserializer = Response.serializer()
    }

    @Serializable
    private class Response(val source: CardSource)

    @Serializable
    sealed class CardSource {
        @Serializable
        @SerialName("CustomCardSource")
        class CustomCardSource(override val name: String, val id: UUID, val groups: List<CardGroup>) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(override val name: String, val id: UUID) : CardSource()

        abstract val name: String
    }

    @Serializable
    class CardGroup(val cards: List<Card>, val iid: Int, val srsStage: Int, val lastReviewed: Instant?, val nextReview: Instant?)
}
