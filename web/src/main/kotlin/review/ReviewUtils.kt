package review

import kana.isKana
import kana.kanaToRomaji

@Suppress("NAME_SHADOWING")
fun fuzzyMatch(given: String, expected: String): FuzzyMatchResult {
    val given = given.trim().lowercase().replace(removeFromAnswerRegex, "")
    val expected = expected.trim().lowercase().replace(removeFromAnswerRegex, "")

    if (given == expected) return FuzzyMatchResult.ALLOW
    if (given.isBlank()) return FuzzyMatchResult.CLOSE
    if (!expected.isKana() && given.length <=2 && expected.length <= 2) return FuzzyMatchResult.REJECT

    if (expected.isKana() && given.any { it in 'a'..'z' || it.isWhitespace() }) return FuzzyMatchResult.KANA_EXPECTED

    val lev = if (expected.isKana()) {
        levenshtein(kanaToRomaji(given), kanaToRomaji(expected))
    } else {
        levenshtein(given, expected)
    }
    return if (expected.isKana()) {
        if (lev <= 1) FuzzyMatchResult.CLOSE
        else FuzzyMatchResult.REJECT
    } else {
        if (lev <= 2 && expected.length > 2 && given.length > 2) FuzzyMatchResult.ALLOW_WITH_TYPO
        else FuzzyMatchResult.REJECT
    }
}

private val removeFromAnswerRegex = Regex("[,.\\-']")

enum class FuzzyMatchResult {
    ALLOW, ALLOW_WITH_TYPO, REJECT, CLOSE, KANA_EXPECTED;

    fun reduce(other: FuzzyMatchResult): FuzzyMatchResult {
        return when {
            this == KANA_EXPECTED || other == KANA_EXPECTED -> KANA_EXPECTED
            this == ALLOW || other == ALLOW -> ALLOW
            this == ALLOW_WITH_TYPO || other == ALLOW_WITH_TYPO -> ALLOW_WITH_TYPO
            this == CLOSE || other == CLOSE -> CLOSE
            this == REJECT || other == REJECT -> REJECT
            else -> throw RuntimeException("unreachable")
        }
    }
}

private fun levenshtein(a: String, b: String): Int {
    var v0 = Array(b.length+1) { it }
    var v1 = Array(b.length+1) { 0 }

    for (i in a.indices) {
        v1[0] = i + 1
        for (j in b.indices) {
            v1[j+1] = minOf(
                v0[j+1] + 1,
                v1[j] + 1,
                v0[j] + if (a[i] == b[j]) 0 else 1
            )
        }
        val t = v0
        v0 = v1
        v1 = t
    }

    return v0.last()
}
