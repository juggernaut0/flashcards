package flashcards

class FlashcardsConfig(
        val app: AppConfig,
        val auth: AuthConfig,
        val data: DataConfig
)

class AppConfig(val port: Int)

class AuthConfig(val host: String, val port: Int? = null)

class DataConfig(
        val user: String,
        val password: String,
        val jdbcUrl: String,
        val dataSourceClassName: String,
)
