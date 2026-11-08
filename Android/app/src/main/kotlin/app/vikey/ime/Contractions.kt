package app.vikey.ime

/** English contractions typed without the apostrophe ("dont" → "don't", "Ill" → "I'll"), fixed at the end of the word
 *  like the system keyboard. Only words that are not English words themselves ("were", "well", "its" stay; "lets" only with
 *  a capital), and the caller skips valid Vietnamese ("im"). Same table as iOS/Keyboard/Contractions.swift. */
object Contractions {
    private val table = mapOf(
        "dont" to "don't", "cant" to "can't", "isnt" to "isn't", "arent" to "aren't", "wasnt" to "wasn't", "werent" to "weren't",
        "doesnt" to "doesn't", "didnt" to "didn't", "hasnt" to "hasn't", "havent" to "haven't", "hadnt" to "hadn't",
        "couldnt" to "couldn't", "wouldnt" to "wouldn't", "shouldnt" to "shouldn't", "mustnt" to "mustn't", "neednt" to "needn't",
        "aint" to "ain't", "wouldve" to "would've", "couldve" to "could've", "shouldve" to "should've", "mightve" to "might've",
        "im" to "I'm", "ive" to "I've", "youre" to "you're", "youve" to "you've", "youll" to "you'll", "youd" to "you'd",
        "theyre" to "they're", "theyve" to "they've", "theyll" to "they'll", "theyd" to "they'd",
        "weve" to "we've", "itll" to "it'll", "thatll" to "that'll", "shes" to "she's", "thats" to "that's", "whats" to "what's",
        "wheres" to "where's", "theres" to "there's", "heres" to "here's", "hows" to "how's", "whos" to "who's", "whove" to "who've",
        "lets" to "let's", "yall" to "y'all", "i" to "I", "wont" to "won't",
    )
    /** Real English words that only become contractions with a capital I ("Ill" → "I'll", "ill" stays).
     *  "Lets" (the start of a sentence) is "Let's" like the system keyboard; "she lets" stays (Tony). */
    private val capitalOnly = mapOf("ill" to "I'll", "id" to "I'd", "lets" to "Let's")
    private val notFixed = setOf("lets")

    /** The fixed word, or null. `typed` is the word as it shows; keeps its capitals (Dont → Don't, DONT → DON'T). */
    fun fix(typed: String): String? {
        val low = typed.lowercase()
        var out: String
        val c = capitalOnly[low]
        if (c != null) {
            if (typed.firstOrNull()?.isUpperCase() != true) return null
            out = c
        } else {
            val t = table[low]
            if (t == null || low in notFixed) return null
            out = t
        }
        if (typed.length > 1 && typed == typed.uppercase()) return out.uppercase()
        if (typed.firstOrNull()?.isUpperCase() == true) out = out.substring(0, 1).uppercase() + out.substring(1)
        return if (out == typed) null else out
    }
}
