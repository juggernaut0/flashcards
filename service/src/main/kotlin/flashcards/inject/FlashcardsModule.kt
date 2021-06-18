package flashcards.inject

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import flashcards.AppConfig
import flashcards.FlashcardsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import javax.inject.Named
import javax.inject.Singleton
import javax.sql.DataSource

@Module
class FlashcardsModule(private val config: FlashcardsConfig) {
    @Provides
    @Singleton
    @Named("authClient")
    fun authClient(): HttpClient {
        return HttpClient(Apache) {
            defaultRequest {
                url.host = config.auth.host
                config.auth.port?.let { url.port = it }
            }

        }
    }

    @Provides
    fun appConfig(): AppConfig = config.app

    @Provides
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            dataSourceClassName = config.data.dataSourceClassName

            addDataSourceProperty("user", config.data.user)
            addDataSourceProperty("password", config.data.password)
            addDataSourceProperty("url", config.data.jdbcUrl)
        }
        return HikariDataSource(config)
    }
}
