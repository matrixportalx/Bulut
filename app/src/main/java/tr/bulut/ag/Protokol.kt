package tr.bulut.ag

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * OpenAI sohbet protokolünün Android'e bağlı olmayan kısmı. Ağ katmanı yalnızca
 * baytları taşır; biçim burada kurulup çözülür ve JVM testleriyle sınanır.
 */
object Protokol {

    data class IstekMesaji(val rol: String, val icerik: String)

    /** Akıştan gelen tek parça. Aynı olayda ikisi birden de gelebilir. */
    data class Parca(val icerik: String = "", val dusunce: String = "")

    sealed interface SatirSonucu {
        data class Veri(val parca: Parca) : SatirSonucu
        data class Hata(val mesaj: String) : SatirSonucu
        data object Bitti : SatirSonucu
        data object Bos : SatirSonucu
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun istekGovdesi(model: String, mesajlar: List<IstekMesaji>, sicaklik: Float?): String =
        buildJsonObject {
            put("model", model)
            put("stream", true)
            if (sicaklik != null) put("temperature", sicaklik)
            put("messages", buildJsonArray {
                mesajlar.forEach { m ->
                    add(buildJsonObject {
                        put("role", m.rol)
                        put("content", m.icerik)
                    })
                }
            })
        }.toString()

    /** Server-sent events akışının bir satırı. */
    fun satirCoz(satir: String): SatirSonucu {
        val s = satir.trim()
        if (!s.startsWith("data:")) return SatirSonucu.Bos
        val veri = s.removePrefix("data:").trim()
        if (veri.isEmpty()) return SatirSonucu.Bos
        if (veri == "[DONE]") return SatirSonucu.Bitti
        val kok = runCatching { json.parseToJsonElement(veri).jsonObject }.getOrNull()
            ?: return SatirSonucu.Bos
        hataMetni(kok)?.let { return SatirSonucu.Hata(it) }
        val secim = (kok["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
            ?: return SatirSonucu.Bos
        val delta = secim["delta"] as? JsonObject ?: secim["message"] as? JsonObject
            ?: return SatirSonucu.Bos
        val icerik = delta.metin("content")
        // Sağlayıcılar düşünce alanına farklı adlar veriyor.
        val dusunce = delta.metin("reasoning_content").ifEmpty { delta.metin("reasoning") }
        if (icerik.isEmpty() && dusunce.isEmpty()) return SatirSonucu.Bos
        return SatirSonucu.Veri(Parca(icerik, dusunce))
    }

    /** Başarısız HTTP yanıtını kullanıcıya gösterilecek cümleye çevirir. */
    fun hataMesaji(kod: Int, govde: String): String {
        val sunucu = runCatching {
            val kok = json.parseToJsonElement(govde)
            // Gemini bazen hatayı tek elemanlı dizi içinde döndürüyor.
            val nesne = (if (kok is JsonArray) kok.firstOrNull() else kok)?.jsonObject
            nesne?.let { hataMetni(it) }
        }.getOrNull() ?: govde.take(300).trim()
        val onek = when (kod) {
            400 -> "İstek reddedildi (400)"
            401, 403 -> "Anahtar geçersiz ya da bu modele erişim yok ($kod)"
            404 -> "Model ya da adres bulunamadı (404)"
            413 -> "Sohbet bu model için fazla uzun (413)"
            429 -> "Ücretsiz kota ya da hız sınırı doldu (429). Biraz bekleyin veya başka bir model seçin"
            in 500..599 -> "Sağlayıcı tarafında hata ($kod)"
            else -> "HTTP $kod"
        }
        return if (sunucu.isBlank()) onek else "$onek: $sunucu"
    }

    /** `/models` yanıtı: OpenAI biçimi `{"data":[{"id":..}]}`, GitHub kataloğu düz dizi. */
    fun modelListesiCoz(govde: String, sonek: String): List<String> {
        val kok = json.parseToJsonElement(govde)
        val dizi = when (kok) {
            is JsonArray -> kok
            is JsonObject -> (kok["data"] ?: kok["models"])?.jsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return dizi.mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            // Gemini'nin yerel listesi "models/gemini-..." biçiminde ad veriyor.
            (o.metin("id").ifEmpty { o.metin("name") }).removePrefix("models/").ifEmpty { null }
        }
            .filter { sonek.isEmpty() || it.endsWith(sonek) }
            .filterNot { gomuMu(it) }
            .distinct()
            .sorted()
    }

    /** Sohbet için anlamsız modelleri (gömme, ses, görsel üretimi) listeden ayıklar. */
    private fun gomuMu(id: String): Boolean {
        val k = id.lowercase()
        return listOf("embed", "whisper", "tts", "imagen", "veo", "aqa", "guard", "moderation", "rerank")
            .any { it in k }
    }

    private fun hataMetni(o: JsonObject): String? {
        val hata = o["error"] ?: return null
        return when (hata) {
            is JsonObject -> hata.metin("message").ifEmpty { hata.toString() }
            is JsonPrimitive -> hata.contentOrNull
            else -> hata.toString()
        }
    }

    private fun JsonObject.metin(alan: String): String =
        (this[alan] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull.orEmpty()
}
