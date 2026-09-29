package tr.bulut.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tr.bulut.veri.GorselModelleri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CloudflareEkrani(vm: BulutViewModel, geri: () -> Unit) {
    val ayarlar by vm.ayarlar.collectAsStateWithLifecycle()
    val g = ayarlar.gorsel
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val bildirim = remember { SnackbarHostState() }

    var hesap by rememberSaveable { mutableStateOf(g.hesapId) }
    var belirtec by rememberSaveable { mutableStateOf("") }
    var yuklendi by rememberSaveable { mutableStateOf(false) }
    var goster by rememberSaveable { mutableStateOf(false) }
    var deneniyor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!yuklendi) {
            belirtec = withContext(Dispatchers.IO) { vm.anahtarOku(BulutViewModel.CLOUDFLARE) }
            if (hesap.isEmpty()) hesap = g.hesapId
            yuklendi = true
        }
    }

    fun ac(url: String) = baglam.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloudflare görsel") },
                navigationIcon = { IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
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
            Text(
                "Görseller Cloudflare Workers AI'da üretilir. Ücretsiz planda günde 10.000 neuron " +
                    "var: FLUX.1 schnell ile 4 adımlı 1024×1024 görsel yaklaşık 58 neuron, yani " +
                    "günde ~170 görsel. SDXL ve DreamShaper beta olduğu için şimdilik neuron harcamıyor.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "1. Hesap kimliği: Cloudflare panelinde Workers AI sayfası → \"REST API ile kullan\".\n" +
                    "2. API belirteci: Profil → API Tokens → Create Token → \"Workers AI\" şablonu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { ac("https://dash.cloudflare.com/?to=/:account/ai/workers-ai") }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    Text("  Workers AI")
                }
                OutlinedButton(onClick = { ac("https://dash.cloudflare.com/profile/api-tokens") }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    Text("  Belirteç al")
                }
            }

            OutlinedTextField(
                value = hesap,
                onValueChange = { hesap = it.trim() },
                label = { Text("Hesap kimliği (Account ID)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = belirtec,
                onValueChange = { belirtec = it },
                label = { Text("API belirteci") },
                singleLine = true,
                visualTransformation = if (goster) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { goster = !goster }) {
                        Icon(if (goster) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Göster")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(enabled = yuklendi, onClick = {
                    vm.cloudflareKaydet(hesap, belirtec)
                    kapsam.launch { bildirim.showSnackbar("Kaydedildi") }
                }) { Text("Kaydet") }
                OutlinedButton(
                    enabled = !deneniyor && hesap.isNotBlank() && belirtec.isNotBlank(),
                    onClick = {
                        deneniyor = true
                        kapsam.launch {
                            vm.cloudflareDene(hesap, belirtec)
                                .onSuccess {
                                    vm.cloudflareKaydet(hesap, belirtec)
                                    bildirim.showSnackbar("Bağlantı çalışıyor ($it görsel modeli), kaydedildi")
                                }
                                .onFailure { bildirim.showSnackbar(it.message ?: "Bağlanamadı") }
                            deneniyor = false
                        }
                    },
                ) {
                    if (deneniyor) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Dene")
                }
            }

            HorizontalDivider()
            Text("Sohbette", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Sohbette \"/görsel <tarif>\" yazınca görsel doğrudan üretilir. Aşağıdaki seçenek " +
                    "açıkken model de, sen istediğinde kendi kararıyla görsel üretebilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Model görsel üretebilsin", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = g.sohbetteOtomatik,
                    onCheckedChange = { acik -> vm.ayarlariDegistir { it.copy(gorsel = it.gorsel.copy(sohbetteOtomatik = acik)) } },
                )
            }
            Text("Sohbette kullanılan görsel modeli", style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GorselModelleri.hepsi.forEach { m ->
                    FilterChip(
                        selected = m.id == g.model,
                        onClick = {
                            vm.ayarlariDegistir {
                                it.copy(gorsel = it.gorsel.copy(model = m.id, adim = m.varsayilanAdim))
                            }
                        },
                        label = { Text(m.ad) },
                    )
                }
            }
            Text(
                "Boyut ve adım, stüdyoda en son kullandığın ayarlardan alınır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
