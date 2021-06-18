package flashcards.inject

import dagger.Component
import flashcards.FlashcardsApp
import javax.inject.Singleton

@Component(modules = [FlashcardsModule::class])
@Singleton
interface FlashcardsInjector {
    fun app(): FlashcardsApp
}
