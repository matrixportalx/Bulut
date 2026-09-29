package tr.bulut.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaglayiciEkrani(vm: BulutViewModel, saglayiciId: String, geri: () -> Unit) {
    val ayarlar by vm.ayarlar.collectAsStateWithLifecycle()
    val saglayici = ayarlar.saglayicilar.firstOrNull { it.id == saglayiciId } ?: return
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val bildirim = remember { SnackbarHostState() }

    var anahtar by rememberSaveable { mutableStateOf("") }
    var anahtarYuklendi by rememberSaveable { mutableStateOf(false) }
    var goster by rememberSaveable { mutableStateOf(false) }
    var adres by rememberSaveable { mutableStateOf(saglayici.tabanUrl) }
    var elleModel by rememberSaveable { mutableStateOf("") }
    var getiriliyor by remember { mutableStateOf(false) }

    LaunchedEffect(saglayiciId) {
        if (!anahtarYuklendi) {
            anahtar = withContext(Dispatchers.IO) { vm.anahtarOku(saglayiciId) }
            anahtarYuklendi = true
        }
    }

    fun guncel() = saglayici.copy(tabanUrl = adres.trim(), etkin = saglayici.anahtarIstege || saglayici.etkin)

    fun kaydet() {
        vm.saglayiciKaydet(guncel(), anahtar)
        kapsam.launch { bildirim.showSnackbar("Kaydedildi") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(saglayici.ad) },
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
            Text(saglayici.aciklama, style = MaterialTheme.typography.bodyMedium)

            if (saglayici.anahtarAdresi.isNotEmpty()) {
                OutlinedButton(onClick = {
                    baglam.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(saglayici.anahtarAdresi)))
                }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    Text("  Anahtar al")
                }
            }

            if (saglayici.ozel) {
                OutlinedTextField(
                    value = adres,
                    onValueChange = { adres = it },
                    label = { Text("Sunucu adresi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = anahtar,
                onValueChange = { anahtar = it },
                label = { Text(if (saglayici.anahtarIstege) "API anahtarı (isteğe bağlı)" else "API anahtarı") },
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
                Button(onClick = ::kaydet, enabled = anahtarYuklendi) { Text("Kaydet") }
                OutlinedButton(
                    enabled = !getiriliyor && (anahtar.isNotBlank() || saglayici.anahtarIstege),
                    onClick = {
                        getiriliyor = true
                        kapsam.launch {
                            // Getirme başarılıysa anahtar da doğru demektir; ayrıca kaydet.
                            vm.modelleriGetir(guncel(), anahtar)
                                .onSuccess { liste ->
                                    vm.saglayiciKaydet(guncel().copy(modeller = liste.ifEmpty { saglayici.modeller }), anahtar)
                                    bildirim.showSnackbar(
                                        if (liste.isEmpty()) "Sunucu boş liste döndürdü" else "${liste.size} model bulundu, kaydedildi"
                                    )
                                }
                                .onFailure { bildirim.showSnackbar(it.message ?: "Liste alınamadı") }
                            getiriliyor = false
                        }
                    },
                ) {
                    if (getiriliyor) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Modelleri getir")
                }
            }

            HorizontalDivider()
            Text(
                if (saglayici.modeller.isEmpty()) "Modeller (varsayılan liste)" else "Modeller (sunucudan)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            saglayici.gorunenModeller.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = {
                        vm.saglayiciGuncelle(saglayici.copy(modeller = saglayici.gorunenModeller - m))
                    }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Kaldır", Modifier.size(16.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = elleModel,
                    onValueChange = { elleModel = it },
                    label = { Text("Model adını elle ekle") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = elleModel.isNotBlank(),
                    onClick = {
                        val yeni = (saglayici.gorunenModeller + elleModel.trim()).distinct()
                        vm.saglayiciGuncelle(saglayici.copy(modeller = yeni))
                        elleModel = ""
                    },
                ) { Icon(Icons.Default.Add, "Ekle") }
            }
            if (saglayici.modeller.isNotEmpty() && saglayici.varsayilanModeller.isNotEmpty()) {
                Text(
                    "Varsayılan listeye dön",
                    Modifier.clickable { vm.saglayiciGuncelle(saglayici.copy(modeller = emptyList())) },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
