package components

import flashcards.api.v1.CardSourceRequest
import kui.Component
import kui.Props
import kui.classes
import kui.renderOnSet
import components.SourceEditor.CardSource
import flashcards.api.v1.Card
import flashcards.api.v1.CustomCardSourceRequest

class CustomSourceEditor(source: CardSource.CustomCardSource, private val makeDirty: () -> Unit) : SourceEditor.Contents() {
    private var showBacks by renderOnSet(false)
    private val groups = source.groups.mapTo(mutableListOf()) {
        CardGroupView(
            cards = it.cards.mapTo(mutableListOf()) { c ->
                Card(front = c.front, back = c.back, prompt = c.prompt, notes = c.notes)
            },
            iid = it.iid
        )
    }
    private var iidSeq = source.groups.maxOfOrNull { it.iid }?.let { it + 1 } ?: 0

    override fun toRequest(): CardSourceRequest {
        return CustomCardSourceRequest(groups = groups.map { it.toGroup() })
    }

    private fun addGroup() {
        groups.add(CardGroupView(iid = iidSeq))
        iidSeq += 1
        makeDirty()
        render()
    }

    override fun render() {
        markup().div(classes("row")) {
            h3 { +"Custom Card Source" }
            label {
                checkbox(model = ::showBacks)
                +"Show backs"
            }
            for (group in groups) {
                component(group)
            }
            button(Props(classes = listOf("button-add-group"), click = ::addGroup)) { +"Add card group" }
        }
    }

    private inner class CardGroupView(private val cards: MutableList<Card> = mutableListOf(), private val iid: Int) : Component() {
        fun toGroup(): flashcards.api.v1.CardGroup {
            return flashcards.api.v1.CardGroup(cards = cards, iid = iid)
        }

        private fun addCard() {
            val contents = CardModalContents()
            Modal.show("Add Card", contents) {
                cards.add(contents.toCard())
                makeDirty()
                render()
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
            Modal.show("Edit Card", contents) {
                cards[i] = contents.toCard()
                makeDirty()
                render()
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
                synonyms = synonyms,
                notes = notes.takeIf { it.isNotBlank() },
            )
        }
    }

    companion object {
        private const val X = "\u00d7"
        private const val EDIT = "\u270E"
    }
}
