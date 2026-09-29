package tr.bulut.ag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class SaglayiciHatasi(mesaj: String) : IOException(mesaj)

/** OpenAI uyumlu uç noktalar için akışlı istemci; bağımlılık yok, HttpURLConnection yeterli. */
class Istemci {

    suspend fun modeller(tabanUrl: String, listeUrl: String, anahtar: String, sonek: String): List<String> =
        withContext(Dispatchers.IO) {
            val baglanti = ac(listeUrl.ifEmpty { adres(tabanUrl, "models") }, anahtar)
            try {
                baglanti.requestMethod = "GET"
                val kod = baglanti.responseCode
                if (kod !in 200..299) throw SaglayiciHatasi(Protokol.hataMesaji(kod, hataGovdesi(baglanti)))
                Protokol.modelListesiCoz(baglanti.inputStream.bufferedReader().readText(), sonek)
            } finally {
                baglanti.disconnect()
            }
        }

    /**
     * Yanıtı parça parça [parcaGeldi]'ye verir. Çağıran iş iptal edilirse bağlantı
     * kapatılır ve okuma hemen biter.
     */
    suspend fun akis(
        tabanUrl: String,
        anahtar: String,
        model: String,
        mesajlar: List<Protokol.IstekMesaji>,
        sicaklik: Float?,
        parcaGeldi: (Protokol.Parca) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val baglanti = ac(adres(tabanUrl, "chat/completions"), anahtar)
        val kayit = currentCoroutineContext()[Job]?.invokeOnCompletion { baglanti.disconnect() }
        try {
            baglanti.requestMethod = "POST"
            baglanti.doOutput = true
            baglanti.setRequestProperty("Content-Type", "application/json")
            baglanti.setRequestProperty("Accept", "text/event-stream")
            baglanti.outputStream.use {
                it.write(Protokol.istekGovdesi(model, mesajlar, sicaklik).toByteArray(Charsets.UTF_8))
            }
            val kod = baglanti.responseCode
            if (kod !in 200..299) throw SaglayiciHatasi(Protokol.hataMesaji(kod, hataGovdesi(baglanti)))
            baglanti.inputStream.bufferedReader(Charsets.UTF_8).use { okuyucu ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val satir = okuyucu.readLine() ?: break
                    when (val sonuc = Protokol.satirCoz(satir)) {
                        is Protokol.SatirSonucu.Veri -> parcaGeldi(sonuc.parca)
                        is Protokol.SatirSonucu.Hata -> throw SaglayiciHatasi(sonuc.mesaj)
                        Protokol.SatirSonucu.Bitti -> break
                        Protokol.SatirSonucu.Bos -> Unit
                    }
                }
            }
        } finally {
            kayit?.dispose()
            baglanti.disconnect()
        }
    }

    private fun ac(url: String, anahtar: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            // Akıl yürüten modeller ilk parçayı dakikalarca geciktirebiliyor.
            readTimeout = 180_000
            if (anahtar.isNotBlank()) setRequestProperty("Authorization", "Bearer ${anahtar.trim()}")
            // OpenRouter uygulamayı bu başlıklarla tanıyor; zorunlu değil.
            setRequestProperty("X-Title", "Bulut")
        }

    private fun hataGovdesi(b: HttpURLConnection): String =
        runCatching { b.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()

    companion object {
        fun adres(taban: String, yol: String) = taban.trim().trimEnd('/') + "/" + yol
    }
}
