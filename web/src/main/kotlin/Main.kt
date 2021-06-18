import auth.AuthPanel
import components.Dashboard
import components.FlashcardsApp
import kotlinx.browser.document

fun main() {
    applyStyles()
    if (auth.isSignedIn()) {
        kui.mountComponent(document.body!!, FlashcardsApp)
        val svc = FlashcardsService()
        FlashcardsApp.pushState(Dashboard(svc))
    } else {
        AuthPanel.Styles.apply()
        kui.mountComponent(document.body!!, AuthPanel())
    }
}
