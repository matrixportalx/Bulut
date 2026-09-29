package tr.bulut.veri

import android.content.Context
import java.io.File

/** Galeri: filesDir/gorseller altında görsel dosyası + aynı adlı JSON künye. */
class GorselDepo(context: Context) {

    val dizin = File(context.filesDir, "gorseller").apply { mkdirs() }

    fun hepsi(): List<GorselKaydi> =
        dizin.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { runCatching { Depo.json.decodeFromString<GorselKaydi>(it.readText()) }.getOrNull() }
            .filter { dosya(it).exists() }
            .sortedByDescending { it.zaman }

    fun dosya(kayit: GorselKaydi) = File(dizin, kayit.dosya)

    fun yaz(kayit: GorselKaydi, bayt: ByteArray) {
        File(dizin, kayit.dosya).writeBytes(bayt)
        File(dizin, "${kayit.id}.json").writeText(Depo.json.encodeToString(GorselKaydi.serializer(), kayit))
    }

    fun sil(kayit: GorselKaydi) {
        dosya(kayit).delete()
        File(dizin, "${kayit.id}.json").delete()
    }
}
