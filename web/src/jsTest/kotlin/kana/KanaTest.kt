package kana

import kotlin.test.Test
import kotlin.test.assertEquals

class KanaTest {
    @Test
    fun testConvertKana() {
        assertEquals("かきかえられる", romajiToKana("kakikaerareru"))
        assertEquals("つなみ", romajiToKana("tsunami"))
        assertEquals("ちゅうにびょう", romajiToKana("chuunibyou"))
        assertEquals("ぎゃくに", romajiToKana("gyakuni"))
        assertEquals("べっぷ", romajiToKana("beppu"))
        assertEquals("しゅつ", romajiToKana("shutsu"))
        assertEquals("あんき", romajiToKana("annki"))
        assertEquals("らーめん", romajiToKana("ra-menn"))
        assertEquals("えっふぇる", romajiToKana("efferu"))
    }

    @Test
    fun testTrailingRomaji() {
        assertEquals("あn", romajiToKana("an"))
    }

    @Test
    fun testLeadingNonKana() {
        assertEquals("qく", romajiToKana("qku"))
    }

    @Test
    fun testEasyN() {
        assertEquals("あんき", romajiToKana("anki"))
    }

    @Test
    fun testKanaPrefix() {
        assertEquals("あい", romajiToKana("あi"))
    }

    @Test
    fun testConvertReverse() {
        assertEquals("kakikaerareru", kanaToRomaji("かきかえられる"))
        assertEquals("tsunami", kanaToRomaji("つなみ"))
        assertEquals("chuunibyou", kanaToRomaji("ちゅうにびょう"))
        assertEquals("gyakuni", kanaToRomaji("ぎゃくに"))
        assertEquals("beppu", kanaToRomaji("べっぷ"))
        assertEquals("shutsu", kanaToRomaji("しゅつ"))
        assertEquals("annki", kanaToRomaji("あんき"))
        assertEquals("ra-menn", kanaToRomaji("らーめん"))
        assertEquals("jinn", kanaToRomaji("じん"))
        assertEquals("efferu", kanaToRomaji("えっふぇる"))
    }
}
