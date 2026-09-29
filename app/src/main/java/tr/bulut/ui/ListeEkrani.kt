package tr.bulut.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import tr.bulut.veri.Sohbet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeEkrani(vm: BulutViewModel, sohbetAc: (String) -> Unit, ayarlarAc: () -> Unit) {
    val sohbetler by vm.sohbetler.collectAsStateWithLifecycle()
    val ayarlar by vm.ayarlar.collectAsStateWithLifecycle()
    val anahtarlilar by vm.anahtarlilar.collectAsStateWithLifecycle()
    val uretenler by vm.uretenler.collectAsStateWithLifecycle()
    val hazir = ayarlar.saglayicilar.any { it.kullanilabilir(anahtarlilar) }
    var silinecek by remember { mutableStateOf<Sohbet?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulut") },
                actions = {
                    IconButton(onClick = ayarlarAc) { Icon(Icons.Default.Settings, "Ayarlar") }
                },
            )
        },
        floatingActionButton = {
            if (hazir) {
                ExtendedFloatingActionButton(
                    onClick = { vm.yeniSohbet()?.let(sohbetAc) ?: ayarlarAc() },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Yeni sohbet") },
                )
            }
        },
    ) { ic ->
        val gorunen = sohbetler.filter { it.mesajlar.isNotEmpty() }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = ic.calculateTopPadding(),
                bottom = ic.calculateBottomPadding() + 88.dp,
            ),
        ) {
            if (!hazir) {
                item { KurulumKarti(ayarlarAc) }
            } else if (gorunen.isEmpty()) {
                item {
                    Text(
                        "Henüz sohbet yok. Sağ alttan yenisini başlat.",
                        Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(gorunen, key = { it.id }) { s ->
                val saglayiciAdi = ayarlar.saglayicilar.firstOrNull { it.id == s.saglayiciId }?.ad ?: s.saglayiciId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { sohbetAc(s.id) }
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(s.baslik.ifBlank { "Adsız sohbet" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$saglayiciAdi · ${s.model} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(s.guncellendi))}",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (s.id in uretenler) CircularProgressIndicator(Modifier.padding(12.dp), strokeWidth = 2.dp)
                    IconButton(onClick = { silinecek = s }) { Icon(Icons.Default.DeleteOutline, "Sil") }
                }
                HorizontalDivider()
            }
        }
    }

    silinecek?.let { s ->
        AlertDialog(
            onDismissRequest = { silinecek = null },
            title = { Text("Sohbet silinsin mi?") },
            text = { Text(s.baslik.ifBlank { "Adsız sohbet" }) },
            confirmButton = { TextButton(onClick = { vm.sohbetSil(s.id); silinecek = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { silinecek = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun KurulumKarti(ayarlarAc: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Başlamak için bir anahtar gir", style = MaterialTheme.typography.titleMedium)
            Text(
                "Gemini, Groq, OpenRouter, Mistral, Cerebras ve GitHub Models ücretsiz " +
                    "anahtar veriyor. Ayarlar'dan birini seç, sayfasından anahtarı alıp yapıştır. " +
                    "Kendi sunucun varsa (Ollama, llama-server) \"Özel sunucu\" anahtarsız da çalışır.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = ayarlarAc) { Text("Ayarlar'a git") }
        }
    }
}
