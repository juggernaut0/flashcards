package review

import review.FuzzyMatchResult
import review.fuzzyMatch
import kotlin.test.Test
import kotlin.test.assertEquals

class FuzzyMatchTest {
    @Test
    fun rejectsWrongAnswer() {
        val result = fuzzyMatch("hello", "goodbye", emptyList(), emptyList())
        assertEquals(FuzzyMatchResult.REJECT, result)
    }

    @Test
    fun allowsCorrectAnswer() {
        val result = fuzzyMatch("hello", "hello", emptyList(), emptyList())
        assertEquals(FuzzyMatchResult.ALLOW, result)
    }

    @Test
    fun allowWithTypoCloseAnswer() {
        val result = fuzzyMatch("hell", "hello", emptyList(), emptyList())
        assertEquals(FuzzyMatchResult.ALLOW_WITH_TYPO, result)
    }

    @Test
    fun closeKanaAnswer() {
        val result = fuzzyMatch("ごんにちは", "こんにちは", emptyList(), emptyList())
        assertEquals(FuzzyMatchResult.CLOSE, result)
    }

    @Test
    fun kanaExpected() {
        val result = fuzzyMatch("hello", "こんにちは", emptyList(), emptyList())
        assertEquals(FuzzyMatchResult.KANA_EXPECTED, result)
    }

    @Test
    fun rejectBlockList() {
        val result = fuzzyMatch("hell", "hello", listOf("hell"), emptyList())
        assertEquals(FuzzyMatchResult.REJECT, result)
    }

    @Test
    fun closeCloseList() {
        val result = fuzzyMatch("konnichiwa", "hello", emptyList(), listOf("konnichiwa"))
        assertEquals(FuzzyMatchResult.CLOSE, result)
    }
}
