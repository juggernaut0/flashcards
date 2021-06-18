package flashcards

import auth.token
import flashcards.graphql.GraphQLHandler
import flashcards.graphql.registerRoutes
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.client.HttpClient
import io.ktor.features.CallLogging
import io.ktor.features.StatusPages
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.staticBasePackage
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import multiplatform.ktor.installWebApplicationExceptionHandler
import org.slf4j.event.Level
import javax.inject.Inject
import javax.inject.Named

class FlashcardsApp @Inject constructor(
    private val apiHandler: ApiHandler,
    private val graphQLHandler: GraphQLHandler,
    @Named("authClient") private val authClient: HttpClient,
    private val config: AppConfig
) {
    fun start() {
        embeddedServer(Jetty, config.port) {
            install(CallLogging) {
                level = Level.INFO
            }
            install(StatusPages) {
                installWebApplicationExceptionHandler()
            }
            install(Authentication) {
                token(httpClient = authClient)
            }
            routing {
                registerRoutes(apiHandler)
                registerRoutes(graphQLHandler)
                get("flashcards") { call.respondRedirect("flashcards/", permanent = true) }
                route("flashcards/") {
                    staticBasePackage = "static"
                    resources()
                    defaultResource("index.html")
                }
            }
        }.start(wait = true)
    }
}
