package tr.bulut.veri

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Ayarlar ve sohbetler filesDir altında JSON; her sohbet ayrı dosya. */
class Depo(context: Context) {

    private val kok = context.filesDir
    private val sohbetDizini = File(kok, "sohbetler").apply { mkdirs() }
    private val ayarDosyasi = File(kok, "ayarlar.json")

    fun ayarlariOku(): Ayarlar {
        val okunan = runCatching { json.decodeFromString<Ayarlar>(ayarDosyasi.readText()) }
            .getOrDefault(Ayarlar())
        return okunan.copy(saglayicilar = HazirSaglayicilar.birlestir(okunan.saglayicilar))
    }

    fun ayarlariYaz(ayarlar: Ayarlar) = guvenliYaz(ayarDosyasi, json.encodeToString(ayarlar))

    fun sohbetleriOku(): List<Sohbet> =
        sohbetDizini.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { runCatching { json.decodeFromString<Sohbet>(it.readText()) }.getOrNull() }
            .sortedByDescending { it.guncellendi }

    fun sohbetYaz(sohbet: Sohbet) =
        guvenliYaz(File(sohbetDizini, "${sohbet.id}.json"), json.encodeToString(sohbet))

    fun sohbetSil(id: String) {
        File(sohbetDizini, "$id.json").delete()
    }

    /** Yazma yarıda kesilirse eski dosya bozulmasın diye önce geçici dosyaya. */
    private fun guvenliYaz(hedef: File, metin: String) {
        val gecici = File(hedef.parentFile, hedef.name + ".tmp")
        gecici.writeText(metin)
        if (!gecici.renameTo(hedef)) {
            hedef.writeText(metin)
            gecici.delete()
        }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
