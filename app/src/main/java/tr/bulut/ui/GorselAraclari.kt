package tr.bulut.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val onbellek = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

/** Dosyayı arka planda, [enFazlaPx]'e yakın bir boyuta küçülterek açar. */
@Composable
fun rememberGorsel(dosya: File, enFazlaPx: Int): State<ImageBitmap?> {
    val anahtar = "${dosya.path}@$enFazlaPx"
    return produceState(onbellek.get(anahtar), anahtar) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val sinir = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(dosya.path, sinir)
                var oran = 1
                while (maxOf(sinir.outWidth, sinir.outHeight) / (oran * 2) >= enFazlaPx) oran *= 2
                BitmapFactory.decodeFile(dosya.path, BitmapFactory.Options().apply { inSampleSize = oran })
                    ?.asImageBitmap()?.also { onbellek.put(anahtar, it) }
            }.getOrNull()
        }
    }
}

fun gorselPaylas(context: Context, dosya: File, metin: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.dosya", dosya)
    val niyet = Intent(Intent.ACTION_SEND).apply {
        type = if (dosya.extension == "png") "image/png" else "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, metin)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(niyet, "Paylaş"))
}

/**
 * Resimler/Bulut klasörüne kopyalar. Android 10 öncesinde depolama izni
 * gerekeceği için orada yalnızca paylaşım yolu var.
 */
suspend fun cihazaKaydet(context: Context, dosya: File): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "Android 10 ve üstü gerekli; Paylaş ile kaydedebilirsin." }
        val degerler = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Bulut_${dosya.nameWithoutExtension.take(8)}.${dosya.extension}")
            put(MediaStore.Images.Media.MIME_TYPE, if (dosya.extension == "png") "image/png" else "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Bulut")
        }
        val cozucu = context.contentResolver
        val uri = cozucu.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, degerler)
            ?: error("Galeriye yazılamadı.")
        cozucu.openOutputStream(uri).use { cikis -> dosya.inputStream().use { it.copyTo(cikis!!) } }
        Unit
    }
}
