@file:UseSerializers(UUIDSerializer::class)

package components

import FlashcardsService
import asynclite.async
import flashcards.api.v1.CardSourceRequest
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

class SourceEditor(private val service: FlashcardsService, private val sourceId: UUID) : Component() {
    private var source: CardSource? = null
    private lateinit var inner: Contents

    init {
        async {
            val source = service.query(Query.serializer(), "id" to sourceId).source
            inner = when(source) {
                is CardSource.CustomCardSource -> CustomSourceEditor(source)
                is CardSource.WanikaniCardSource -> WanikaniSourceEditor(service, source)
            }
            this.source = source
            render()
        }
    }

    private fun delete() {
        async {
            service.deleteSource(sourceId)
        }
    }

    private fun save() {
        async {
            service.updateSource(sourceId, inner.toRequest())
        }
    }

    override fun render() {
        markup().div(classes("container")) {
            val source = source
            if (source == null) {
                p { +"Loading..." }
            } else {
                component(Header(service))
                h2 { +source.name }
                div(classes("row")) {
                    button(Props(classes = listOf("button-confirm"), click = ::save)) { +"Save" }
                    button(Props(classes = listOf("button-delete"), click = ::delete)) { +"Delete" }
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
        class CustomCardSource(override val name: String, val groups: List<CardGroup>) : CardSource()
        @Serializable
        @SerialName("WanikaniCardSource")
        class WanikaniCardSource(override val name: String, val id: UUID) : CardSource()

        abstract val name: String
    }

    @Serializable
    class CardGroup(val cards: List<Card>, val iid: Int)

    @Serializable
    class Card(val front: String, val back: String, val prompt: String?, val notes: String?)
}