package components

import kana.romajiToKana
import kui.Component
import kui.Props
import kotlin.reflect.KMutableProperty0

class KanaInput(private val props: Props, private val model: KMutableProperty0<String>) : Component() {
    private var value: String
        get() = model.get()
        set(value) {
            model.set(romajiToKana(value))
            render()
        }

    override fun render() {
        markup().inputText(props, model = ::value)
    }
}
