package flashcards

import com.typesafe.config.ConfigFactory
import flashcards.inject.DaggerFlashcardsInjector
import flashcards.inject.FlashcardsModule
import io.github.config4k.extract

fun main() {
    val config: FlashcardsConfig = ConfigFactory.load().extract()
    runMigrations(DataSourceConfig(config.data.jdbcUrl, config.data.user, config.data.password))
    DaggerFlashcardsInjector.builder()
            .flashcardsModule(FlashcardsModule(config))
            .build()
            .app()
            .start()
}
