package tr.bulut.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import tr.bulut.veri.GorselModelleri

private data class Boyut(val ad: String, val g: Int, val y: Int)

private val boyutlar = listOf(
    Boyut("Kare", 1024, 1024),
    Boyut("Dikey", 768, 1344),
    Boyut("Yatay", 1344, 768),
    Boyut("Küçük kare", 512, 512),
    Boyut("Küçük dikey", 512, 768),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyoEkrani(
    vm: BulutViewModel,
    kaynakId: String?,
    geri: () -> Unit,
    galeriAc: () -> Unit,
    gorselAc: (String) -> Unit,
    cloudflareAc: () -> Unit,
) {
    val ayarlar by vm.ayarlar.collectAsStateWithLifecycle()
    val anahtarlilar by vm.anahtarlilar.collectAsStateWithLifecycle()
    val durum by vm.studyo.collectAsStateWithLifecycle()
    val gorseller by vm.gorseller.collectAsStateWithLifecycle()
    val g = ayarlar.gorsel
    val hazir = g.hesapId.isNotBlank() && BulutViewModel.CLOUDFLARE in anahtarlilar
    val kapsam = rememberCoroutineScope()
    val bildirim = remember { SnackbarHostState() }

    val kaynak = kaynakId?.let { vm.gorsel(it) }
    var istem by rememberSaveable { mutableStateOf(kaynak?.istem.orEmpty()) }
    var negatif by rememberSaveable { mutableStateOf(kaynak?.negatif.orEmpty()) }
    var modelId by rememberSaveable { mutableStateOf(kaynak?.model ?: g.model) }
    val model = GorselModelleri.bul(modelId)
    var adim by rememberSaveable(modelId) {
        mutableIntStateOf(
            (kaynak?.adim?.takeIf { kaynak.model == modelId } ?: g.adim.takeIf { g.model == modelId && it > 0 }
                ?: model.varsayilanAdim).coerceIn(1, model.enFazlaAdim)
        )
    }
    var genislik by rememberSaveable { mutableIntStateOf(kaynak?.genislik ?: g.genislik) }
    var yukseklik by rememberSaveable { mutableIntStateOf(kaynak?.yukseklik ?: g.yukseklik) }
    var sabitTohum by rememberSaveable { mutableStateOf(kaynak?.tohum != null) }
    var tohum by rememberSaveable { mutableStateOf(kaynak?.tohum?.toString().orEmpty()) }
    var gelistiriliyor by remember { mutableStateOf(false) }

    // Ayarlar geç yüklenirse (ilk açılış) kaydedilmiş model seçilsin.
    LaunchedEffect(g.model) { if (kaynak == null && istem.isEmpty()) modelId = g.model }
    LaunchedEffect(durum.hata) { durum.hata?.let { bildirim.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Görsel Stüdyosu") },
                navigationIcon = { IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = { IconButton(onClick = galeriAc) { Icon(Icons.Default.PhotoLibrary, "Galeri") } },
            )
        },
        snackbarHost = { SnackbarHost(bildirim) },
    ) { ic ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hazir) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cloudflare hesabı gerekli", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Görseller Cloudflare Workers AI'da üretiliyor; ücretsiz planda günde " +
                                "10.000 neuron var (FLUX ile yaklaşık 170 görsel).",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = cloudflareAc) { Text("Hesabı bağla") }
                    }
                }
            }

            OutlinedTextField(
                value = istem,
                onValueChange = { istem = it },
                label = { Text("Ne çizilsin?") },
                placeholder = { Text("Örn. sisli bir sabahta Galata Kulesi, sulu boya") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    enabled = istem.isNotBlank() && !gelistiriliyor,
                    onClick = {
                        gelistiriliyor = true
                        kapsam.launch {
                            vm.istemiGelistir(istem)
                                .onSuccess { istem = it }
                                .onFailure { bildirim.showSnackbar(it.message ?: "Geliştirilemedi") }
                            gelistiriliyor = false
                        }
                    },
                ) {
                    if (gelistiriliyor) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                    Text("  İstemi geliştir")
                }
                Text(
                    "Sohbet modeliyle İngilizce, ayrıntılı istem yazar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Model", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GorselModelleri.hepsi.forEach { m ->
                    FilterChip(selected = m.id == modelId, onClick = { modelId = m.id }, label = { Text(m.ad) })
                }
            }
            Text(model.aciklama, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (model.ayarlanabilir) {
                OutlinedTextField(
                    value = negatif,
                    onValueChange = { negatif = it },
                    label = { Text("Negatif istem (olmasın)") },
                    placeholder = { Text("blurry, low quality, extra fingers") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                Text("Boyut", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    boyutlar.forEach { b ->
                        FilterChip(
                            selected = b.g == genislik && b.y == yukseklik,
                            onClick = { genislik = b.g; yukseklik = b.y },
                            label = { Text("${b.ad} ${b.g}×${b.y}") },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sabit tohum", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = sabitTohum, onCheckedChange = { sabitTohum = it })
                }
                if (sabitTohum) {
                    OutlinedTextField(
                        value = tohum,
                        onValueChange = { yeni -> tohum = yeni.filter { it.isDigit() }.take(10) },
                        label = { Text("Tohum") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Text("Boyut: 1024×1024", style = MaterialTheme.typography.bodyMedium)
            }

            Text("Adım: $adim", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = adim.toFloat(),
                onValueChange = { adim = it.roundToInt() },
                valueRange = 1f..model.enFazlaAdim.toFloat(),
                steps = (model.enFazlaAdim - 2).coerceAtLeast(0),
            )
            val gg = if (model.ayarlanabilir) genislik else 1024
            val yy = if (model.ayarlanabilir) yukseklik else 1024
            Text(
                model.tahminiNeuron(gg, yy, adim)?.let { "≈ $it neuron · günlük ücretsiz 10.000" }
                    ?: "Beta model: neuron harcamıyor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (durum.uretiyor) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedButton(onClick = vm::studyoDurdur, modifier = Modifier.fillMaxWidth()) { Text("Durdur") }
            } else {
                Button(
                    enabled = hazir && istem.isNotBlank(),
                    onClick = {
                        vm.studyoUret(
                            istem, negatif, model, adim, genislik, yukseklik,
                            if (sabitTohum) tohum.toLongOrNull() else null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Üret") }
            }

            durum.sonuc?.let { id -> gorseller.firstOrNull { it.id == id } }?.let { k ->
                val bmp by rememberGorsel(vm.gorselDosyasi(k), 1024)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(k.genislik.toFloat() / k.yukseklik)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { gorselAc(k.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    bmp?.let { Image(it, k.istem, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        ?: CircularProgressIndicator()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Galeriye kaydedildi" + (k.tohum?.let { " · tohum $it" } ?: ""),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (k.tohum != null) {
                        TextButton(onClick = { sabitTohum = true; tohum = k.tohum.toString() }) { Text("Tohumu sabitle") }
                    }
                }
            }
        }
    }
}
