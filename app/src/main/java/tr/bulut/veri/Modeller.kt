package tr.bulut.veri

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI uyumlu bir sohbet uç noktası. Hazır sağlayıcıların hepsi (Gemini dahil)
 * bu biçimi konuşuyor; tek istemci hepsine yetiyor.
 */
@Serializable
data class Saglayici(
    val id: String,
    val ad: String,
    /** `.../chat/completions` ve `.../models` bu adresin altına eklenir. */
    val tabanUrl: String,
    val aciklama: String = "",
    /** Anahtarın alındığı sayfa; özel sunucuda boş. */
    val anahtarAdresi: String = "",
    /** Model listesi başka bir adresteyse (GitHub Models). Boşsa `tabanUrl/models`. */
    val modelListesiUrl: String = "",
    /** Doluysa listeden yalnızca bu sonekle bitenler alınır (OpenRouter `:free`). */
    val modelSoneki: String = "",
    /** Sunucudan en son çekilen liste; boşsa [varsayilanModeller] kullanılır. */
    val modeller: List<String> = emptyList(),
    val varsayilanModeller: List<String> = emptyList(),
    /** Anahtarsız çalışabilir mi (yerel sunucu). */
    val anahtarIstege: Boolean = false,
    val ozel: Boolean = false,
    /** Anahtarsız sağlayıcı kullanıcı adresini kaydedene kadar seçilemez. */
    val etkin: Boolean = false,
) {
    fun kullanilabilir(anahtarlilar: Set<String>) = id in anahtarlilar || (anahtarIstege && etkin)

    val gorunenModeller: List<String>
        get() = modeller.ifEmpty { varsayilanModeller }
}

@Serializable
data class Ayarlar(
    val saglayicilar: List<Saglayici> = HazirSaglayicilar.hepsi,
    val sonSaglayici: String = "",
    val sonModel: String = "",
    val sistemIstemi: String = "",
    /** null: sağlayıcının varsayılanı. */
    val sicaklik: Float? = null,
    /** İsteğe giden en fazla geçmiş mesaj sayısı; ücretsiz katmanların token sınırı dar. */
    val gecmisSiniri: Int = 30,
    val gorsel: GorselAyarlari = GorselAyarlari(),
)

@Serializable
enum class Rol {
    @SerialName("user") KULLANICI,
    @SerialName("assistant") ASISTAN,
}

@Serializable
data class Mesaj(
    val rol: Rol,
    val icerik: String,
    /** Akıl yürüten modellerin ayrı gönderdiği düşünce metni. */
    val dusunce: String = "",
    val hata: String? = null,
    val model: String = "",
    /** Sohbette üretilen görselin galeri kimliği. */
    val gorsel: String = "",
    /** Üretilecek/üretilmiş görselin tarifi; doluyken [gorsel] boşsa üretim bekliyor ya da düştü. */
    val gorselIstemi: String = "",
    val zaman: Long = System.currentTimeMillis(),
)

@Serializable
data class Sohbet(
    val id: String,
    val baslik: String,
    val saglayiciId: String,
    val model: String,
    val mesajlar: List<Mesaj> = emptyList(),
    val guncellendi: Long = System.currentTimeMillis(),
)
