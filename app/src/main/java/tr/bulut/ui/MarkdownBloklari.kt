package tr.bulut.ui

/**
 * Model yanıtlarındaki markdown'ın blok düzeyi. Tam bir ayrıştırıcı değil;
 * sohbette gerçekten görülen biçimleri (kod bloğu, başlık, liste, alıntı) ayırır.
 * Satır içi biçim (kalın, eğik, `kod`) çizim sırasında işlenir.
 */
sealed interface Blok {
    data class Paragraf(val metin: String) : Blok
    data class Baslik(val duzey: Int, val metin: String) : Blok
    data class Madde(val isaret: String, val metin: String, val girinti: Int) : Blok
    data class Alinti(val metin: String) : Blok
    data class Kod(val dil: String, val metin: String) : Blok
    data object Cizgi : Blok
}

object MarkdownBloklari {

    private val baslik = Regex("^(#{1,6})\\s+(.*)$")
    private val madde = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$")
    private val cizgi = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

    fun ayir(metin: String): List<Blok> {
        val bloklar = mutableListOf<Blok>()
        val paragraf = StringBuilder()
        fun paragrafiKapat() {
            if (paragraf.isNotBlank()) bloklar += Blok.Paragraf(paragraf.toString().trimEnd())
            paragraf.clear()
        }

        val satirlar = metin.lines()
        var i = 0
        while (i < satirlar.size) {
            val satir = satirlar[i]
            val kirpik = satir.trimStart()
            when {
                kirpik.startsWith("```") -> {
                    paragrafiKapat()
                    val dil = kirpik.removePrefix("```").trim()
                    val kod = StringBuilder()
                    i++
                    // Kapanmamış blok (akış sürerken) sonuna kadar kod sayılır.
                    while (i < satirlar.size && !satirlar[i].trimStart().startsWith("```")) {
                        if (kod.isNotEmpty()) kod.append('\n')
                        kod.append(satirlar[i])
                        i++
                    }
                    bloklar += Blok.Kod(dil, kod.toString())
                }
                satir.isBlank() -> paragrafiKapat()
                cizgi.matches(satir) -> { paragrafiKapat(); bloklar += Blok.Cizgi }
                baslik.matchEntire(kirpik) != null -> {
                    paragrafiKapat()
                    val m = baslik.matchEntire(kirpik)!!
                    bloklar += Blok.Baslik(m.groupValues[1].length, m.groupValues[2].trimEnd('#', ' '))
                }
                madde.matchEntire(satir) != null -> {
                    paragrafiKapat()
                    val m = madde.matchEntire(satir)!!
                    val isaret = m.groupValues[2].let { if (it[0].isDigit()) it.trimEnd(')').let { n -> if (n.endsWith('.')) n else "$n." } else "•" }
                    bloklar += Blok.Madde(isaret, m.groupValues[3], m.groupValues[1].replace("\t", "  ").length / 2)
                }
                kirpik.startsWith(">") -> {
                    paragrafiKapat()
                    val alinti = StringBuilder()
                    while (i < satirlar.size && satirlar[i].trimStart().startsWith(">")) {
                        if (alinti.isNotEmpty()) alinti.append('\n')
                        alinti.append(satirlar[i].trimStart().removePrefix(">").removePrefix(" "))
                        i++
                    }
                    bloklar += Blok.Alinti(alinti.toString())
                    continue
                }
                else -> {
                    if (paragraf.isNotEmpty()) paragraf.append('\n')
                    paragraf.append(satir)
                }
            }
            i++
        }
        paragrafiKapat()
        return bloklar
    }
}
