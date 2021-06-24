package kana

fun Char.isKana() = code in 0x3040..0x30FF
fun String.isCjk() = any { it.code in 0x4E00..0x9FFF }
fun String.isKana() = any { it.isKana() }

fun romajiToKana(s: String): String {
    // scan left to right converting romaji to kana
    return buildString {
        val working = ArrayDeque<Char>()
        for (c in s) {
            if (c.isKana()) {
                append(working.toCharArray())
                append(c)
                working.clear()
                continue
            }
            working.addLast(c)
            if (working.size > 4) {
                append(working.removeFirst())
            }
            // handle easy ltsu
            if (working.size >= 2 && working[0] == working[1] && working[0] != 'n') {
                append('っ')
                working.removeFirst()
            }
            val workingStr = working.toCharArray().concatToString()
            for (i in workingStr.indices) {
                val pre = workingStr.take(i)
                val post = workingStr.substring(i)
                if (post in mapping) {
                    append(pre)
                    append(mapping[post])
                    working.clear()
                    break
                }
            }
            // handle easy nn
            if (working.size >= 2 && working[0] == 'n' && working[1] !in aiueoy) {
                append('ん')
                working.removeFirst()
            }
        }
        append(working.toCharArray())
    }
}

fun kanaToRomaji(s: String): String {
    return buildString {
        var skip = false
        for (i in s.indices) {
            if (skip) { // already processed
                skip = false
                continue
            }

            if (i != s.lastIndex && s[i] in iSet && s[i+1] in lySet) {
                append(reverseMapping["${s[i]}${s[i+1]}"])
                skip = true
            } else if (i != s.lastIndex && s[i] == 'っ' && s[i+1] !in aiueoSet && s[i+1].toString() in reverseMapping) {
                append(reverseMapping[s[i+1].toString()]!!.substring(0, 1))
            } else if (s[i].toString() in reverseMapping) {
                append(reverseMapping[s[i].toString()])
            } else {
                append(s[i])
            }
        }
    }
}

private val aiueoy = setOf('a', 'i', 'u', 'e', 'o', 'y')
private val iSet = setOf('き', 'ぎ', 'し', 'じ', 'ち', 'に', 'ひ', 'び', 'ぴ', 'み', 'り')
private val lySet = setOf('ゃ', 'ゅ', 'ょ')
private val aiueoSet = setOf('あ', 'い', 'う', 'え', 'お')
private val mapping = mapOf(
    "a" to "あ",
    "i" to "い",
    "u" to "う",
    "e" to "え",
    "o" to "お",
    "ka" to "か",
    "ki" to "き",
    "ku" to "く",
    "ke" to "け",
    "ko" to "こ",
    "kya" to "きゃ",
    "kyu" to "きゅ",
    "kyo" to "きょ",
    "ga" to "が",
    "gi" to "ぎ",
    "gu" to "ぐ",
    "ge" to "げ",
    "go" to "ご",
    "gya" to "ぎゃ",
    "gyu" to "ぎゅ",
    "gyo" to "ぎょ",
    "sa" to "さ",
    "shi" to "し",
    "si" to "し",
    "su" to "す",
    "se" to "せ",
    "so" to "そ",
    "sha" to "しゃ",
    "shu" to "しゅ",
    "sho" to "しょ",
    "za" to "ざ",
    "ji" to "じ",
    "zi" to "じ",
    "zu" to "ず",
    "ze" to "ぜ",
    "zo" to "ぞ",
    "ja" to "じゃ",
    "ju" to "じゅ",
    "jo" to "じょ",
    "ta" to "た",
    "ti" to "ち",
    "chi" to "ち",
    "tu" to "つ",
    "tsu" to "つ",
    "te" to "て",
    "to" to "と",
    "cha" to "ちゃ",
    "chu" to "ちゅ",
    "cho" to "ちょ",
    "da" to "だ",
    "di" to "ぢ",
    "du" to "づ",
    "de" to "で",
    "do" to "ど",
    "na" to "な",
    "ni" to "に",
    "nu" to "ぬ",
    "ne" to "ね",
    "no" to "の",
    "nya" to "にゃ",
    "nyu" to "にゅ",
    "nyo" to "にょ",
    "ha" to "は",
    "hi" to "ひ",
    "hu" to "ふ",
    "fu" to "ふ",
    "he" to "へ",
    "ho" to "ほ",
    "hya" to "ひゃ",
    "hyu" to "ひゅ",
    "hyo" to "ひょ",
    "ba" to "ば",
    "bi" to "び",
    "bu" to "ぶ",
    "be" to "べ",
    "bo" to "ぼ",
    "bya" to "びゃ",
    "byu" to "びゅ",
    "byo" to "びょ",
    "pa" to "ぱ",
    "pi" to "ぴ",
    "pu" to "ぷ",
    "pe" to "ぺ",
    "po" to "ぽ",
    "pya" to "ぴゃ",
    "pyu" to "ぴゅ",
    "pyo" to "ぴょ",
    "ma" to "ま",
    "mi" to "み",
    "mu" to "む",
    "me" to "め",
    "mo" to "も",
    "mya" to "みゃ",
    "myu" to "みゅ",
    "myo" to "みょ",
    "ra" to "ら",
    "ri" to "り",
    "ru" to "る",
    "re" to "れ",
    "ro" to "ろ",
    "rya" to "りゃ",
    "ryu" to "りゅ",
    "ryo" to "りょ",
    "ya" to "や",
    "yu" to "ゆ",
    "yo" to "よ",
    "wa" to "わ",
    "wo" to "を",
    "nn" to "ん",
    "ltsu" to "っ",
)
private val reverseMapping = mapping.entries.associateTo(mutableMapOf()) { (k, v) -> v to k }.also { it.remove("っ") }
