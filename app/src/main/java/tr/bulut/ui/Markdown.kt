package tr.bulut.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownMetin(metin: String, renk: Color, modifier: Modifier = Modifier) {
    val bloklar = remember(metin) { MarkdownBloklari.ayir(metin) }
    val kodArkaplan = MaterialTheme.colorScheme.surfaceContainerHighest
    val stil = MaterialTheme.typography.bodyLarge.copy(color = renk)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bloklar.forEach { blok ->
            when (blok) {
                is Blok.Paragraf -> Text(satirIci(blok.metin, kodArkaplan), style = stil)
                is Blok.Baslik -> Text(
                    satirIci(blok.metin, kodArkaplan),
                    style = when (blok.duzey) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }.copy(color = renk, fontWeight = FontWeight.Bold),
                )
                is Blok.Madde -> Row(Modifier.padding(start = (blok.girinti * 16).dp)) {
                    Text(blok.isaret, style = stil, modifier = Modifier.width(24.dp))
                    Text(satirIci(blok.metin, kodArkaplan), style = stil)
                }
                is Blok.Alinti -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        satirIci(blok.metin, kodArkaplan),
                        style = stil.copy(fontStyle = FontStyle.Italic),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                is Blok.Kod -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(kodArkaplan)
                        .padding(10.dp)
                ) {
                    if (blok.dil.isNotEmpty()) {
                        Text(
                            blok.dil,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        blok.metin,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                }
                Blok.Cizgi -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

private val satirIciDesen = Regex("""(`[^`\n]+`)|(\*\*[^*\n]+\*\*)|(__[^_\n]+__)|(\*[^*\s][^*\n]*\*)|(~~[^~\n]+~~)""")

/** `kod`, **kalın**, *eğik*, ~~üstü çizili~~. İç içe biçim bilinçli olarak yok. */
private fun satirIci(metin: String, kodArkaplan: Color): AnnotatedString = buildAnnotatedString {
    var konum = 0
    satirIciDesen.findAll(metin).forEach { m ->
        append(metin.substring(konum, m.range.first))
        val v = m.value
        when {
            v.startsWith("`") -> pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = kodArkaplan))
                .also { append(v.substring(1, v.length - 1)) }
            v.startsWith("**") || v.startsWith("__") -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                .also { append(v.substring(2, v.length - 2)) }
            v.startsWith("~~") -> pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                .also { append(v.substring(2, v.length - 2)) }
            else -> pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                .also { append(v.substring(1, v.length - 1)) }
        }
        pop()
        konum = m.range.last + 1
    }
    append(metin.substring(konum))
}
