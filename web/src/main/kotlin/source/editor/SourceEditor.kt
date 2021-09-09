@file:UseSerializers(UUIDSerializer::class)

package source.editor

import FlashcardsService
import WanikaniService
import asynclite.async
import asynclite.delay
import components.Header
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
import multiplatform.graphql.GraphQLArgument
import multiplatform.graphql.GraphQLVariable

class SourceEditor(private val service: FlashcardsService, private val wanikaniService: WanikaniService, private val sourceId: UUID) : Component() {
    private var source: CardSource? = null
    private lateinit var inner: Contents
    private var dirty = false
    private var saved = false

    init {
        async {
            val source = service.query(Query.serializer(), "id" to sourceId).source
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

    @Serializable
    @GraphQLVariable("id", "String!")
    private class Query(@GraphQLArgument("id", "\$id") val source: CardSource)

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

    @Serializable
    class Card(val front: String, val back: String, val prompt: String?, val synonyms: List<String>?, val notes: String?)
}
