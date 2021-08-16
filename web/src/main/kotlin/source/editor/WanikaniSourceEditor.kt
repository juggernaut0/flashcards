package source.editor

import WanikaniService
import asynclite.async
import source.editor.SourceEditor.CardSource.WanikaniCardSource
import flashcards.api.v1.CardSourceRequest
import flashcards.api.v1.WanikaniCardSourceRequest
import kui.Props
import kui.classes
import kui.renderOnSet

class WanikaniSourceEditor(private val service: WanikaniService, private val source: WanikaniCardSource) : SourceEditor.Contents() {
    private var apiKey: String by renderOnSet(service.getApiKey(source.id) ?: "")

    override fun toRequest(): CardSourceRequest {
        // kinda a hack
        service.saveApiKey(source.id, apiKey)
        return WanikaniCardSourceRequest(source.name)
    }

    private fun isKeyValid(): Boolean {
        return apiKeyRegex.matches(apiKey)
    }

    private fun refresh() {
        async {
            service.forSource(source.id).update(force = true)
        }

    }

    override fun render() {
        markup().div(classes("row")) {
            p {
                +"You can find or generate API keys in your "
                a(Props(attrs = mapOf("target" to "_blank")), href = "https://www.wanikani.com/settings/personal_access_tokens") { +"WaniKani settings" }
                +". Make sure your key has all permissions enabled."
            }
            label {
                +"API Key"
                inputText(classes("form-input"), placeholder = "01234567-abcd-abcd-abcd-0123456789ab", model = ::apiKey)
            }
            if (apiKey.isEmpty()) {
                span(classes("error-alert")) { +"API Key missing: this source will not be used." }
            } else if (!isKeyValid()) {
                span(classes("error-alert")) { +"API Key invalid: double check it has been entered properly." }
            }
            button(Props(classes = listOf("button-confirm"), click = ::refresh)) { +"Refresh WaniKani data" }
        }
    }

    companion object {
        private val apiKeyRegex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}