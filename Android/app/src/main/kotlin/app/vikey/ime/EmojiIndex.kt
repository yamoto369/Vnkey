package app.vikey.ime

import android.content.Context
import android.graphics.Paint
import org.json.JSONArray
import java.text.Normalizer

/** Emoji data + search (English, Vietnamese, and the raw Telex keys); port of App/Model/EmojiSearch.swift. */
class Emoji(val char: String, val group: Int, val version: Double, val nameEN: String, val nameVI: String,
            val skins: List<String>, val popularity: Int, val terms: List<Term>) {
    fun withSkin(tone: Int) = if (tone > 0 && skins.size == 5) skins[tone - 1] else char
    override fun equals(other: Any?) = other is Emoji && other.char == char
    override fun hashCode() = char.hashCode()
}

class Term(text: String, val weight: Int) {   // weight: name 3, keyword 2
    val folded = EmojiIndex.fold(text)
    val words = folded.split(" ").filter { it.isNotEmpty() }
    val exactWords = text.lowercase().split(" ").filter { it.isNotEmpty() }.toSet()
}

class EmojiIndex private constructor(val all: List<Emoji>) {
    companion object {
        @Volatile private var shared: EmojiIndex? = null
        fun get(c: Context): EmojiIndex = shared ?: synchronized(this) { shared ?: load(c).also { shared = it } }

        private fun load(c: Context): EmojiIndex {
            val rows = try { JSONArray(c.assets.open("Emoji.json").bufferedReader().readText()) } catch (e: Exception) { return EmojiIndex(emptyList()) }
            val paint = Paint()
            val out = ArrayList<Emoji>(rows.length())
            for (i in 0 until rows.length()) {
                val r = rows.optJSONArray(i) ?: continue
                if (r.length() < 8) continue
                val ch = r.optString(0); if (ch.isEmpty() || !paint.hasGlyph(ch)) continue   // only what this Android can draw
                val en = r.optString(3); val vi = r.optString(4)
                val terms = ArrayList<Term>()
                for (s in listOf(en, vi)) if (s.isNotEmpty()) terms.add(Term(s, 3))
                for (k in (r.optString(5) + " | " + r.optString(6)).split(" | ")) if (k.isNotEmpty()) terms.add(Term(k, 2))
                // emoticons (":D", "=))", "^_^") rank like names: typing one exactly puts its emoji first (Tony)
                if (r.length() > 9) for (t in r.optString(9).split(" ")) if (t.isNotEmpty()) terms.add(Term(t, 3))
                val skins = ArrayList<String>(); r.optJSONArray(7)?.let { a -> for (j in 0 until a.length()) skins.add(a.optString(j)) }
                out.add(Emoji(ch, r.optInt(1), r.optDouble(2, 0.0), en, vi, skins, if (r.length() > 8) r.optInt(8) else 0, terms))
            }
            return EmojiIndex(out)
        }

        /** Lowercase, no accents, đ → d. */
        fun fold(s: String): String = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace('đ', 'd')
    }

    /** Best matches first. `raw` is the keys as typed; the Telex reading of the same keys is searched too. */
    fun search(raw: String, limit: Int = 200, among: List<Emoji>? = null): List<Emoji> {
        val q = raw.lowercase().trim()
        if (q.isEmpty()) return emptyList()
        val variants = arrayListOf(q)
        val t = Telex.convert(q); if (t != q) variants.add(t)
        val queries = variants.map { v -> v.split(" ").filter { it.isNotEmpty() }.map { it to fold(it) } }
        val scored = ArrayList<Triple<Int, Int, Emoji>>()
        for ((i, e) in (among ?: all).withIndex()) {
            var best = 0
            for (words in queries) best = maxOf(best, score(words, e.terms))
            if (best > 0) scored.add(Triple(best * 2 + e.popularity, i, e))
        }
        scored.sortWith(compareByDescending<Triple<Int, Int, Emoji>> { it.first }.thenBy { it.second })
        return scored.take(limit).map { it.third }
    }

    private fun score(words: List<Pair<String, String>>, terms: List<Term>): Int {
        var total = 0
        val whole = words.joinToString(" ") { it.second }
        for ((exact, folded) in words) {
            var best = 0
            val accented = exact != folded
            for (t in terms) {
                val name = t.weight == 3
                var s = 0
                if (t.folded == whole) s = if (name) 100 else 65
                else for (tw in t.words) if (tw.startsWith(folded)) {
                    s = maxOf(s, if (tw.length == folded.length) (if (name) 70 else 55) else (if (name) 45 else 30))
                    if (tw.length == folded.length) break
                }
                if (s <= 0) continue
                if (exact in t.exactWords) s += 10 else if (accented) s -= 40
                if (name) s += maxOf(0, 6 - t.words.size)
                best = maxOf(best, s)
            }
            if (best <= 0) return 0
            total += best
        }
        return total / words.size
    }
}

/** Frequently used emoji (port of EmojiUsage in App/Model/EmojiSearch.swift): each pick adds 1 and every older score
 *  fades a little, so a favorite stays on top but this week's emoji still climbs. Stored as "char\tscore" lines. */
object EmojiUsage {
    const val KEY = "EmojiUsage"
    /** Until enough picks exist, the block is filled with the emoji people use most. */
    val starter = listOf("😂", "❤️", "🥰", "😍", "😭", "👍", "🙏", "😊", "🔥", "😘", "🤣", "🥺", "😅", "🎉", "💕", "😁", "🙂", "😉",
                         "🤔", "👏", "😎", "💪", "✨", "😢", "🙌", "💯", "😆", "🤗", "😴", "👌")

    private fun load(p: android.content.SharedPreferences): HashMap<String, Double> {
        val m = HashMap<String, Double>()
        for (l in (p.getString(KEY, "") ?: "").split("\n")) { val i = l.lastIndexOf('\t'); if (i > 0) l.substring(i + 1).toDoubleOrNull()?.let { m[l.substring(0, i)] = it } }
        return m
    }

    fun record(char: String, p: android.content.SharedPreferences) {
        val s = load(p)
        for (k in s.keys) s[k] = s[k]!! * 0.97
        s[char] = (s[char] ?: 0.0) + 1
        if (s.size > 80) for ((k, _) in s.entries.sortedBy { it.value }.take(s.size - 80)) s.remove(k)
        p.edit().putString(KEY, s.entries.joinToString("\n") { "${it.key}\t${it.value}" }).apply()
    }

    fun frequent(p: android.content.SharedPreferences, n: Int): List<String> {
        val s = load(p)
        val list = ArrayList(s.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key }).map { it.key })
        for (e in starter) if (list.size < n && e !in list) list.add(e)
        return list.take(n)
    }
}
