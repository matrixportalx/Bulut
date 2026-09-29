package tr.bulut.ag

/**
 * Sohbette görsel üretiminin iki tetikleyicisi (Ruya'daki gibi):
 * elle `/görsel <tarif>` komutu ya da modelin yanıt sonuna eklediği
 * `IMAGE_PROMPT: ...` satırı.
 */
object GorselKomutu {

    private val onekler = listOf("/görsel", "/gorsel", "/resim", "/image", "/çiz", "/ciz")
    private val satir = Regex("""^\s*\**IMAGE_PROMPT\**\s*:\s*(.*)$""", RegexOption.IGNORE_CASE)

    /** Komutsa tarifi, değilse null döndürür. Tarifi boş komut "" döner. */
    fun komut(metin: String): String? {
        val t = metin.trim()
        val onek = onekler.firstOrNull { t.equals(it, true) || t.startsWith("$it ", true) || t.startsWith("$it\n", true) }
            ?: return null
        return t.substring(onek.length).trim()
    }

    /** Yanıttan IMAGE_PROMPT satırını ayırır: (gösterilecek metin, tarif ya da null). */
    fun ayikla(yanit: String): Pair<String, String?> {
        var tarif: String? = null
        val kalan = yanit.lines().filter { s ->
            val m = satir.matchEntire(s) ?: return@filter true
            tarif = m.groupValues[1].trim().trim('*', '`', '"').trim().ifBlank { null }
            false
        }
        return kalan.joinToString("\n").trimEnd() to tarif
    }

    /** Akış sürerken yarım yazılmış `IMAGE_PR…` satırı da görünmesin. */
    fun gizle(yanit: String): String {
        val govde = ayikla(yanit).first
        val son = govde.substringAfterLast('\n').trim().trimStart('*').uppercase()
        return if (son.length >= 3 && "IMAGE_PROMPT:".startsWith(son)) {
            govde.substringBeforeLast('\n', "").trimEnd()
        } else govde
    }

    fun sistemEki(): String = "\n\n" +
        "Görsel oluşturma yeteneğin var. Kullanıcı açıkça bir resim, görsel, çizim ya da " +
        "fotoğraf oluşturmanı isterse normal yanıtını yaz ve EN SONA yeni bir satırda tam " +
        "olarak şu biçimde bir satır ekle:\n" +
        "IMAGE_PROMPT: <İngilizce, ayrıntılı sahne tarifi, en fazla 60 kelime>\n" +
        "Kullanıcı görsel istemediyse bu satırı asla yazma. Görseli sen üretmiyorsun; " +
        "satırı yazman yeterli, uygulama görseli oluşturup yanıtının altına ekler."

    const val GELISTIRME_ISTEMI =
        "You turn a user's image idea (any language) into one prompt for a text-to-image " +
            "model. Write it in English, 30-70 words: subject, setting, composition, lighting, " +
            "style, mood. Keep every concrete detail the user gave. Output only the prompt, " +
            "no quotes, no explanation."
}
