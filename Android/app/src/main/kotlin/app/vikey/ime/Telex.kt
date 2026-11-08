package app.vikey.ime

/** A small Telex reader for search text ("nguwaj" → "ngựa"); port of App/Model/EmojiSearch.swift Telex. */
object Telex {
    fun convert(s: String) = s.split(" ").joinToString(" ") { word(it) }

    private val tones = mapOf('s' to 1, 'f' to 2, 'r' to 3, 'x' to 4, 'j' to 5)
    private val marks: Map<Char, String> = mapOf(   // base vowel -> the 5 toned forms
        'a' to "áàảãạ", 'ă' to "ắằẳẵặ", 'â' to "ấầẩẫậ", 'e' to "éèẻẽẹ", 'ê' to "ếềểễệ", 'i' to "íìỉĩị",
        'o' to "óòỏõọ", 'ô' to "ốồổỗộ", 'ơ' to "ớờởỡợ", 'u' to "úùủũụ", 'ư' to "ứừửữự", 'y' to "ýỳỷỹỵ")
    private val vowels = setOf('a', 'ă', 'â', 'e', 'ê', 'i', 'o', 'ô', 'ơ', 'u', 'ư', 'y')

    fun word(keys: String): String {
        val out = ArrayList<Char>()
        var tone = 0
        var toneKey: Char? = null
        for (k in keys.lowercase()) {
            val last = out.lastOrNull()
            when {
                k == 'd' && last == 'd' -> out[out.size - 1] = 'đ'
                k == 'a' && last == 'a' -> out[out.size - 1] = 'â'
                k == 'e' && last == 'e' -> out[out.size - 1] = 'ê'
                k == 'o' && last == 'o' -> out[out.size - 1] = 'ô'
                k == 'w' -> {
                    val i = out.indexOfLast { it in "aou" }
                    if (i >= 0 && (out.subList(i + 1, out.size).none { it in vowels } || i == out.size - 1)) {
                        val c = out[i]
                        out[i] = when (c) { 'a' -> 'ă'; 'o' -> 'ơ'; else -> 'ư' }
                        if (c == 'o' && i > 0 && out[i - 1] == 'u') out[i - 1] = 'ư'
                        if (c == 'u' && i + 1 < out.size && out[i + 1] == 'o') out[i + 1] = 'ơ'
                    } else out.add('ư')
                }
                k == 'z' -> { tone = 0; toneKey = null }
                else -> {
                    val t = tones[k]
                    if (t != null && out.any { it in vowels }) {
                        if (toneKey == k) { tone = 0; toneKey = null; out.add(k) } else { tone = t; toneKey = k }
                    } else out.add(k)
                }
            }
        }
        if (tone == 0) return String(out.toCharArray())
        val i = toneIndex(out) ?: return String(out.toCharArray())
        marks[out[i]]?.let { out[i] = it[tone - 1] }
        return String(out.toCharArray())
    }

    private fun toneIndex(w: List<Char>): Int? {
        var start = 0
        if (w.size > 2 && w[0] == 'q' && w[1] == 'u') start = 2
        else if (w.size > 2 && w[0] == 'g' && w[1] == 'i' && w[2] in vowels) start = 2
        val idx = w.indices.filter { it >= start && w[it] in vowels }
        val first = idx.firstOrNull() ?: return null
        val lastV = idx.last()
        val nucleus = ArrayList<Int>()
        var j = first
        while (j in idx) { nucleus.add(j); j++ }
        nucleus.lastOrNull { w[it] in "ăâêôơư" }?.let { return it }
        if (nucleus.size == 1) return first
        val endsInConsonant = lastV < w.size - 1
        if (endsInConsonant || nucleus.size == 3) return nucleus[1]
        val pair = "" + w[nucleus[0]] + w[nucleus[1]]
        return if (pair in listOf("oa", "oe", "uy")) nucleus[1] else nucleus[0]
    }
}
