package tr.bulut.veri

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HazirSaglayicilarTest {

    @Test
    fun `birlestirme cekilen modelleri ve ozel adresi korur`() {
        val kayitli = listOf(
            HazirSaglayicilar.groq.copy(ad = "Eski ad", modeller = listOf("m1")),
            HazirSaglayicilar.ozelSunucu.copy(tabanUrl = "http://10.0.0.2:8080/v1", etkin = true),
        )
        val sonuc = HazirSaglayicilar.birlestir(kayitli)
        assertEquals(HazirSaglayicilar.hepsi.map { it.id }, sonuc.map { it.id })
        val groq = sonuc.first { it.id == "groq" }
        assertEquals("Groq", groq.ad)
        assertEquals(listOf("m1"), groq.gorunenModeller)
        val ozel = sonuc.first { it.id == "ozel" }
        assertEquals("http://10.0.0.2:8080/v1", ozel.tabanUrl)
        assertTrue(ozel.etkin)
    }

    @Test
    fun `anahtarsiz sunucu kaydedilmeden kullanilamaz`() {
        assertFalse(HazirSaglayicilar.ozelSunucu.kullanilabilir(emptySet()))
        assertTrue(HazirSaglayicilar.ozelSunucu.copy(etkin = true).kullanilabilir(emptySet()))
        assertFalse(HazirSaglayicilar.gemini.kullanilabilir(emptySet()))
        assertTrue(HazirSaglayicilar.gemini.kullanilabilir(setOf("gemini")))
    }

    @Test
    fun `ayarlar ve sohbet serilestirilip geri okunur`() {
        val a = Ayarlar(sonSaglayici = "gemini", sicaklik = 0.3f)
        assertEquals(a, Depo.json.decodeFromString<Ayarlar>(Depo.json.encodeToString(a)))
        val sohbet = Sohbet("1", "b", "groq", "m", listOf(Mesaj(Rol.KULLANICI, "selam", zaman = 5)))
        assertEquals(sohbet, Depo.json.decodeFromString<Sohbet>(Depo.json.encodeToString(sohbet)))
    }
}
