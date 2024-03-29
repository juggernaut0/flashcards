package source.editor

import FlashcardsService
import asynclite.async
import components.Collapse
import components.Modal
import components.TagInput
import flashcards.api.v1.CardSourceRequest
import source.editor.SourceEditor.CardSource
import flashcards.api.v1.Card
import flashcards.api.v1.CardGroup
import flashcards.api.v1.CustomCardSourceRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kui.*

class CustomSourceEditor(
    private val flashcardsService: FlashcardsService,
    source: CardSource.CustomCardSource,
    private val makeDirty: () -> Unit,
) : SourceEditor.Contents() {
    private var showBacks by renderOnSet(false)
    private var search: String by renderOnSet("")
    private val sourceId = source.id
    private val groups = source.groups.mapTo(mutableListOf()) { CardGroupView(it) }
    private var iidSeq = source.groups.maxOfOrNull { it.iid }?.let { it + 1 } ?: 0

    private fun nextIid(): Int {
        val iid = iidSeq
        iidSeq += 1
        return iid
    }

    override fun toRequest(): CardSourceRequest {
        return CustomCardSourceRequest(groups = groups.mapNotNull { it.toGroup() })
    }

    private fun addGroup() {
        groups.add(CardGroupView(nextIid()))
        makeDirty()
        render()
    }

    private fun removeGroup(group: CardGroupView) {
        Modal.show(
            title = "Remove Group",
            body = componentOf { it.span { +"Are you sure you want to remove the card group?" } },
            danger = true,
        ) { ok ->
            if (ok) {
                groups.remove(group)
                makeDirty()
                render()
            }
        }
    }

    private fun moveGroupUp(group: CardGroupView) {
        val i = groups.indexOf(group)
        if (i == 0) return
        val tmp = groups[i-1]
        groups[i-1] = groups[i]
        groups[i] = tmp
        makeDirty()
        render()
    }

    private fun moveGroupDown(group: CardGroupView) {
        val i = groups.indexOf(group)
        if (i == groups.lastIndex) return
        val tmp = groups[i+1]
        groups[i+1] = groups[i]
        groups[i] = tmp
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
                    groups.add(CardGroupView(nextIid(), cards = group.cards.toMutableList()))
                }
                makeDirty()
                render()
            }
        }
    }

    private fun export() {
        val expGroups = groups.mapNotNull { it.toGroup() }.map { ExportCardGroup(it.cards) }
        val contents = prettyPrintJson.encodeToString(ListSerializer(ExportCardGroup.serializer()), expGroups)
        Modal.show("Export Cards", componentOf { it.pre { +contents } })
    }

    override fun render() {
        markup().div(classes("row")) {
            h3 { +"Custom Card Source" }
            div(classes("gapped-row")) {
                button(Props(classes = listOf("button-confirm"), click = ::import)) { +"Import" }
                button(Props(classes = listOf("button-confirm"), click = ::export)) { +"Export" }
            }
            div(classes("gapped-row")) {
                label(classes("inline")) {
                    checkbox(model = ::showBacks)
                    +"Show backs"
                }
                inputText(classes("form-input", "inline"), placeholder = "Search for cards", model = ::search)
            }
            for (group in groups) {
                if (group.matchesSearch()) {
                    component(group)
                }
            }
            if (search.isBlank()) {
                button(Props(classes = listOf("button-add-group"), click = ::addGroup)) { +"Add card group" }
            }
        }
    }

    private inner class CardGroupView(
        private val iid: Int,
        private val cards: MutableList<Card> = mutableListOf(),
        private val srsStage: Int = 0,
        private val lastReviewed: Instant? = null,
        private val nextReview: Instant? = null,
    ) : Component() {
        constructor(cardGroup: SourceEditor.CardGroup) : this(
            iid = cardGroup.iid,
            cards = cardGroup.cards.toMutableList(),
            srsStage = cardGroup.srsStage,
            lastReviewed = cardGroup.lastReviewed,
            nextReview = cardGroup.nextReview,
        )

        fun toGroup(): CardGroup? {
            return if (cards.isNotEmpty()) CardGroup(cards = cards, iid = iid) else null
        }

        fun matchesSearch(): Boolean {
            if (search.isBlank()) return true
            val search = search.trim()
            return cards.any { it.front.contains(search, ignoreCase = true) }
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
                removeGroup(this)
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

        private fun showInfo() {
            val contents = CardGroupInfoContents(
                iid = iid,
                srsInfo = SrsInfo(
                    stage = srsStage,
                    lastReviewed = lastReviewed,
                    nextReview = nextReview,
                ),
                reset = { async { flashcardsService.resetReview(sourceId, iid) } }
            )
            Modal.show("Card Group Info", contents)
        }

        override fun render() {
            val backClasses = "blur".takeUnless { showBacks }
            markup().div(classes("card-group")) {
                div(classes("card-group-title")) {
                    button(Props(
                        classes = listOf("card-button", "card-button-edit"),
                        click = { showInfo() },
                    )) { +INFO }
                    button(Props(
                        classes = listOf("card-button", "card-button-edit"),
                        click = { moveGroupUp(this@CardGroupView) },
                    )) { +UP }
                    button(Props(
                        classes = listOf("card-button", "card-button-edit"),
                        click = { moveGroupDown(this@CardGroupView) },
                    )) { +DOWN }
                    button(Props(
                        classes = listOf("card-button", "card-button-del"),
                        click = { removeGroup(this@CardGroupView) },
                    )) { +X }
                }
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
        val blockList = card?.blockList.orEmpty().toMutableList()
        val closeList = card?.closeList.orEmpty().toMutableList()
        var notes = card?.notes ?: ""

        override fun render() {
            markup().div(classes("rows")) {
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
                component(Collapse(classes("row"), showing = false)) {
                    slot(Collapse.Slot.Header(showing = true)) {
                        +"$DOWN More Fields"
                    }
                    slot(Collapse.Slot.Header(showing = false)) {
                        +"$RIGHT More Fields"
                    }
                    slot(Collapse.Slot.Body) {
                        label {
                            +"Synonyms"
                            component(TagInput(synonyms))
                        }
                        label {
                            +"Block List"
                            component(TagInput(blockList))
                        }
                        label {
                            +"Close List"
                            component(TagInput(closeList))
                        }
                        label {
                            +"Notes"
                            textarea(classes("form-input"), model = ::notes)
                        }
                    }
                }
            }
        }

        fun toCard(): Card {
            return Card(
                front = front,
                back = back,
                prompt = prompt.takeIf { it.isNotBlank() },
                synonyms = synonyms.takeIf { it.isNotEmpty() },
                blockList = blockList.takeIf { it.isNotEmpty() },
                closeList = closeList.takeIf { it.isNotEmpty() },
                notes = notes.takeIf { it.isNotBlank() },
            )
        }
    }

    private class CardGroupInfoContents(
        private val iid: Int,
        private var srsInfo: SrsInfo,
        private val reset: () -> Unit
    ) : Component() {
        private var resetting by renderOnSet(false)

        private fun resetConfirm() {
            resetting = if (resetting) {
                reset()
                srsInfo = SrsInfo(0, null, null)
                false
            } else {
                true
            }
        }

        override fun render() {
            markup().div(classes("rows")) {
                div(classes("flex-row")) {
                    div(classes("col")) {
                        h5 { +"IID" }
                        p { +"$iid" }
                    }
                }
                div(classes("flex-row")) {
                    div(classes("col")) {
                        h5 { +"SRS Stage" }
                        p { if (srsInfo.stage != 0) +"${srsInfo.stage}" else +"In Lesson" }
                    }
                    div(classes("col")) {
                        h5 { +"Last Reviewed" }
                        p { +lastReviewed() }
                    }
                    div(classes("col")) {
                        h5 { +"Next Review" }
                        p { +nextReview() }
                    }
                }
                div(classes("flex-row")) {
                    div(classes("col")) {
                        button(Props(
                            classes = listOf("button-delete"),
                            click = { resetConfirm() },
                            disabled = srsInfo.stage == 0,
                        )) { if (!resetting) +"Reset SRS" else +"Are you sure?" }
                    }
                }
            }
        }

        private fun lastReviewed(): String {
            val lastReviewed = srsInfo.lastReviewed ?: return "Never"
            return display(lastReviewed)
        }

        private fun nextReview(): String {
            val nextReview = srsInfo.nextReview ?: return "Never"
            return if (nextReview <= Clock.System.now()) "Now" else display(nextReview)
        }

        private fun display(instant: Instant): String {
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val mon = local.month.name.substring(0, 3).lowercase().replaceFirstChar { it.uppercaseChar() }
            return "$mon ${local.dayOfMonth} ${local.hour.pad0(2)}:${local.minute.pad0(2)}"
        }

        private fun Int.pad0(n: Int): String = toString().padStart(n, '0')
    }

    @Serializable
    private class ExportCardGroup(val cards: List<Card>)

    private class SrsInfo(val stage: Int, val lastReviewed: Instant?, val nextReview: Instant?)

    companion object {
        private const val X = "\u00d7"
        private const val EDIT = "\u270E"
        private const val UP = "\u25B4"
        private const val DOWN = "\u25BE"
        private const val RIGHT = "\u25B8"
        private const val INFO = "\u2139"
        private val prettyPrintJson = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = false }
    }
}
