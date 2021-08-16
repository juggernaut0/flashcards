package source.editor

import asynclite.async
import components.Modal
import components.TagInput
import flashcards.api.v1.CardSourceRequest
import source.editor.SourceEditor.CardSource
import flashcards.api.v1.Card
import flashcards.api.v1.CardGroup
import flashcards.api.v1.CustomCardSourceRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kui.*

class CustomSourceEditor(source: CardSource.CustomCardSource, private val makeDirty: () -> Unit) : SourceEditor.Contents() {
    private var showBacks by renderOnSet(false)
    private val groups = source.groups.mapTo(mutableListOf()) {
        CardGroupView(
            cards = it.cards.mapTo(mutableListOf()) { c ->
                Card(front = c.front, back = c.back, prompt = c.prompt, synonyms = c.synonyms, notes = c.notes)
            },
            iid = it.iid
        )
    }
    private var iidSeq = source.groups.maxOfOrNull { it.iid }?.let { it + 1 } ?: 0

    private fun nextIid(): Int {
        val iid = iidSeq
        iidSeq += 1
        return iid
    }

    override fun toRequest(): CardSourceRequest {
        return CustomCardSourceRequest(groups = groups.map { it.toGroup() })
    }

    private fun addGroup() {
        groups.add(CardGroupView(iid = nextIid()))
        makeDirty()
        render()
    }

    private fun import() {
        async {
            val contents = object : Component() {
                var text = ""

                override fun render() {
                    markup().textarea(classes("form-input"), model = ::text)
                }
            }
            val ok = Modal.suspendShow("Import Cards", contents)
            if (ok) {
                val impGroups = prettyPrintJson.decodeFromString(ListSerializer(ExportCardGroup.serializer()), contents.text)
                for (group in impGroups) {
                    groups.add(CardGroupView(group.cards.toMutableList(), nextIid()))
                }
                makeDirty()
                render()
            }
        }
    }

    private fun export() {
        val expGroups = groups.map { ExportCardGroup(it.toGroup().cards) }
        val contents = prettyPrintJson.encodeToString(ListSerializer(ExportCardGroup.serializer()), expGroups)
        Modal.show("Export Cards", componentOf { it.pre { +contents } })
    }

    override fun render() {
        markup().div(classes("row")) {
            h3 { +"Custom Card Source" }
            label {
                checkbox(model = ::showBacks)
                +"Show backs"
            }
            div(classes("gapped-row")) {
                button(Props(classes = listOf("button-confirm"), click = ::import)) { +"Import" }
                button(Props(classes = listOf("button-confirm"), click = ::export)) { +"Export" }
            }
            for (group in groups) {
                component(group)
            }
            button(Props(classes = listOf("button-add-group"), click = ::addGroup)) { +"Add card group" }
        }
    }

    private inner class CardGroupView(private val cards: MutableList<Card> = mutableListOf(), private val iid: Int) : Component() {
        fun toGroup(): CardGroup {
            return CardGroup(cards = cards, iid = iid)
        }

        private fun addCard() {
            val contents = CardModalContents()
            Modal.show("Add Card", contents) { ok ->
                if (ok) {
                    cards.add(contents.toCard())
                    makeDirty()
                    render()
                }
            }
        }

        private fun removeCard(i: Int) {
            cards.removeAt(i)
            makeDirty()
            if (cards.isEmpty()) {
                groups.remove(this)
                this@CustomSourceEditor.render()
            } else {
                render()
            }
        }

        private fun editCard(i: Int, card: Card) {
            val contents = CardModalContents(card)
            Modal.show("Edit Card", contents) { ok ->
                if (ok) {
                    cards[i] = contents.toCard()
                    makeDirty()
                    render()
                }
            }
        }

        override fun render() {
            val backClasses = "blur".takeUnless { showBacks }
            markup().div(classes("card-group")) {
                for ((i, card) in cards.withIndex()) {
                    div(classes("card-group-card")) {
                        div(classes("card-info")) {
                            span { +card.front }
                            span(Props(classes = listOfNotNull(backClasses))) { +card.back }
                        }
                        button(Props(classes = listOf("card-button", "card-button-edit"), click = { editCard(i, card) })) { +EDIT }
                        button(Props(classes = listOf("card-button", "card-button-del"), click = { removeCard(i) })) { +X }
                    }
                }
                button(Props(classes = listOf("button-add-card"), click = ::addCard)) { +"Add card" }
            }
        }
    }

    private class CardModalContents(card: Card? = null) : Component() {
        var front = card?.front ?: ""
        var back = card?.back ?: ""
        var prompt = card?.prompt ?: ""
        val synonyms = card?.synonyms.orEmpty().toMutableList()
        var notes = card?.notes ?: ""

        override fun render() {
            markup().div {
                label {
                    +"Front"
                    inputText(classes("form-input"), model = ::front)
                }
                label {
                    +"Back"
                    inputText(classes("form-input"), model = ::back)
                }
                label {
                    +"Prompt"
                    inputText(classes("form-input"), model = ::prompt)
                }
                label {
                    +"Synonyms"
                    component(TagInput(synonyms))
                }
                label {
                    +"Notes"
                    textarea(classes("form-input"), model = ::notes)
                }
            }
        }

        fun toCard(): Card {
            return Card(
                front = front,
                back = back,
                prompt = prompt.takeIf { it.isNotBlank() },
                synonyms = synonyms.takeIf { it.isNotEmpty() },
                notes = notes.takeIf { it.isNotBlank() },
            )
        }
    }

    @Serializable
    private class ExportCardGroup(val cards: List<Card>)

    companion object {
        private const val X = "\u00d7"
        private const val EDIT = "\u270E"
        private val prettyPrintJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
    }
}
