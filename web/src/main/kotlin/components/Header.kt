package components

import kui.Component
import kui.Props
import kui.classes

class Header : Component() {
    override fun render() {
        markup().div(classes("app-header")) {
            button(Props(
                classes = listOf("link-button"),
                click = { FlashcardsApp.pushDashboard() },
            )) { +"\u276E Return to dashboard" }
        }
    }
}
