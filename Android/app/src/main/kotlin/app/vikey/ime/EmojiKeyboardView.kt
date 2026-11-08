package app.vikey.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import app.vikey.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The emoji keyboard, laid out like the iOS one: a search field, the category name, a grid that scrolls
 * sideways, and a bar with ABC, the categories and delete. Long-press an emoji for skin tones.
 * In search mode only the field and one row of results show; the IME types into the query.
 * Port of iOS/Keyboard/EmojiKeyboardView.swift.
 */
class EmojiKeyboardView(ctx: Context) : View(ctx) {
    interface Listener {
        fun emojiInsert(text: String)
        fun emojiDelete()
        fun emojiClose()
        fun emojiStartSearch()
    }
    var listener: Listener? = null
    var haptics = false
    var dark = false
        set(v) { if (field != v) { field = v; invalidate() } }
    var searching = false; private set
    var feedback: KeyHaptics? = null

    companion object { const val SEARCH_HEIGHT = 92f; private const val MARGIN = 8f }

    private class Category(val title: String, val icon: Int, var items: List<Emoji>)
    private var categories: List<Category> = emptyList()
    private var results: List<Emoji> = emptyList()
    private var queryShown = ""
    private val d = resources.displayMetrics.density
    private val vietnameseUI = android.content.res.Resources.getSystem().configuration.locales[0].language == "vi"   // the phone's language, like the iOS keyboard
    private val prefs get() = Settings.prefs(context)
    private val tone get() = prefs.getInt(Settings.EMOJI_TONE, 0)

    // grid geometry (px)
    private class Column(val section: Int, val items: List<Emoji>, val x: Float)
    private var columns: List<Column> = emptyList()
    private var rows = 3; private var cellW = 0f; private var cellH = 0f; private var glyphPad = 0f
    private val gridRect = RectF(); private val fieldRect = RectF(); private val barRect = RectF()
    private var contentW = 0f
    private var scrollX = 0f
    private val scroller = OverScroller(ctx)
    private var velocity: VelocityTracker? = null
    private var downX = 0f; private var downY = 0f; private var dragging = false; private var lastX = 0f
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val handler = Handler(Looper.getMainLooper())
    private var longPress: Runnable? = null
    private var deleteRepeat: Runnable? = null
    private var pressedCell: Emoji? = null; private val pressedRect = RectF()
    // skin tone popup
    private var skinFor: Emoji? = null; private var skinItems: List<String> = emptyList(); private var skinIndex = 0; private val skinRect = RectF()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val tmp = RectF()

    private val fg get() = if (dark) Color.WHITE else Color.BLACK
    private val secondary get() = if (dark) Color.argb(153, 255, 255, 255) else Color.argb(153, 60, 60, 67)
    private val brand = Color.rgb(79, 69, 229)

    /** Builds the categories (the index loads in the background when the keyboard opens, so this is quick). */
    fun load() {
        if (categories.isNotEmpty()) { reloadRecents(); return }
        val all = EmojiIndex.get(context).all
        fun g(vararg ids: Int) = all.filter { it.group in ids }
        val t = listOf(
            Triple("Dùng thường xuyên", "Frequently Used", R.drawable.ic_cat_clock) to emptyList<Emoji>(),
            Triple("Mặt cười & người", "Smileys & People", R.drawable.ic_cat_face) to g(0, 1),
            Triple("Động vật & thiên nhiên", "Animals & Nature", R.drawable.ic_cat_paw) to g(3),
            Triple("Đồ ăn & thức uống", "Food & Drink", R.drawable.ic_cat_food) to g(4),
            Triple("Hoạt động", "Activity", R.drawable.ic_cat_ball) to g(6),
            Triple("Du lịch & địa điểm", "Travel & Places", R.drawable.ic_cat_car) to g(5),
            Triple("Đồ vật", "Objects", R.drawable.ic_cat_bulb) to g(7),
            Triple("Biểu tượng", "Symbols", R.drawable.ic_cat_heart) to g(8),
            Triple("Cờ", "Flags", R.drawable.ic_cat_flag) to g(9),
        )
        categories = t.map { (h, items) -> Category(if (vietnameseUI) h.first else h.second, h.third, items) }
        reloadRecents()
        layoutGrid()
    }

