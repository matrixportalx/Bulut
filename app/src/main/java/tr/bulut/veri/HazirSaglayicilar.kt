package tr.bulut.veri

/**
 * Ücretsiz katmanı olan sağlayıcılar. Model adları sık değişiyor; buradakiler
 * yalnızca ilk açılış için — "Modelleri getir" güncel listeyi sunucudan alır.
 */
object HazirSaglayicilar {

    val gemini = Saglayici(
        id = "gemini",
        ad = "Google Gemini",
        tabanUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        aciklama = "Google AI Studio anahtarı. Ücretsiz katmanda dakika ve gün başına " +
            "istek sınırı var; bu katmanda gönderilenler Google tarafından ürün " +
            "geliştirmede kullanılabilir.",
        anahtarAdresi = "https://aistudio.google.com/apikey",
        varsayilanModeller = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro"),
    )

    val groq = Saglayici(
        id = "groq",
        ad = "Groq",
        tabanUrl = "https://api.groq.com/openai/v1",
        aciklama = "Açık ağırlıklı modeller (Llama, Qwen, gpt-oss), çok hızlı. " +
            "Ücretsiz planda model başına dakika/gün sınırı var.",
        anahtarAdresi = "https://console.groq.com/keys",
        varsayilanModeller = listOf(
            "llama-3.3-70b-versatile", "openai/gpt-oss-120b", "qwen/qwen3-32b", "llama-3.1-8b-instant",
        ),
    )

    val openRouter = Saglayici(
        id = "openrouter",
        ad = "OpenRouter",
        tabanUrl = "https://openrouter.ai/api/v1",
        aciklama = "Birçok sağlayıcının adı \":free\" ile biten ücretsiz modelleri. " +
            "Günlük istek sınırı düşük; liste yalnızca ücretsiz modellerle getirilir.",
        anahtarAdresi = "https://openrouter.ai/keys",
        modelSoneki = ":free",
        varsayilanModeller = listOf(
            "deepseek/deepseek-chat-v3-0324:free", "meta-llama/llama-3.3-70b-instruct:free",
        ),
    )

    val mistral = Saglayici(
        id = "mistral",
        ad = "Mistral",
        tabanUrl = "https://api.mistral.ai/v1",
        aciklama = "Ücretsiz \"Experiment\" planı telefon doğrulaması ister. Bu planda " +
            "veriler eğitimde kullanılabilir; yönetim panelinden kapatılabiliyor.",
        anahtarAdresi = "https://console.mistral.ai/api-keys",
        varsayilanModeller = listOf("mistral-small-latest", "mistral-medium-latest", "open-mistral-nemo"),
    )

    val cerebras = Saglayici(
        id = "cerebras",
        ad = "Cerebras",
        tabanUrl = "https://api.cerebras.ai/v1",
        aciklama = "Açık ağırlıklı modeller, çok hızlı. Ücretsiz katmanda günlük token sınırı var.",
        anahtarAdresi = "https://cloud.cerebras.ai",
        varsayilanModeller = listOf("llama-3.3-70b", "qwen-3-32b", "gpt-oss-120b"),
    )

    val github = Saglayici(
        id = "github",
        ad = "GitHub Models",
        tabanUrl = "https://models.github.ai/inference",
        aciklama = "GitHub hesabıyla ücretsiz deneme kotası. Anahtar olarak " +
            "\"models:read\" izinli bir kişisel erişim belirteci (PAT) girilir.",
        anahtarAdresi = "https://github.com/settings/personal-access-tokens",
        modelListesiUrl = "https://models.github.ai/catalog/models",
        varsayilanModeller = listOf("openai/gpt-4.1-mini", "openai/gpt-4.1", "meta/Llama-3.3-70B-Instruct"),
    )

    val ozelSunucu = Saglayici(
        id = "ozel",
        ad = "Özel sunucu",
        tabanUrl = "http://192.168.1.10:11434/v1",
        aciklama = "Kendi Ollama, llama-server ya da LM Studio sunucun. Adres " +
            "\"/v1\" ile biter; anahtar gerekmiyorsa boş bırak.",
        anahtarIstege = true,
        ozel = true,
    )

    val hepsi = listOf(gemini, groq, openRouter, mistral, cerebras, github, ozelSunucu)

    /**
     * Kayıtlı listeyi koddaki hazır tanımlarla birleştirir: sonradan eklenen hazır
     * sağlayıcılar görünür, kullanıcının çektiği model listesi ve özel sunucunun
     * adresi korunur.
     */
    fun birlestir(kayitli: List<Saglayici>): List<Saglayici> {
        val kayitliMap = kayitli.associateBy { it.id }
        val hazirlar = hepsi.map { hazir ->
            val eski = kayitliMap[hazir.id] ?: return@map hazir
            if (hazir.ozel) eski.copy(varsayilanModeller = hazir.varsayilanModeller)
            else hazir.copy(modeller = eski.modeller)
        }
        val hazirIdler = hepsi.map { it.id }.toSet()
        return hazirlar + kayitli.filter { it.id !in hazirIdler }
    }
}
