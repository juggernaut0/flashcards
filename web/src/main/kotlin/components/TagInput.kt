package components

import kui.*

class TagInput(private val tags: MutableList<String>, private val onChange: (() -> Unit)? = null) : Component() {
    private val inputRef = ElementRef()
    private var newValue = ""

    private fun removeTag(tag: String) {
        tags.remove(tag)
        render()
        onChange?.invoke()
    }

    private fun addTag() {
        if (newValue.isNotBlank()) {
            tags.add(newValue)
            newValue = ""
            render()
            onChange?.invoke()
        }
    }

    private fun onKeyUp(eventArgs: KeyboardEventArgs) {
        if (eventArgs.key == "Enter") {
            addTag()
        } else if(eventArgs.key == "Backspace" && newValue.isEmpty()) {
            tags.removeAt(tags.lastIndex)
            render()
            onChange?.invoke()
        }
    }

    override fun render() {
        markup().div(Props(classes = listOf("taginput"), click = { inputRef.get().focus() })) {
            inputText(Props(
                classes = listOf("taginput-input"),
                ref = inputRef,
                keyup = { onKeyUp(it) },
                blur = { addTag() }
            ), model = ::newValue)

            for (tag in tags) {
                span(classes("taginput-tag")) {
                    +tag
                    button(Props(classes = listOf("taginput-tag-close"), click = { removeTag(tag) })) { +"\u00d7" }
                }
            }
        }
    }
}
