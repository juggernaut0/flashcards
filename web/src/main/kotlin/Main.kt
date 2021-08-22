import auth.AuthPanel
import components.FlashcardsApp
import kotlinx.browser.document

fun main() {
    if (auth.isSignedIn()) {
        kui.mountComponent(document.body!!, FlashcardsApp)
        FlashcardsApp.pushDashboard()
    } else {
        AuthPanel.Styles.apply()
        kui.mountComponent(document.body!!, AuthPanel())
    }
}
