package tr.bulut.ag

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tr.bulut.ag.Protokol.SatirSonucu

class ProtokolTest {

    @Test
    fun `icerik parcasi okunur`() {
        val s = Protokol.satirCoz("""data: {"choices":[{"index":0,"delta":{"content":"Merhaba"}}]}""")
        assertEquals(SatirSonucu.Veri(Protokol.Parca(icerik = "Merhaba")), s)
    }

    @Test
    fun `dusunce alani iki adla da okunur`() {
        val a = Protokol.satirCoz("""data: {"choices":[{"delta":{"reasoning_content":"hmm"}}]}""")
        val b = Protokol.satirCoz("""data:{"choices":[{"delta":{"reasoning":"peki","content":null}}]}""")
        assertEquals(SatirSonucu.Veri(Protokol.Parca(dusunce = "hmm")), a)
        assertEquals(SatirSonucu.Veri(Protokol.Parca(dusunce = "peki")), b)
    }

    @Test
    fun `bitis yorum ve bos satirlar`() {
        assertEquals(SatirSonucu.Bitti, Protokol.satirCoz("data: [DONE]"))
        assertEquals(SatirSonucu.Bos, Protokol.satirCoz(": OPENROUTER PROCESSING"))
        assertEquals(SatirSonucu.Bos, Protokol.satirCoz(""))
        assertEquals(SatirSonucu.Bos, Protokol.satirCoz("""data: {"choices":[{"delta":{"role":"assistant"}}]}"""))
        assertEquals(SatirSonucu.Bos, Protokol.satirCoz("""data: {"choices":[]}"""))
        assertEquals(SatirSonucu.Bos, Protokol.satirCoz("data: {bozuk"))
    }

    @Test
    fun `akis icindeki hata yakalanir`() {
        val s = Protokol.satirCoz("""data: {"error":{"message":"Rate limit exceeded","code":429}}""")
        assertEquals(SatirSonucu.Hata("Rate limit exceeded"), s)
    }

    @Test
    fun `istek govdesi dogru kurulur`() {
        val govde = Protokol.istekGovdesi(
            "gemini-2.5-flash",
            listOf(Protokol.IstekMesaji("system", "Kısa yaz"), Protokol.IstekMesaji("user", "\"tırnak\"\nsatır")),
            0.5f,
        )
        val o = Json.parseToJsonElement(govde).jsonObject
        assertEquals("gemini-2.5-flash", o["model"]!!.jsonPrimitive.content)
        assertEquals("true", o["stream"]!!.jsonPrimitive.content)
        assertEquals(0.5, o["temperature"]!!.jsonPrimitive.content.toDouble(), 1e-6)
        val m = o["messages"]!!.jsonArray
        assertEquals(2, m.size)
        assertEquals("\"tırnak\"\nsatır", m[1].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun `sicaklik yoksa alan gonderilmez`() {
        val o = Json.parseToJsonElement(Protokol.istekGovdesi("m", emptyList(), null)).jsonObject
        assertFalse("temperature" in o)
    }

    @Test
    fun `hata mesajlari`() {
        assertEquals(
            "Anahtar geçersiz ya da bu modele erişim yok (401): Invalid API key",
            Protokol.hataMesaji(401, """{"error":{"message":"Invalid API key","type":"invalid_request_error"}}"""),
        )
        // Gemini hatayı dizi içinde döndürebiliyor.
        val gemini = Protokol.hataMesaji(429, """[{"error":{"code":429,"message":"Quota exceeded","status":"RESOURCE_EXHAUSTED"}}]""")
        assertTrue(gemini.startsWith("Ücretsiz kota"))
        assertTrue(gemini.endsWith("Quota exceeded"))
        assertEquals("HTTP 418: çaydanlık", Protokol.hataMesaji(418, "çaydanlık"))
        assertEquals("Sağlayıcı tarafında hata (503)", Protokol.hataMesaji(503, ""))
    }

    @Test
    fun `openai model listesi ayiklanir`() {
        val govde = """{"object":"list","data":[
            {"id":"models/gemini-2.5-flash"},{"id":"text-embedding-004"},
            {"id":"llama-3.3-70b-versatile"},{"id":"whisper-large-v3"},{"id":"llama-3.3-70b-versatile"}]}"""
        assertEquals(
            listOf("gemini-2.5-flash", "llama-3.3-70b-versatile"),
            Protokol.modelListesiCoz(govde, ""),
        )
    }

    @Test
    fun `sonek filtresi ve duz dizi`() {
        val openRouter = """{"data":[{"id":"a/b:free"},{"id":"a/c"},{"id":"x/y:free"}]}"""
        assertEquals(listOf("a/b:free", "x/y:free"), Protokol.modelListesiCoz(openRouter, ":free"))
        val github = """[{"id":"openai/gpt-4.1","name":"OpenAI GPT-4.1"},{"id":"meta/Llama-3.3-70B-Instruct"}]"""
        assertEquals(listOf("meta/Llama-3.3-70B-Instruct", "openai/gpt-4.1"), Protokol.modelListesiCoz(github, ""))
    }

    @Test
    fun `adres birlestirme`() {
        assertEquals("https://x/v1/models", Istemci.adres(" https://x/v1/ ", "models"))
        assertEquals("https://x/v1/chat/completions", Istemci.adres("https://x/v1", "chat/completions"))
    }
}
