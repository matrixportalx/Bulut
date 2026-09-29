package tr.bulut.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaleriEkrani(vm: BulutViewModel, geri: () -> Unit, gorselAc: (String) -> Unit, studyoAc: () -> Unit) {
    val gorseller by vm.gorseller.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galeri (${gorseller.size})") },
                navigationIcon = { IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = studyoAc,
                icon = { Icon(Icons.Default.Brush, null) },
                text = { Text("Yeni görsel") },
            )
        },
    ) { ic ->
        if (gorseller.isEmpty()) {
            Text(
                "Henüz görsel yok. Stüdyodan ya da sohbette /görsel komutuyla üretebilirsin.",
                Modifier.padding(ic).padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 4.dp, end = 4.dp,
                top = ic.calculateTopPadding() + 4.dp,
                bottom = ic.calculateBottomPadding() + 88.dp,
            ),
        ) {
            items(gorseller, key = { it.id }) { k ->
                val bmp by rememberGorsel(vm.gorselDosyasi(k), 256)
                Box(
                    Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { gorselAc(k.id) },
                ) {
                    bmp?.let { Image(it, k.istem, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
            }
        }
    }
}
