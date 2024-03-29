package flashcards

import auth.api.v1.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import multiplatform.ktor.handleApi
import java.util.*

private val mockToken = "mockToken"
private val mockUserId = UUID.fromString("3e4097ef-3853-4829-9b56-67791b78a798")

fun Route.mockAuthRoutes() {
    route(validate.path.pathString()) {
        intercept(ApplicationCallPipeline.Call) {
            val token = call.request.header("Authorization")
            if (token != "Bearer $mockToken") {
                call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                finish()
            }
        }
    }
    handleApi(validate) { mockUserId }
    handleApi(lookup) { UserInfo(mockUserId, null) }
    handleApi(register) { AuthenticatedUser(mockUserId, mockToken) }
    handleApi(signIn) { AuthenticatedUser(mockUserId, mockToken) }
    handleApi(getGoogleClientId) { throw NotFoundException() }
}