package components

import kui.Component
import kui.Props
import kui.classes
import kotlinx.browser.document
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Modal : Component() {
    private var showing = false
    private var title = ""
    private var body: Component? = null
    private var ok: ((Boolean) -> Unit)? = null
    private var okText: String? = null
    private var danger: Boolean = false

    init {
        kui.mountComponent(document.body!!, Modal)
    }

    fun show(title: String, body: Component, okText: String? = null, danger: Boolean = false, ok: ((Boolean) -> Unit)? = null) {
        if (showing) return
        this.title = title
        this.body = body
        this.ok = ok // TODO allow disabling ok button for validation
        this.okText = okText
        this.danger = danger
        showing = true
        render()
    }

    suspend fun suspendShow(title: String, body: Component, okText: String? = null, danger: Boolean = false): Boolean {
        return suspendCoroutine {
            show(
                title = title,
                body = body,
                okText = okText,
                danger = danger,
                ok = { ok -> it.resume(ok) }
            )
        }
    }

    private fun close(result: Boolean) {
        try {
            ok?.invoke(result)
        } finally {
            showing = false
            ok = null
            render()
        }
    }

    override fun render() {
        val cls = if (showing) listOf("modal-background", "modal-show") else listOf("modal-background")
        markup().div(Props(classes = cls, mousedown = { close(false) })) {
            div(classes("modal-box").copy(mousedown = { /* stop bubbling */ })) {
                h3 { +title }
                body?.also { component(it) }
                div(classes("modal-btns")) {
                    if (ok != null) {
                        button(Props(
                            classes = listOf("modal-btn", if (danger) "modal-btn-danger" else "modal-btn-ok"),
                            click = { close(true) }
                        )) {
                            +(okText ?: "Ok")
                        }
                    }
                    button(Props(
                        classes = listOf("modal-btn", "modal-btn-cancel"),
                        click = { close(false) })) {
                        +"Cancel"
                    }
                }
            }
        }
    }
}
