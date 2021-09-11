package wanikani

import kotlinx.serialization.json.Json
import review.Reviewer
import kotlin.test.Test
import kotlin.test.assertEquals

class ToCardGroupTest {
    @Test
    fun toCardGroup() {
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
        val oneRaw = """
{
    "id": 440,
    "object": "kanji",
    "url": "https://api.wanikani.com/v2/subjects/440",
    "data_updated_at": "2021-09-09T18:06:35.892390Z",
    "data": {
        "created_at": "2012-02-27T19:55:19.000000Z",
        "level": 1,
        "slug": "一",
        "hidden_at": null,
        "document_url": "https://www.wanikani.com/kanji/%E4%B8%80",
        "characters": "一",
        "meanings": [
            {
                "meaning": "One",
                "primary": true,
                "accepted_answer": true
            }
        ],
        "auxiliary_meanings": [
            {
                "type": "whitelist",
                "meaning": "1"
            }
        ],
        "readings": [
            {
                "type": "onyomi",
                "primary": true,
                "reading": "いち",
                "accepted_answer": true
            },
            {
                "type": "kunyomi",
                "primary": false,
                "reading": "ひと",
                "accepted_answer": false
            },
            {
                "type": "nanori",
                "primary": false,
                "reading": "かず",
                "accepted_answer": false
            },
            {
                "type": "onyomi",
                "primary": true,
                "reading": "いつ",
                "accepted_answer": true
            }
        ],
        "component_subject_ids": [
            1
        ],
        "amalgamation_subject_ids": [
            2467,
            2468,
            2477,
            2510,
            2544,
            2588,
            2627,
            2660,
            2665,
            2672,
            2679,
            2721,
            2730,
            2751,
            2959,
            3048,
            3256,
            3335,
            3348,
            3349,
            3372,
            3481,
            3527,
            3528,
            3656,
            3663,
            4133,
            4173,
            4258,
            4282,
            4563,
            4615,
            4701,
            4823,
            4906,
            5050,
            5224,
            5237,
            5349,
            5362,
            5838,
            6010,
            6029,
            6150,
            6169,
            6209,
            6210,
            6346,
            6584,
            6614,
            6723,
            6811,
            6851,
            7037,
            7293,
            7305,
            7451,
            7561,
            7617,
            7734,
            7780,
            7927,
            8209,
            8214,
            8414,
            8456,
            8583,
            8709,
            8896,
            8921,
            9103
        ],
        "visually_similar_subject_ids": [],
        "meaning_mnemonic": "Lying on the <radical>ground</radical> is something that looks just like the ground, the number <kanji>One</kanji>. Why is this One lying down? It's been shot by the number two. It's lying there, bleeding out and dying. The number One doesn't have long to live.",
        "meaning_hint": "To remember the meaning of <kanji>One</kanji>, imagine yourself there at the scene of the crime. You grab <kanji>One</kanji> in your arms, trying to prop it up, trying to hear its last words. Instead, it just splatters some blood on your face. \"Who did this to you?\" you ask. The number One points weakly, and you see number Two running off into an alleyway. He's always been jealous of number One and knows he can be number one now that he's taken the real number one out.",
        "reading_mnemonic": "As you're sitting there next to <kanji>One</kanji>, holding him up, you start feeling a weird sensation all over your skin. From the wound comes a fine powder (obviously coming from the special bullet used to kill One) that causes the person it touches to get extremely <reading>itchy</reading> (<ja>いち</ja>).",
        "reading_hint": "Make sure you feel the ridiculously <reading>itchy</reading> sensation covering your body. It climbs from your hands, where you're holding the number <kanji>One</kanji> up, and then goes through your arms, crawls up your neck, goes down your body, and then covers everything. It becomes uncontrollable, and you're scratching everywhere, writhing on the ground. It's so itchy that it's the most painful thing you've ever experienced (you should imagine this vividly, so you remember the reading of this kanji).",
        "lesson_position": 26,
        "spaced_repetition_system_id": 2
    }
}
        """

        val subject = json.decodeFromString(WkObject.serializer(KanjiSubject.serializer()), oneRaw)
        val assignment = WkObject(0, "", Assignment(null, false, subject.id, "kanji", 1, null))

        val cardGroup = toCardGroup(assignment, subject, null)

        val expected = """
{
    "cards": [
        {
            "front": "一",
            "back": "One",
            "prompt": "Kanji Meaning",
            "synonyms": [
                "1"
            ],
            "closeList": [
                "ichi",
                "itsu"
            ],
            "notes": "Lying on the <radical>ground</radical> is something that looks just like the ground, the number <kanji>One</kanji>. Why is this One lying down? It's been shot by the number two. It's lying there, bleeding out and dying. The number One doesn't have long to live.\n\nTo remember the meaning of <kanji>One</kanji>, imagine yourself there at the scene of the crime. You grab <kanji>One</kanji> in your arms, trying to prop it up, trying to hear its last words. Instead, it just splatters some blood on your face. \"Who did this to you?\" you ask. The number One points weakly, and you see number Two running off into an alleyway. He's always been jealous of number One and knows he can be number one now that he's taken the real number one out."
        },
        {
            "front": "一",
            "back": "いち",
            "prompt": "Kanji On'yomi",
            "synonyms": [
                "いつ"
            ],
            "notes": "As you're sitting there next to <kanji>One</kanji>, holding him up, you start feeling a weird sensation all over your skin. From the wound comes a fine powder (obviously coming from the special bullet used to kill One) that causes the person it touches to get extremely <reading>itchy</reading> (<ja>いち</ja>).\n\nMake sure you feel the ridiculously <reading>itchy</reading> sensation covering your body. It climbs from your hands, where you're holding the number <kanji>One</kanji> up, and then goes through your arms, crawls up your neck, goes down your body, and then covers everything. It becomes uncontrollable, and you're scratching everywhere, writhing on the ground. It's so itchy that it's the most painful thing you've ever experienced (you should imagine this vividly, so you remember the reading of this kanji)."
        }
    ],
    "iid": 0
}
        """

        assertEquals(expected.trim(), json.encodeToString(Reviewer.CardGroup.serializer(), cardGroup).trim())
    }
}
