package tr.bulut.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import tr.bulut.veri.GorselModelleri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GorselEkrani(vm: BulutViewModel, gorselId: String, geri: () -> Unit, studyodaAc: (String) -> Unit) {
    val gorseller by vm.gorseller.collectAsStateWithLifecycle()
    val k = gorseller.firstOrNull { it.id == gorselId }
    val baglam = LocalContext.current
    val kapsam = rememberCoroutineScope()
    val bildirim = remember { SnackbarHostState() }
    var silSor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Görsel") },
                navigationIcon = { IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
                actions = {
                    if (k != null) {
                        IconButton(onClick = { gorselPaylas(baglam, vm.gorselDosyasi(k), k.istem) }) {
                            Icon(Icons.Default.Share, "Paylaş")
                        }
                        IconButton(onClick = {
                            kapsam.launch {
                                cihazaKaydet(baglam, vm.gorselDosyasi(k))
                                    .onSuccess { bildirim.showSnackbar("Resimler/Bulut klasörüne kaydedildi") }
                                    .onFailure { bildirim.showSnackbar(it.message ?: "Kaydedilemedi") }
                            }
                        }) { Icon(Icons.Default.Download, "Cihaza kaydet") }
                        IconButton(onClick = { studyodaAc(k.id) }) { Icon(Icons.Default.Brush, "İstemi stüdyoda kullan") }
                        IconButton(onClick = { silSor = true }) { Icon(Icons.Default.DeleteOutline, "Sil") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(bildirim) },
    ) { ic ->
        if (k == null) {
            Text("Görsel bulunamadı; silinmiş olabilir.", Modifier.padding(ic).padding(24.dp))
            return@Scaffold
        }
        val bmp by rememberGorsel(vm.gorselDosyasi(k), 2048)
        var olcek by remember { mutableFloatStateOf(1f) }
        var kayma by remember { mutableStateOf(Offset.Zero) }
        Column(
            Modifier
                .fillMaxSize()
                .padding(ic)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(k.genislik.toFloat() / k.yukseklik)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, kaydirma, yakinlastirma, _ ->
                            olcek = (olcek * yakinlastirma).coerceIn(1f, 5f)
                            kayma = if (olcek == 1f) Offset.Zero else kayma + kaydirma
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                bmp?.let {
                    Image(
                        it, k.istem,
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = olcek; scaleY = olcek
                            translationX = kayma.x; translationY = kayma.y
                        },
                    )
                } ?: CircularProgressIndicator()
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectionContainer { Text(k.istem, style = MaterialTheme.typography.bodyLarge) }
                if (k.negatif.isNotBlank()) {
                    Text("Negatif: ${k.negatif}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    listOfNotNull(
                        GorselModelleri.bul(k.model).ad,
                        "${k.genislik}×${k.yukseklik}",
                        "${k.adim} adım",
                        k.tohum?.let { "tohum $it" },
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(k.zaman)),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }

    if (silSor && k != null) {
        AlertDialog(
            onDismissRequest = { silSor = false },
            title = { Text("Görsel silinsin mi?") },
            text = { Text("Sohbette görünüyorsa oradan da kalkar.") },
            confirmButton = { TextButton(onClick = { silSor = false; vm.gorselSil(k); geri() }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { silSor = false }) { Text("Vazgeç") } },
        )
    }
}