    private fun reloadRecents() {
        if (categories.isEmpty()) return
        val byChar = EmojiIndex.get(context).all.associateBy { it.char }
        categories[0].items = EmojiUsage.frequent(prefs, 24).mapNotNull { byChar[it] }
        layoutGrid()
    }

    fun setSearching(on: Boolean) {
        searching = on
        results = emptyList()
        setQuery("")
        scrollX = 0f; scroller.forceFinished(true)
        requestLayout(); layoutGrid()
    }

    /** The query as shown (already read as Telex in Vietnamese mode) and its results. */
    fun setQuery(display: String, r: List<Emoji> = emptyList()) {
        queryShown = display
        if (searching) { results = r; scrollX = 0f; scroller.forceFinished(true) }
        layoutGrid()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { layoutGrid() }

    private fun fontSize(cell: Float) = floor(cell * 0.74f)
    private fun glyphWidth(cell: Float): Float { text.textSize = fontSize(cell); return ceil(text.measureText("😀")) }

    private fun layoutGrid() {
        val W = width.toFloat(); val H = height.toFloat(); val M = MARGIN * d
        if (W <= 0) return
        val fieldH = 36 * d
        fieldRect.set(M, M, W - M, M + fieldH)
        val alignX = M + M
        val cols = ArrayList<Column>()
        if (searching) {
            val cell = 44 * d
            gridRect.set(0f, fieldRect.bottom + 2 * d, W, H)
            rows = 1; cellW = cell; cellH = cell
            glyphPad = (cell - glyphWidth(cell)) / 2
            val lead = max(0f, alignX - glyphPad)
            for ((i, e) in results.withIndex()) cols.add(Column(0, listOf(e), lead + i * cell))
            contentW = lead + results.size * cell + M
        } else {
            val titleBottom = fieldRect.bottom + M + 14 * d
            val gridTop = titleBottom + 4 * d; val barH = 44 * d
            val gh = H - gridTop - barH
            rows = max(3, (gh / (42 * d)).toInt())
            val cell = floor(gh / rows)
            val glyph = glyphWidth(cell)
            val nCols = max(7, ((W - 2 * alignX - glyph) / (cell + 4 * d)).roundToInt() + 1)
            cellW = (W - 2 * alignX - glyph) / (nCols - 1); cellH = cell
            glyphPad = (cellW - glyph) / 2
            gridRect.set(0f, gridTop, W, gridTop + rows * cell)
            barRect.set(0f, H - barH, W, H)
            val lead = max(0f, alignX - glyphPad)
            var x = lead
            for ((s, c) in categories.withIndex()) {
                var i = 0
                while (i < c.items.size) { cols.add(Column(s, c.items.subList(i, min(i + rows, c.items.size)), x)); x += cellW; i += rows }
            }
            contentW = x + lead
        }
        columns = cols
        scrollX = scrollX.coerceIn(0f, maxScroll())
        invalidate()
    }
    private fun maxScroll() = max(0f, contentW - width)

    override fun onDraw(c: Canvas) {
        val W = width.toFloat(); val M = MARGIN * d
        // search field
        fill.color = if (dark) Color.argb(36, 255, 255, 255) else Color.argb(18, 0, 0, 0)
        c.drawRoundRect(fieldRect, 10 * d, 10 * d, fill)
        val ic = ContextCompat.getDrawable(context, R.drawable.ic_search)!!.mutate(); ic.setTint(secondary)
        val isz = (18 * d).toInt(); val ix = (fieldRect.left + M).toInt(); val iy = (fieldRect.centerY() - isz / 2).toInt()
        ic.setBounds(ix, iy, ix + isz, iy + isz); ic.draw(c)
        text.typeface = Typeface.DEFAULT; text.textSize = 17 * d; text.textAlign = Paint.Align.LEFT
        val empty = queryShown.isEmpty()
        text.color = if (empty) secondary else fg
        val shown = if (empty) (if (vietnameseUI) "Tìm emoji" else "Search Emoji") else queryShown
        val tx = fieldRect.left + M + 18 * d + 6 * d
        c.drawText(shown, tx, fieldRect.centerY() - (text.descent() + text.ascent()) / 2, text)
        if (searching) {   // caret
            val tw = if (empty) 0f else text.measureText(shown)
            fill.color = brand
            c.drawRect(if (empty) tx - d else tx + tw + d, fieldRect.centerY() - 10 * d, (if (empty) tx - d else tx + tw + d) + 2 * d, fieldRect.centerY() + 10 * d, fill)
        }
        text.textAlign = Paint.Align.CENTER
        if (!searching && categories.isNotEmpty()) {
            val s = currentSection()
            text.color = secondary; text.typeface = if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.DEFAULT, 600, false) else Typeface.DEFAULT_BOLD; text.textSize = 12 * d; text.textAlign = Paint.Align.LEFT
            c.drawText(categories[s].title.uppercase(), M + M, fieldRect.bottom + M + 11 * d, text)
            text.textAlign = Paint.Align.CENTER
            // bar: ABC, categories, delete
            text.color = fg; text.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); text.textSize = 16 * d
            c.drawText("ABC", M / 2 + 25 * d, barRect.centerY() - (text.descent() + text.ascent()) / 2, text)
            val del = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!.mutate(); del.setTint(fg)
            val dsz = (22 * d).toInt(); val dx = (W - 50 * d + 23 * d).toInt(); val dy = (barRect.centerY() - dsz / 2).toInt()
            del.setBounds(dx - dsz / 2, dy, dx + dsz / 2, dy + dsz); del.draw(c)
            val left = 56 * d; val right = W - 52 * d; val step = (right - left) / max(1, categories.size)
            for ((i, cat) in categories.withIndex()) {
                val dr = ContextCompat.getDrawable(context, cat.icon)!!.mutate(); dr.setTint(if (i == s) fg else secondary)
                val sz = (20 * d).toInt(); val cx = (left + i * step + step / 2).toInt(); val cy = barRect.centerY().toInt()
                dr.setBounds(cx - sz / 2, cy - sz / 2, cx + sz / 2, cy + sz / 2); dr.draw(c)
            }
        }
        // grid
        c.save(); c.clipRect(gridRect)
        text.color = fg; text.typeface = Typeface.DEFAULT; text.textSize = fontSize(cellH)
        val t = tone
        for (col in columns) {
            val x = col.x - scrollX
            if (x + cellW < 0 || x > W) continue
            for ((r, e) in col.items.withIndex()) {
                val y = gridRect.top + r * cellH
                if (pressedCell === e) { fill.color = Color.argb(64, 128, 128, 128); tmp.set(x, y, x + cellW, y + cellH); c.drawRoundRect(tmp, 8 * d, 8 * d, fill) }
                c.drawText(e.withSkin(t), x + cellW / 2, y + cellH / 2 - (text.descent() + text.ascent()) / 2, text)
            }
        }
        c.restore()
        if (skinFor != null) drawSkinPopup(c)
    }

    private fun drawSkinPopup(c: Canvas) {
        fill.color = if (dark) Color.rgb(92, 92, 92) else Color.WHITE
        fill.setShadowLayer(6 * d, 0f, 2 * d, Color.argb(77, 0, 0, 0))
        c.drawRoundRect(skinRect, 12 * d, 12 * d, fill); fill.clearShadowLayer()
        val w = 44 * d
        text.color = fg; text.typeface = Typeface.DEFAULT; text.textSize = 30 * d
        for ((i, s) in skinItems.withIndex()) {
            tmp.set(skinRect.left + 4 * d + i * w, skinRect.top + 4 * d, skinRect.left + 4 * d + (i + 1) * w, skinRect.top + 48 * d)
            if (i == skinIndex) { fill.color = Color.argb(64, 79, 69, 229); tmp.inset(d, d); c.drawRoundRect(tmp, 8 * d, 8 * d, fill); tmp.inset(-d, -d) }
            c.drawText(s, tmp.centerX(), tmp.centerY() - (text.descent() + text.ascent()) / 2, text)
        }
    }

    private fun currentSection(): Int {
        val x = scrollX + 30 * d
        return columns.firstOrNull { it.x + cellW > x }?.section ?: 0
    }

    private fun cellAt(x: Float, y: Float): Pair<Emoji, RectF>? {
        if (!gridRect.contains(x, y)) return null
        val cx = x + scrollX
        val col = columns.firstOrNull { cx >= it.x && cx < it.x + cellW } ?: return null
        val r = ((y - gridRect.top) / cellH).toInt()
        val e = col.items.getOrNull(r) ?: return null
        return e to RectF(col.x - scrollX, gridRect.top + r * cellH, col.x - scrollX + cellW, gridRect.top + (r + 1) * cellH)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                velocity?.recycle(); velocity = VelocityTracker.obtain().also { it.addMovement(e) }
                downX = x; downY = y; lastX = x; dragging = false
                if (fieldRect.contains(x, y)) { if (!searching) listener?.emojiStartSearch(); return true }
                if (!searching && barRect.contains(x, y)) { barDown(x); return true }
                cellAt(x, y)?.let { (em, r) ->
                    pressedCell = em; pressedRect.set(r)
                    if (em.skins.size == 5) {
                        val run = Runnable { pressedCell = null; showSkins(em, r); invalidate() }
                        longPress = run; handler.postDelayed(run, 350)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(e)
                if (skinFor != null) { skinIndex = (((x - skinRect.left - 4 * d) / (44 * d)).toInt()).coerceIn(0, skinItems.size - 1); invalidate(); return true }
                if (!dragging && abs(x - downX) > slop && gridRect.contains(downX, downY)) { dragging = true; cancelLong(); pressedCell = null }
                if (dragging) { scrollX = (scrollX - (x - lastX)).coerceIn(0f, maxScroll()); lastX = x }
            }
            MotionEvent.ACTION_UP -> {
                cancelLong(); stopDeleteRepeat()
                if (skinFor != null) {
                    val em = skinFor!!
                    prefs.edit().putInt(Settings.EMOJI_TONE, skinIndex).apply()
                    choose(if (skinIndex == 0) em.char else em.skins[skinIndex - 1], em)
                    skinFor = null
                } else if (dragging) {
                    velocity?.let { it.computeCurrentVelocity(1000); scroller.fling(scrollX.toInt(), 0, (-it.xVelocity).toInt(), 0, 0, maxScroll().toInt(), 0, 0); postInvalidateOnAnimation() }
                } else pressedCell?.let { choose(it.withSkin(tone), it) }
                pressedCell = null; dragging = false
                velocity?.recycle(); velocity = null
            }
            MotionEvent.ACTION_CANCEL -> { cancelLong(); stopDeleteRepeat(); pressedCell = null; skinFor = null; dragging = false; velocity?.recycle(); velocity = null }
        }
        invalidate()
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) { scrollX = scroller.currX.toFloat().coerceIn(0f, maxScroll()); postInvalidateOnAnimation() }
    }

    private fun cancelLong() { longPress?.let { handler.removeCallbacks(it) }; longPress = null }

    private fun barDown(x: Float) {
        val W = width.toFloat(); val M = MARGIN * d
        if (x < M / 2 + 50 * d) { listener?.emojiClose(); return }
        if (x > W - 50 * d) {   // delete, repeating while held
            listener?.emojiDelete(); if (haptics) feedback?.tick(this)
            val rep = object : Runnable { override fun run() { listener?.emojiDelete(); handler.postDelayed(this, 90) } }
            deleteRepeat = rep; handler.postDelayed(rep, 450)
            return
        }
        val left = 56 * d; val right = W - 52 * d; val step = (right - left) / max(1, categories.size)
        val s = ((x - left) / step).toInt().coerceIn(0, categories.size - 1)
        val col = columns.firstOrNull { it.section == s } ?: return
        val lead = max(0f, MARGIN * 2 * d - glyphPad)
        scrollX = (col.x - lead).coerceIn(0f, maxScroll())
    }
    private fun stopDeleteRepeat() { deleteRepeat?.let { handler.removeCallbacks(it) }; deleteRepeat = null }

    private fun choose(s: String, e: Emoji) {
        if (haptics) feedback?.tick(this) else feedback?.click()
        listener?.emojiInsert(s)
        EmojiUsage.record(e.char, prefs)
    }

    private fun showSkins(e: Emoji, over: RectF) {
        skinFor = e; skinItems = listOf(e.char) + e.skins
        skinIndex = tone.coerceIn(0, skinItems.size - 1)
        val w = 44 * d; val total = w * skinItems.size + 8 * d
        var x = over.centerX() - total / 2
        x = min(max(4 * d, x), width - total - 4 * d)
        val y = max(2 * d, over.top - 54 * d)
        skinRect.set(x, y, x + total, y + 52 * d)
        if (haptics) feedback?.tick(this)
    }
}
