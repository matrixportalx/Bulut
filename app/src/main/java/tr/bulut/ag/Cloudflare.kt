package tr.bulut.ag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import tr.bulut.veri.GorselModel

/** Cloudflare Workers AI biçiminin Android'e bağlı olmayan kısmı. */
object CloudflareProtokol {

    data class Istek(
        val model: GorselModel,
        val istem: String,
        val negatif: String,
        val adim: Int,
        val genislik: Int,
        val yukseklik: Int,
        val tohum: Long?,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun adres(hesapId: String, yol: String) =
        "https://api.cloudflare.com/client/v4/accounts/${hesapId.trim()}/$yol"

    /** Model şemada olmayan alanı reddettiği için yalnızca desteklenenler gönderilir. */
    fun govde(i: Istek): String = buildJsonObject {
        put("prompt", i.istem)
        if (i.model.ayarlanabilir) {
            put("num_steps", i.adim)
            put("width", i.genislik)
            put("height", i.yukseklik)
            if (i.negatif.isNotBlank()) put("negative_prompt", i.negatif)
            if (i.tohum != null) put("seed", i.tohum)
        } else {
            put("steps", i.adim)
        }
    }.toString()

    /**
     * FLUX `{"result":{"image":"<base64 jpeg>"}}` döndürüyor, SD modelleri ise
     * doğrudan PNG baytı. İkisi de burada görsel baytına çevrilir.
     */
    fun yanitCoz(icerikTuru: String?, bayt: ByteArray): ByteArray {
        if (icerikTuru?.startsWith("image/") == true || pngMi(bayt) || jpegMi(bayt)) return bayt
        val kok = json.parseToJsonElement(bayt.toString(Charsets.UTF_8)).jsonObject
        hata(kok)?.let { throw SaglayiciHatasi(it) }
        val sonuc = kok["result"] as? JsonObject ?: kok
        val b64 = (sonuc["image"] as? JsonPrimitive)?.contentOrNull
            ?: throw SaglayiciHatasi("Yanıtta görsel yok.")
        return Base64.getDecoder().decode(b64.substringAfter("base64,"))
    }

    fun hataMesaji(kod: Int, govde: String): String {
        val sunucu = runCatching { hata(json.parseToJsonElement(govde).jsonObject) }.getOrNull()
            ?: govde.take(300).trim()
        val onek = when (kod) {
            400 -> "İstek reddedildi (400)"
            401, 403 -> "Hesap kimliği ya da API belirteci geçersiz ($kod). Belirtecin Workers AI izni olmalı"
            404 -> "Model ya da hesap bulunamadı (404)"
            429 -> "Günlük ücretsiz kota ya da hız sınırı doldu (429)"
            in 500..599 -> "Cloudflare tarafında hata ($kod)"
            else -> "HTTP $kod"
        }
        return if (sunucu.isBlank()) onek else "$onek: $sunucu"
    }

    /** `/ai/models/search` yanıtından model adları. */
    fun modelSayisi(govde: String): Int =
        ((json.parseToJsonElement(govde).jsonObject["result"]) as? JsonArray)?.size ?: 0

    fun uzanti(bayt: ByteArray) = if (pngMi(bayt)) "png" else "jpg"

    private fun hata(o: JsonObject): String? {
        if ((o["success"] as? JsonPrimitive)?.contentOrNull != "false") return null
        val hatalar = o["errors"] as? JsonArray
        return hatalar?.mapNotNull { (it as? JsonObject)?.get("message")?.let { m -> (m as? JsonPrimitive)?.contentOrNull } }
            ?.joinToString("; ")?.ifBlank { null } ?: "Bilinmeyen hata"
    }

    private fun pngMi(b: ByteArray) = b.size > 4 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte()
    private fun jpegMi(b: ByteArray) = b.size > 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte()
}

class GorselIstemci {

    suspend fun uret(hesapId: String, belirtec: String, istek: CloudflareProtokol.Istek): ByteArray =
        withContext(Dispatchers.IO) {
            val b = ac(CloudflareProtokol.adres(hesapId, "ai/run/${istek.model.id}"), belirtec)
            try {
                b.requestMethod = "POST"
                b.doOutput = true
                b.setRequestProperty("Content-Type", "application/json")
                b.outputStream.use { it.write(CloudflareProtokol.govde(istek).toByteArray(Charsets.UTF_8)) }
                val kod = b.responseCode
                if (kod !in 200..299) {
                    throw SaglayiciHatasi(CloudflareProtokol.hataMesaji(kod, hataGovdesi(b)))
                }
                CloudflareProtokol.yanitCoz(b.contentType, b.inputStream.use { it.readBytes() })
            } finally {
                b.disconnect()
            }
        }

    /** Hesap kimliği ve belirteci sınar; erişilebilen görsel modeli sayısını döndürür. */
    suspend fun dene(hesapId: String, belirtec: String): Int = withContext(Dispatchers.IO) {
        val b = ac(CloudflareProtokol.adres(hesapId, "ai/models/search?task=Text-to-Image"), belirtec)
        try {
            val kod = b.responseCode
            if (kod !in 200..299) throw SaglayiciHatasi(CloudflareProtokol.hataMesaji(kod, hataGovdesi(b)))
            CloudflareProtokol.modelSayisi(b.inputStream.bufferedReader().readText())
        } finally {
            b.disconnect()
        }
    }

    private fun ac(url: String, belirtec: String) =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("Authorization", "Bearer ${belirtec.trim()}")
        }

    private fun hataGovdesi(b: HttpURLConnection): String =
        runCatching { b.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
}
