package components

import FlashcardsService
import WanikaniService
import asynclite.async
import flashcards.api.v1.CustomCardSourceRequest
import flashcards.api.v1.WanikaniCardSourceRequest
import kui.Component
import kui.Props
import kui.classes
import kui.renderOnSet

class SourceCreation(private val service: FlashcardsService, private val wanikaniService: WanikaniService) : Component() {
    private var name: String = ""
    private var selectedType: Type by renderOnSet(Type.CUSTOM)
    private var apiKey: String = ""
    private var canCreate by renderOnSet(true)
    private var error: String? = null

    private fun create() {
        error = null
        if (name.isBlank()) {
            error = "name must not be blank."
            render()
            return
        }

        val selectedType = selectedType
        val apiKey = apiKey

        if (selectedType == Type.WANIKANI && apiKey.isBlank()) {
            error = "api key must not be blank."
            render()
            return
        }

        val sourceRequest = when (selectedType) {
            Type.CUSTOM -> CustomCardSourceRequest(name)
            Type.WANIKANI -> WanikaniCardSourceRequest(name)
        }

        async {
            canCreate = false
            try {
                val source = service.createSource(sourceRequest)
                if (selectedType == Type.WANIKANI) {
                    wanikaniService.saveApiKey(source, apiKey)
                }
                error = "already created. Return to your dashboard to create another."
                FlashcardsApp.pushSourceEditor(source)
            } catch (e: Throwable) {
                console.error(e)
                error = e.message + ". Try again later?"
                canCreate = true
            }
        }
    }

    override fun render() {
        markup().div(classes("container")) {
            component(Header())
            h2 { +"Add a card source" }
            div(classes("row")) {
                inputText(classes("form-input"), placeholder = "Name", model = ::name)
            }
            div(classes("row")) {
                label { +"Choose source type" }
                div {
                    for (type in Type.values()) {
                        button(Props(
                            classes = listOfNotNull("source-type", ifn(type == selectedType) { "source-type-active" }),
                            click = { selectedType = type }
                        )) { +type.display }
                    }
                }
            }
            if (selectedType == Type.WANIKANI) {
                div(classes("row")) {
                    label { +"API Key (Make sure it has all permissions)" }
                    inputText(classes("form-input"), placeholder = "01234567-abcd-abcd-abcd-0123456789ab", model = ::apiKey)
                }
            }
            if (error != null) {
                div(classes("error-alert", "row")) {
                    +"Cannot create source: $error"
                }
            }
            div(classes("row")) {
                button(Props(classes = listOf("button-confirm"), click = ::create, disabled = !canCreate)) {
                    +"Create"
                }
            }
        }
    }

    private inline fun <T> ifn(cond: Boolean, then: () -> T): T? {
        return if (cond) then() else null
    }

    enum class Type(val display: String) {
        CUSTOM("Custom"), WANIKANI("WaniKani")
    }
}