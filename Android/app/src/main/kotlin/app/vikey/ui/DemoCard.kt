package app.vikey.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vikey.R
import kotlinx.coroutines.delay
import org.json.JSONArray

/** The website's demo: a normal input method vs Vikey, typed key by key (frames from the real engine, Tools: gen_demo). */
@Composable
fun DemoCard() {
    val ctx = LocalContext.current
    var plain by remember { mutableStateOf("") }
    var vikey by remember { mutableStateOf("") }
    var vietnamese by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val scenes = try { JSONArray(ctx.assets.open("demo.json").bufferedReader().readText()) } catch (_: Exception) { return@LaunchedEffect }
        val order = (0 until scenes.length()).shuffled()
        while (true) for (si in order) {
            val scene = scenes.getJSONObject(si)
            plain = ""; vikey = ""; vietnamese = scene.optInt("start") == 1
            delay(500)
            val frames = scene.getJSONArray("frames")
            for (i in 0 until frames.length()) {
                val f = frames.getJSONArray(i); if (f.length() < 4) continue
                vikey = f.optString(1); plain = f.optString(3); vietnamese = f.optInt(2) == 1
                delay(if (f.optString(0) == " ") 160 else 70)
            }
            delay(1800)
        }
    }
    val b = brand
    Column(Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(20.dp), spotColor = BrandLight.copy(alpha = 0.18f)).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.demo_plain).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)   // iOS .caption semibold
            Spacer(Modifier.weight(1f))
            Text(if (vietnamese) "VI" else "EN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(if (vietnamese) b else Color.Gray, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
        }
        Spacer(Modifier.height(12.dp))
        // the normal side: differing words red with a dotted underline (Compose has no dotted underline: drawn here)
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        val plainText = if (plain.isEmpty()) AnnotatedString(" ") else marked(plain, vikey, good = false, b)
        Text(plainText, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout = it },
            modifier = Modifier.drawBehind {
                val l = layout ?: return@drawBehind
                if (l.layoutInput.text.text != plainText.text) return@drawBehind
                val dot = 1.5.dp.toPx(); val step = 3.5.dp.toPx()
                for (r in plainText.spanStyles) {
                    if (r.item.color != DemoRed) continue
                    val start = r.start; val end = minOf(r.end, l.layoutInput.text.length)
                    if (start >= end || l.getLineForOffset(start) != 0) continue
                    val x0 = l.getHorizontalPosition(start, true); val x1 = minOf(l.getHorizontalPosition(end, true), size.width)
                    val y = l.getLineBaseline(0) + 3.dp.toPx()
                    var x = x0 + dot
                    while (x < x1) { drawCircle(DemoRed, dot / 2, Offset(x, y)); x += step }
                }
            })
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(15.dp).clip(RoundedCornerShape(3.5.dp)))
            Spacer(Modifier.width(5.dp)); Wordmark(13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(if (vikey.isEmpty()) AnnotatedString(" ") else marked(vikey, plain, good = true, b), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val DemoRed = Color(0xFFD14242)

/** Words where a normal keyboard and Vikey disagree are marked: red dotted on the normal side, brand-tinted on the Vikey side. */
private fun marked(a: String, b: String, good: Boolean, brand: Color): AnnotatedString {
    val wa = a.split(" "); val wb = b.split(" ")
    val red = DemoRed
    return buildAnnotatedString {
        for ((i, w) in wa.withIndex()) {
            if (i > 0) append(" ")
            if (w.isNotEmpty() && (i >= wb.size || wb[i] != w)) {
                if (good) withStyle(SpanStyle(color = brand, background = brand.copy(alpha = 0.14f))) { append(w) }
                else withStyle(SpanStyle(color = red)) { append(w) }   // + the dotted underline drawn by DemoCard
            } else append(w)
        }
    }
}
