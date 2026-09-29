package tr.bulut.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyarlarEkrani(vm: BulutViewModel, geri: () -> Unit, saglayiciAc: (String) -> Unit) {
    val ayarlar by vm.ayarlar.collectAsStateWithLifecycle()
    val anahtarlilar by vm.anahtarlilar.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = { IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
            )
        },
    ) { ic ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = ic.calculateTopPadding(), bottom = 32.dp)) {
            item { Baslik("Sağlayıcılar") }
            items(ayarlar.saglayicilar, key = { it.id }) { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { saglayiciAc(s.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.ad, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                s.kullanilabilir(anahtarlilar) -> "Hazır · ${s.gorunenModeller.size} model"
                                s.anahtarIstege -> "Adres girilmedi"
                                else -> "Anahtar girilmedi"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (s.kullanilabilir(anahtarlilar)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp))
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Baslik("Sohbet")
                Column(Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = ayarlar.sistemIstemi,
                        onValueChange = { yeni -> vm.ayarlariDegistir { it.copy(sistemIstemi = yeni) } },
                        label = { Text("Sistem istemi") },
                        placeholder = { Text("Örn. Türkçe ve kısa yanıt ver.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                    )

                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sıcaklığı ayarla", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Kapalıyken modelin varsayılanı kullanılır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = ayarlar.sicaklik != null,
                            onCheckedChange = { acik -> vm.ayarlariDegistir { it.copy(sicaklik = if (acik) 0.7f else null) } },
                        )
                    }
                    ayarlar.sicaklik?.let { t ->
                        Text("Sıcaklık: ${"%.1f".format(t)}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = t,
                            onValueChange = { v -> vm.ayarlariDegistir { it.copy(sicaklik = (v * 10).roundToInt() / 10f) } },
                            valueRange = 0f..2f,
                            steps = 19,
                        )
                    }

                    Text(
                        "Gönderilen geçmiş: son ${ayarlar.gecmisSiniri} mesaj",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        "Ücretsiz katmanların dakikalık token sınırı dar; uzun sohbette sınırı düşürmek 429 hatasını azaltır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = ayarlar.gecmisSiniri.toFloat(),
                        onValueChange = { v -> vm.ayarlariDegistir { it.copy(gecmisSiniri = v.roundToInt()) } },
                        valueRange = 2f..100f,
                    )

                    Text(
                        "API anahtarları cihazın donanım anahtar deposuyla şifrelenir. Sohbetler " +
                            "yalnızca bu cihazda tutulur ve yedeklemeye dahil edilmez; mesajlar " +
                            "yalnızca seçtiğin sağlayıcıya gönderilir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Baslik(metin: String) {
    Text(
        metin,
        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
