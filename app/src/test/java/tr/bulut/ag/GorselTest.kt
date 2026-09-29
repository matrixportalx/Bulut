package tr.bulut.ag

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64
import tr.bulut.veri.GorselModelleri

class GorselTest {

    private fun istek(model: tr.bulut.veri.GorselModel, tohum: Long? = 42) =
        CloudflareProtokol.Istek(model, "a cat", "blurry", 4, 768, 1344, tohum)

    @Test
    fun `flux yalnizca istem ve adim alir`() {
        val o = Json.parseToJsonElement(CloudflareProtokol.govde(istek(GorselModelleri.fluxSchnell))).jsonObject
        assertEquals(setOf("prompt", "steps"), o.keys)
        assertEquals("4", o["steps"]!!.jsonPrimitive.content)
    }

    @Test
    fun `sdxl boyut negatif ve tohum alir`() {
        val o = Json.parseToJsonElement(CloudflareProtokol.govde(istek(GorselModelleri.sdxlLightning))).jsonObject
        assertEquals(setOf("prompt", "num_steps", "width", "height", "negative_prompt", "seed"), o.keys)
        assertEquals("1344", o["height"]!!.jsonPrimitive.content)
        val tohumsuz = Json.parseToJsonElement(CloudflareProtokol.govde(istek(GorselModelleri.sdxlLightning, null))).jsonObject
        assertFalse("seed" in tohumsuz)
    }

    @Test
    fun `flux json yaniti ve sd png yaniti cozulur`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
        val b64 = Base64.getEncoder().encodeToString(jpeg)
        val json = """{"result":{"image":"$b64"},"success":true,"errors":[],"messages":[]}"""
        assertArrayEquals(jpeg, CloudflareProtokol.yanitCoz("application/json", json.toByteArray()))
        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0)
        assertArrayEquals(png, CloudflareProtokol.yanitCoz(null, png))
        assertEquals("png", CloudflareProtokol.uzanti(png))
        assertEquals("jpg", CloudflareProtokol.uzanti(jpeg))
    }

    @Test
    fun `cloudflare hatasi okunur`() {
        val govde = """{"success":false,"errors":[{"code":10000,"message":"Authentication error"}],"result":null}"""
        assertEquals(
            "Hesap kimliği ya da API belirteci geçersiz (403). Belirtecin Workers AI izni olmalı: Authentication error",
            CloudflareProtokol.hataMesaji(403, govde),
        )
        assertEquals(3, CloudflareProtokol.modelSayisi("""{"success":true,"result":[{},{},{}]}"""))
    }

    @Test
    fun `neuron tahmini`() {
        assertEquals(57, GorselModelleri.fluxSchnell.tahminiNeuron(1024, 1024, 4))
        assertNull(GorselModelleri.sdxlLightning.tahminiNeuron(1024, 1024, 8))
    }

    @Test
    fun `gorsel komutu`() {
        assertEquals("kırmızı bir balon", GorselKomutu.komut("/görsel kırmızı bir balon"))
        assertEquals("a cat", GorselKomutu.komut("  /IMAGE a cat "))
        assertEquals("", GorselKomutu.komut("/resim"))
        assertNull(GorselKomutu.komut("/görseller nerede"))
        assertNull(GorselKomutu.komut("bir görsel çiz"))
    }

    @Test
    fun `model satiri ayiklanir`() {
        val (metin, tarif) = GorselKomutu.ayikla("İşte çizdim!\n\n**IMAGE_PROMPT:** a red balloon over Istanbul\n")
        assertEquals("İşte çizdim!", metin)
        assertEquals("a red balloon over Istanbul", tarif)
        assertEquals("Selam" to null, GorselKomutu.ayikla("Selam"))
    }

    @Test
    fun `akis sirasinda yarim satir gizlenir`() {
        assertEquals("Tamam", GorselKomutu.gizle("Tamam\nIMAGE_PR"))
        assertEquals("Tamam", GorselKomutu.gizle("Tamam\nIMAGE_PROMPT: a ca"))
        // Üç harften sonra satır IMAGE_PROMPT'un başı gibi görünüyorsa geçici olarak gizlenir.
        assertEquals("Tamam", GorselKomutu.gizle("Tamam\nIma"))
        assertEquals("Imagine", GorselKomutu.gizle("Imagine"))
        assertEquals("Tamam\nİyi", GorselKomutu.gizle("Tamam\nİyi"))
    }
}
