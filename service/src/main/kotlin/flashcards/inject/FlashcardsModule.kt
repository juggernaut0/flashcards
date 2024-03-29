package flashcards.inject

import dagger.Module
import dagger.Provides
import flashcards.AppConfig
import flashcards.FlashcardsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.defaultRequest
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import javax.inject.Named
import javax.inject.Singleton

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
    fun config(): FlashcardsConfig = config

    @Provides
    fun connectionFactory(): ConnectionFactory {
        val options = ConnectionFactoryOptions.parse(config.data.r2dbcUrl)
            .mutate()
            .option(ConnectionFactoryOptions.USER, config.data.user)
            .option(ConnectionFactoryOptions.PASSWORD, config.data.password)
            .build()
        return ConnectionFactories.get(options)
    }
}
