package components

import FlashcardsService
import WanikaniService
import dashboard.Dashboard
import dashboard.DashboardModel
import deck.overview.DeckOverview
import deck.overview.DeckOverviewModel
import kui.Component
import kui.componentOf
import kotlinx.browser.window
import lesson.LessonModel
import lesson.LessonScreen
import multiplatform.UUID
import review.ReviewModel
import review.ReviewScreen
import review.ReviewSummaryData

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

    private val flashcardsService = FlashcardsService()
    private val wanikaniService = WanikaniService()

    fun pushDashboard() {
        pushState(Dashboard(DashboardModel(flashcardsService, wanikaniService)))
    }

    fun pushDeckOverview(deckId: UUID) {
        pushState(DeckOverview(DeckOverviewModel(flashcardsService, wanikaniService, deckId)))
    }

    fun pushLessonScreen(deckId: UUID) {
        pushState(LessonScreen(LessonModel(flashcardsService, wanikaniService, deckId)))
    }

    fun pushReviewScreen(deckId: UUID) {
        pushState(ReviewScreen(ReviewModel(flashcardsService, wanikaniService, deckId)))
    }

    fun pushReviewSummary(summaryData: ReviewSummaryData) {
        pushState(ReviewSummary(flashcardsService, summaryData))
    }

    fun pushSourceCreation() {
        pushState(SourceCreation(flashcardsService, wanikaniService))
    }

    fun pushSourceEditor(sourceId: UUID) {
        pushState(SourceEditor(flashcardsService, wanikaniService, sourceId))
    }

    private fun pushState(component: Component) {
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
