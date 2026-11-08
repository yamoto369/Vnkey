package app.vikey.ime

import android.content.Context
import java.io.File

/** Debug log of what each key press costs (Vikey app › Gỡ lỗi). No typed text: only timings, counts and key kinds. */
object KBLog {
    private val buffer = ArrayList<String>()
    private val start = System.nanoTime()
    private var file: File? = null
    fun init(c: Context) { file = File(c.filesDir, "keyboard-log.txt") }
    fun add(line: String) {
        val t = String.format(java.util.Locale.ROOT, "%8.3f", (System.nanoTime() - start) / 1e9)
        synchronized(buffer) { buffer.add("$t $line"); if (buffer.size >= 20) flush() }
    }
    fun flush() {
        val f = file ?: return
        val lines: List<String>
        synchronized(buffer) { if (buffer.isEmpty()) return; lines = ArrayList(buffer); buffer.clear() }
        Thread {
            try {
                f.appendText(lines.joinToString("\n") + "\n")
                if (f.length() > 400_000) {   // keep the last ~200 KB, like iOS
                    val b = f.readBytes(); f.writeBytes(b.copyOfRange(b.size - 200_000, b.size))
                }
            } catch (_: Exception) {}
        }.start()
    }
    fun read(c: Context): String = try { File(c.filesDir, "keyboard-log.txt").readText() } catch (_: Exception) { "" }
    fun clear(c: Context) { File(c.filesDir, "keyboard-log.txt").delete() }
}

/** What one key press costs (diagnostics readout). */
object Diag {
    var on = false
    var touchStart = 0L; var touchUp = 0L
    var touches = 0; var haptics = 0; var edits = 0; var late = 0
    var suggestMs = 0.0; var delayMs = 0.0; var readMs = 0.0
    var lastKind = "letter"
    fun time(ms: Double) = if (ms < 0.1) String.format(java.util.Locale.ROOT, "%.0f ns", ms * 1_000_000) else String.format(java.util.Locale.ROOT, "%.2f ms", ms)   // "1.00 ms" in every locale, like iOS
}
