package components

import kui.Component
import kui.Props
import kui.classes
import kotlinx.browser.document

object Modal : Component() {
    private var showing = false
    private var title = ""
    private var body: Component? = null
    private var ok: (() -> Unit)? = null
    private var okText: String? = null
    private var danger: Boolean = false

    init {
        kui.mountComponent(document.body!!, Modal)
    }

    fun show(title: String, body: Component, okText: String? = null, danger: Boolean = false, ok: () -> Unit) {
        if (showing) return
        this.title = title
        this.body = body
        this.ok = ok // TODO allow disabling ok button for validation
        this.okText = okText
        this.danger = danger
        showing = true
        render()
    }

    fun close() {
        showing = false
        ok = null
        render()
    }

    override fun render() {
        val cls = if (showing) listOf("modal-background", "modal-show") else listOf("modal-background")
        markup().div(Props(classes = cls, click = { close() })) {
            div(classes("modal-box").copy(click = {})) {
                h3 { +title }
                body?.also { component(it) }
                div(classes("modal-btns")) {
                    button(Props(
                        classes = listOf("modal-btn", if (danger) "modal-btn-danger" else "modal-btn-ok"),
                        click = { ok?.invoke(); close() })) {
                        +(okText ?: "Ok")
                    }
                    button(Props(
                        classes = listOf("modal-btn", "modal-btn-cancel"),
                        click = { close() })) {
                        +"Cancel"
                    }
                }
            }
        }
    }
}
