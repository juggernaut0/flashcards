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
import review.ReviewSummary
import review.ReviewSummaryData
import source.editor.SourceEditor

object FlashcardsApp : Component() {
    private val emptyComponent = componentOf { it.div {  } }
    private var current: Component = emptyComponent

    private val flashcardsService = FlashcardsService()
    private val wanikaniService = WanikaniService()

    init {
        current = componentFromRoute(window.location.hash)
        window.onpopstate = { _ ->
            current = componentFromRoute(window.location.hash)
            render()
        }
    }

    fun pushDashboard() {
        pushState("#/")
    }

    fun pushDeckOverview(deckId: UUID) {
        pushState("#/deck/$deckId")
    }

    fun pushLessonScreen(deckId: UUID) {
        pushState("#/deck/$deckId/lesson")
    }

    fun pushReviewScreen(deckId: UUID) {
        pushState("#/deck/$deckId/review")
    }

    fun pushReviewSummary(summaryData: ReviewSummaryData) {
        ReviewSummary.data = summaryData
        pushState("#/reviewSummary")
    }

    fun pushSourceCreation() {
        pushState("#/source/create")
    }

    fun pushSourceEditor(sourceId: UUID) {
        pushState("#/source/$sourceId")
    }

    private fun componentFromRoute(url: String): Component {
        val parts = parseUrlParams(url)
        return when {
            parts.size == 2 && parts[0] == "deck" -> DeckOverview(DeckOverviewModel(flashcardsService, wanikaniService, UUID(parts[1])))
            parts.size == 3 && parts[0] == "deck" && parts[2] == "lesson" -> LessonScreen(LessonModel(flashcardsService, wanikaniService, UUID(parts[1])))
            parts.size == 3 && parts[0] == "deck" && parts[2] == "review" -> ReviewScreen(ReviewModel(flashcardsService, wanikaniService, UUID(parts[1])))
            parts.size == 1 && parts[0] == "reviewSummary" -> ReviewSummary()
            parts.size == 2 && parts[0] == "source" && parts[1] == "create" -> SourceCreation(flashcardsService, wanikaniService)
            parts.size == 2 && parts[0] == "source" -> SourceEditor(flashcardsService, wanikaniService, UUID(parts[1]))
            else -> Dashboard(DashboardModel(flashcardsService, wanikaniService))
        }
    }

    private fun parseUrlParams(fragment: String): List<String> {
        return fragment
            .trimStart('#', '/')
            .split('/')
            .filter { it.isNotEmpty() }
            .map { decodeURIComponent(it) }
    }

    private fun pushState(url: String) {
        window.history.pushState(null, "", url)
        current = componentFromRoute(url)
        render()
    }

    override fun render() {
        markup().component(current)
    }
}

external fun decodeURIComponent(s: String): String
