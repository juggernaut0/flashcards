package components

import kui.Component
import kui.componentOf
import kotlinx.browser.window

object FlashcardsApp : Component() {
    private val emptyComponent = componentOf { it.div {  } }
    private val history: MutableList<Component> = mutableListOf()
    private var current: Int = -1

    init {
        window.onpopstate = { evt ->
            current = evt.state as? Int? ?: 0
            render()
        }
    }

    fun pushState(component: Component) {
        val wasEmpty = history.isEmpty()
        if (current == history.lastIndex) {
            history.add(component)
            current++
        } else {
            current++
            history[current] = component
        }
        if (!wasEmpty) {
            window.history.pushState(current, "")
        } else {
            window.history.pushState(null, "")
        }
        render()
    }

    override fun render() {
        markup().component(if (history.isEmpty()) emptyComponent else history[current])
    }
}
