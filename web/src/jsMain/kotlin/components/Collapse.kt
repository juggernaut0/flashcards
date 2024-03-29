package components

import kui.Props
import kui.SlottedComponent
import kui.classes
import kui.renderOnSet

class Collapse(private val props: Props = Props.empty, showing: Boolean = true) : SlottedComponent<Collapse.Slot>() {
    private var showing by renderOnSet(showing)

    private fun showingClasses() = if (showing) classes("collapse-body") else classes("collapse-body-hidden")

    override fun render() {
        markup().div(props) {
            div(Props(
                classes = listOf("collapse-header"),
                click = { showing = !showing }
            )) { slot(Slot.Header(showing)) }
            div(showingClasses()) { slot(Slot.Body) }
        }
    }

    sealed interface Slot {
        data class Header(val showing: Boolean) : Slot
        object Body : Slot
    }
}