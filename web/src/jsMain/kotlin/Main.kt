import asynclite.async
import auth.AuthPanel
import auth.AuthorizedClient
import auth.api.v1.LookupParams
import components.FlashcardsApp
import kotlinx.browser.document
import kotlinx.browser.window
import multiplatform.api.FetchClient

fun main() {
    if (auth.isSignedIn()) {
        async {
            val user = runCatching {
                AuthorizedClient(FetchClient()).callApi(auth.api.v1.lookup, LookupParams())
            }.getOrNull()
            if (user == null) {
                auth.signOut()
                window.location.reload()
                return@async
            }
            kui.mountComponent(document.body!!, FlashcardsApp)
        }
    } else {
        AuthPanel.Styles.apply()
        kui.mountComponent(document.body!!, AuthPanel())
    }
}
