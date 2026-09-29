package tr.bulut.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownBloklariTest {

    @Test
    fun `karisik belge`() {
        val metin = """
            # Başlık
            İlk satır
            devamı

            - bir
              - iki
            1. sayılı
            > alıntı
            > sürüyor
            ---
            ```kotlin
            val a = 1

            val b = 2
            ```
            son
        """.trimIndent()
        assertEquals(
            listOf(
                Blok.Baslik(1, "Başlık"),
                Blok.Paragraf("İlk satır\ndevamı"),
                Blok.Madde("•", "bir", 0),
                Blok.Madde("•", "iki", 1),
                Blok.Madde("1.", "sayılı", 0),
                Blok.Alinti("alıntı\nsürüyor"),
                Blok.Cizgi,
                Blok.Kod("kotlin", "val a = 1\n\nval b = 2"),
                Blok.Paragraf("son"),
            ),
            MarkdownBloklari.ayir(metin),
        )
    }

    @Test
    fun `akis sirasinda kapanmamis kod blogu`() {
        assertEquals(
            listOf(Blok.Paragraf("Şöyle:"), Blok.Kod("", "print(1)")),
            MarkdownBloklari.ayir("Şöyle:\n```\nprint(1)"),
        )
    }

    @Test
    fun `kalin satir madde sanilmaz`() {
        assertEquals(listOf(Blok.Paragraf("**Not:** dikkat")), MarkdownBloklari.ayir("**Not:** dikkat"))
    }
}
