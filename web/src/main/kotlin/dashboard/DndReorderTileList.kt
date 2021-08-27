package dashboard

import kui.*
import org.w3c.dom.DragEvent

class DndReorderTileList<T>(
    private var items: List<T>,
    val contentType: String = "text/plain",
    val click: ((T) -> Unit)? = null,
    val add: (() -> Unit)? = null,
    val reorder: ((List<T>) -> Unit)? = null,
    val tileContent: MarkupBuilder.(T) -> Unit,
) : Component() {
    private var dragon: MutableList<Int?> = items.indices.toMutableList()

    private fun rebuildDragon(entered: Int, dragging: Int) {
        for (i in dragon.indices) {
            dragon[i] = if (i < entered) {
                if (i < dragging) {
                    i
                } else  {
                    i + 1
                }
            } else if (i > entered) {
                if (i > dragging) {
                    i
                } else {
                    i - 1
                }
            } else {
                null
            }
        }
        render()
    }

    private fun drop(entered: Int, dragging: Int) {
        dragon[entered] = dragging
        items = List(items.size) { i -> items[dragon[i]!!] }
        dragon = items.indices.toMutableList()
        render()
        reorder?.invoke(items)
    }

    override fun render() {
        markup().div(classes("buttons")) {
            for ((i, itemIndex) in dragon.withIndex()) {
                if (itemIndex != null) {
                    val item = items[itemIndex]
                    button(
                        Props(
                            classes = listOf("dash-tile"),
                            attrs = mapOf("draggable" to "true"),
                            click = click?.let { { it(item) } },
                            extraEvents = mapOf(
                                "dragstart" to {
                                    it as DragEvent
                                    it.dataTransfer?.setData(contentType, i.toString())
                                    dragon[i] = null
                                },
                                "dragend" to {
                                    dragon = items.indices.toMutableList()
                                    render()
                                },
                                "dragenter" to ev@{
                                    it as DragEvent
                                    val draggedIndex = it.getData() ?: return@ev
                                    it.preventDefault()
                                    rebuildDragon(i, draggedIndex)
                                },
                            )
                        )
                    ) {
                        div(classes("dash-tile-content")) {
                            tileContent(item)
                        }
                    }
                } else {
                    div(Props(
                        classes = listOf("dash-tile", "dash-tile-drop"),
                        extraEvents = mapOf(
                            "dragenter" to { it.preventDefault() },
                            "dragover" to { it.preventDefault() },
                            "drop" to ev@{
                                it as DragEvent
                                val draggedIndex = it.getData() ?: return@ev
                                drop(i, draggedIndex)
                            }
                        )
                    )) { }
                }
            }
            button(
                Props(
                    classes = listOf("dash-tile", "dash-tile-add"),
                    click = add
                )
            ) { +"+" }
        }
    }

    private fun DragEvent.getData(): Int? = dataTransfer?.getData(contentType)?.toIntOrNull()
}
