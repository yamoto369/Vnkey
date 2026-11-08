package app.vikey.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.vikey.KeyboardStatus
import app.vikey.R
import kotlinx.coroutines.launch
import kotlin.math.sin

/** Three steps: what Vikey does, enable the keyboard in Settings, try it. Port of iOS/App/OnboardingView.swift. */
@Composable
fun OnboardingScreen(done: () -> Unit) {
    val ctx = LocalContext.current
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var added by remember { mutableStateOf(KeyboardStatus.isAdded(ctx)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) added = KeyboardStatus.isAdded(ctx) }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    Box(Modifier.fillMaxSize()) {
        Glow()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> Intro()
                    1 -> AddKeyboard(added)
                    else -> TryIt()
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)) {
                for (i in 0 until 3) Box(Modifier.padding(4.dp).size(7.dp).clip(CircleShape).background(if (i == pager.currentPage) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)))
            }
            val label = when (pager.currentPage) { 0 -> R.string.onb_start; 1 -> if (added) R.string.onb_continue else R.string.onb_later; else -> R.string.onb_done }
            Box(Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(16.dp), spotColor = BrandLight.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(BrandLight, Color(0xFF3D4DCC))))
                .clickable { if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else done() }
                .padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(label), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }
}


@Composable private fun Intro() {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Image(painterResource(R.drawable.logo), null, Modifier.size(84.dp).shadow(14.dp, RoundedCornerShape(20.dp), spotColor = BrandLight.copy(alpha = 0.3f)).clip(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(22.dp))
        FitTitle(stringResource(R.string.onb_title1), TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.onb_sub1), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
        DemoCard()
        Spacer(Modifier.weight(2f))
    }
}

@Composable private fun AddKeyboard(added: Boolean) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        Spacer(Modifier.weight(1f))
        FitTitle(stringResource(R.string.onb_title2))
        Spacer(Modifier.height(22.dp))
        Step(1, stringResource(R.string.onb_step1)); Step(2, stringResource(R.string.onb_step2)); Step(3, stringResource(R.string.onb_step3))
        Spacer(Modifier.height(22.dp))
        OutlinedButton({ KeyboardStatus.openSettings(ctx) }) { Icon(Icons.Outlined.Settings, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.open_settings), fontWeight = FontWeight.SemiBold) }
        if (added) { Spacer(Modifier.height(16.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF34C759)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.added), color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold) } }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.onb_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(2f))
    }
}

@Composable private fun Step(n: Int, text: String) {
    Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(BrandLight), contentAlignment = Alignment.Center) { Text("$n", color = Color.White, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.width(14.dp))
        Text(text, fontSize = 20.sp)
    }
}

@Composable private fun TryIt() {
    var sample by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        Spacer(Modifier.weight(1f))
        FitTitle(stringResource(R.string.onb_title3))
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.onb_sub3), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(sample, { sample = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.onb_type_here)) }, minLines = 3, maxLines = 6, shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        Spacer(Modifier.weight(2f))
    }
}

/** A large title (iOS .largeTitle bold, 34) that never wraps a written line: each line of the text keeps to one line,
 *  the size steps down until it does (Tony: no weird line breaks). */
@Composable private fun FitTitle(text: String, align: TextAlign = TextAlign.Start) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val lines = text.count { it == '\n' } + 1
        val maxW = constraints.maxWidth
        val base = androidx.compose.material3.LocalTextStyle.current   // measure exactly what Text draws (theme letter spacing too)
        fun style(sz: Float) = base.merge(androidx.compose.ui.text.TextStyle(fontSize = sz.sp, lineHeight = (sz * 1.2f).sp, fontWeight = FontWeight.Bold, textAlign = align))
        val size = remember(text, maxW, base) {
            var sz = 34f
            while (sz > 22f && measurer.measure(text, style(sz), constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxW)).lineCount > lines) sz -= 1f
            sz
        }
        Text(text, Modifier.fillMaxWidth(), style = style(size))
    }
}

/** Slow breathing brand glow behind the onboarding. */
@Composable fun Glow() {
    val t by rememberInfiniteTransition(label = "glow").animateFloat(0f, 6.2831f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val s = sin(t)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.offset((120 + s * 30).dp, (-260 + s * 20).dp).align(Alignment.Center).size(420.dp).blur(90.dp, BlurredEdgeTreatment.Unbounded).background(BrandLight.copy(alpha = 0.22f), CircleShape))
        Box(Modifier.offset((-140 - s * 25).dp, 280.dp).align(Alignment.Center).size(360.dp).blur(90.dp, BlurredEdgeTreatment.Unbounded).background(Color(0xFF5C8CFF).copy(alpha = 0.16f), CircleShape))
    }
}
