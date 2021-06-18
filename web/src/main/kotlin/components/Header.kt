package components

import FlashcardsService
import kui.Component
import kui.Props
import kui.classes

class Header(private val service: FlashcardsService) : Component() {
    override fun render() {
        markup().div(classes("app-header")) {
            button(Props(
                classes = listOf("link-button"),
                click = { FlashcardsApp.pushState(Dashboard(service)) },
            )) { +"\u276E Return to dashboard" }
        }
    }
}
