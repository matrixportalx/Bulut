package tr.bulut.veri

import kotlinx.serialization.Serializable
import kotlin.math.ceil

/** Cloudflare Workers AI metinden görsel modeli. */
data class GorselModel(
    val id: String,
    val ad: String,
    val aciklama: String,
    val varsayilanAdim: Int,
    val enFazlaAdim: Int,
    /** Genişlik/yükseklik, negatif istem ve tohum yalnızca SD tabanlılarda var. */
    val ayarlanabilir: Boolean,
    /** Neuron fiyatı; null ise beta ve ücretsiz. */
    val karoBasina: Double? = null,
    val adimBasina: Double? = null,
) {
    /** Günlük 10.000 ücretsiz neuron'dan ne kadar yiyeceği. */
    fun tahminiNeuron(genislik: Int, yukseklik: Int, adim: Int): Int? {
        val karo = karoBasina ?: return null
        val karolar = ceil(genislik / 512.0) * ceil(yukseklik / 512.0)
        return (karolar * karo + adim * (adimBasina ?: 0.0)).toInt()
    }
}

object GorselModelleri {
    val fluxSchnell = GorselModel(
        id = "@cf/black-forest-labs/flux-1-schnell",
        ad = "FLUX.1 schnell",
        aciklama = "En iyi kalite. 1024×1024 sabit, negatif istem ve tohum yok.",
        varsayilanAdim = 4, enFazlaAdim = 8, ayarlanabilir = false,
        karoBasina = 4.8, adimBasina = 9.6,
    )
    val sdxlLightning = GorselModel(
        id = "@cf/bytedance/stable-diffusion-xl-lightning",
        ad = "SDXL Lightning",
        aciklama = "Beta, ücretsiz. Boyut, negatif istem ve tohum ayarlanır.",
        varsayilanAdim = 8, enFazlaAdim = 20, ayarlanabilir = true,
    )
    val dreamshaper = GorselModel(
        id = "@cf/lykon/dreamshaper-8-lcm",
        ad = "DreamShaper 8 LCM",
        aciklama = "Beta, ücretsiz. SD 1.5 tabanlı, fotogerçekçi; 512–768 boyutta iyi.",
        varsayilanAdim = 8, enFazlaAdim = 20, ayarlanabilir = true,
    )
    val sdxlBase = GorselModel(
        id = "@cf/stabilityai/stable-diffusion-xl-base-1.0",
        ad = "SDXL 1.0",
        aciklama = "Beta, ücretsiz. Yavaş ama ayrıntılı; 20 adım önerilir.",
        varsayilanAdim = 20, enFazlaAdim = 20, ayarlanabilir = true,
    )

    val hepsi = listOf(fluxSchnell, sdxlLightning, dreamshaper, sdxlBase)

    fun bul(id: String) = hepsi.firstOrNull { it.id == id } ?: fluxSchnell
}

@Serializable
data class GorselAyarlari(
    val hesapId: String = "",
    val model: String = GorselModelleri.fluxSchnell.id,
    val adim: Int = 0,
    val genislik: Int = 1024,
    val yukseklik: Int = 1024,
    /** Model sohbette kendi kararıyla görsel üretebilsin mi. */
    val sohbetteOtomatik: Boolean = false,
)

/** Galerideki bir görsel; dosyanın yanında `<id>.json` olarak durur. */
@Serializable
data class GorselKaydi(
    val id: String,
    val dosya: String,
    val istem: String,
    val negatif: String = "",
    val model: String,
    val adim: Int,
    val genislik: Int,
    val yukseklik: Int,
    /** null: model tohum kabul etmiyor. */
    val tohum: Long? = null,
    val zaman: Long = System.currentTimeMillis(),
)
