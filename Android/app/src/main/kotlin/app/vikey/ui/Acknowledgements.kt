package app.vikey.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vikey.R

class Credit(val name: String, val by: String, val license: String, val url: String)

/** Credits for the code and the data Vikey is built from (same list as ACKNOWLEDGEMENTS.md and App/Model/Acknowledgements.swift). */
object Acknowledgements {
    val code = listOf(Credit("OpenKey", "Tuyen Mai", "GPL-3.0", "https://github.com/tuyenvm/OpenKey"))
    val wordData = listOf(
        Credit("FrequencyWords (OpenSubtitles 2018)", "Hermit Dave", "CC BY-SA 4.0", "https://github.com/hermitdave/FrequencyWords"),
        Credit("FineWeb-2 and FineWeb", "Hugging Face", "ODC-By 1.0", "https://huggingface.co/datasets/HuggingFaceFW/fineweb-2"),
        Credit("HPLT v2", "High Performance Language Technologies", "CC0", "https://hplt-project.org"),
        Credit("MADLAD-400", "Google", "ODC-By 1.0", "https://huggingface.co/datasets/allenai/MADLAD-400"),
        Credit("Reddit comments", "tensorshield/reddit_dataset_157", "MIT", "https://huggingface.co/datasets/tensorshield/reddit_dataset_157"),
        Credit("Vietnamese forums and reader comments", "VnExpress, Tuổi Trẻ, Tinhte, Otofun, Dạy Nhau Học", "word statistics", "https://vikey.app/acknowledgements"),
        Credit("Emoji names and keywords (CLDR)", "Unicode, Inc.", "Unicode License v3", "https://cldr.unicode.org"),
        Credit("Emojibase", "Miles Johnson", "MIT", "https://emojibase.dev"),
    )
    val testData = listOf(
        Credit("ViLexNorm", "Nguyen et al., EACL 2024", "CC BY-NC-SA 4.0", "https://github.com/ngxtnhi/ViLexNorm"),
        Credit("UIT-ViCTSD", "UIT NLP Group", "research use", "https://huggingface.co/datasets/tarudesu/ViCTSD"),
        Credit("VietMix", "EACL 2026", "research use", "https://huggingface.co/datasets/razent/vietmix"),
        Credit("Tap typing with touch sensing images", "Google Research", "CC BY 4.0", "https://github.com/google-research-datasets/tap-typing-with-touch-sensing-images"),
        Credit("How We Type: Mobile", "Aalto University", "research use", "https://userinterfaces.aalto.fi/how-we-type-mobile"),
        Credit("GitHub Typo Corpus", "Hagiwara & Mita, LREC 2020", "research use", "https://github.com/mhagiwara/github-typo-corpus"),
        Credit("Birkbeck spelling error corpus", "Roger Mitton", "free to use", "https://www.dcs.bbk.ac.uk/~roger/corpora.html"),
        Credit("Lists of common misspellings", "Wikipedia", "CC BY-SA 4.0", "https://en.wikipedia.org/wiki/Wikipedia:Lists_of_common_misspellings"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgementsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    androidx.activity.compose.BackHandler(onBack = back)   // system Back returns to Settings, like the iOS navigation back
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        TopAppBar(title = { Text(stringResource(R.string.acknowledgements)) }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            section(R.string.ack_code, Acknowledgements.code, null) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            section(R.string.ack_words, Acknowledgements.wordData, R.string.ack_words_note) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            section(R.string.ack_test, Acknowledgements.testData, R.string.ack_test_note) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: Int, items: List<Credit>, note: Int?, open: (String) -> Unit) {
    item { SectionTitle(stringResource(title)) }
    item {
        Card {
            for ((i, c) in items.withIndex()) {
                Row(Modifier.fillMaxWidth().clickable { open(c.url) }.padding(16.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(c.name); Text("${c.by} · ${c.license}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
                if (i < items.size - 1) HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
        note?.let { Footer(stringResource(it)) }
        Spacer(Modifier.height(16.dp))
    }
}
