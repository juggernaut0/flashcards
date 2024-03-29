package flashcards

import auth.ktor.token
import flashcards.graphql.GraphQLHandler
import flashcards.graphql.registerRoutes
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.jetty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import multiplatform.ktor.installWebApplicationExceptionHandler
import org.slf4j.event.Level
import javax.inject.Inject
import javax.inject.Named

class FlashcardsApp @Inject constructor(
    private val apiHandler: ApiHandler,
    private val graphQLHandler: GraphQLHandler,
    @Named("authClient") private val authClient: HttpClient,
    private val config: FlashcardsConfig,
) {
    fun start() {
        embeddedServer(Jetty, config.app.port) {
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
                if (config.auth.mock) {
                    mockAuthRoutes()
                }
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
