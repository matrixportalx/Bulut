# Bulut

Ücretsiz katmanı olan bulut yapay zekâ API'leri için Android sohbet uygulaması.
Hepsi OpenAI uyumlu `chat/completions` uç noktası üzerinden konuşuluyor.

| Sağlayıcı | Anahtar | Not |
|---|---|---|
| Google Gemini | aistudio.google.com/apikey | Ücretsiz katmanda veriler ürün geliştirmede kullanılabilir |
| Groq | console.groq.com/keys | Açık ağırlıklı modeller, çok hızlı |
| OpenRouter | openrouter.ai/keys | Yalnızca `:free` modeller listelenir |
| Mistral | console.mistral.ai/api-keys | "Experiment" planı, telefon doğrulaması |
| Cerebras | cloud.cerebras.ai | Günlük token sınırı |
| GitHub Models | github.com/settings/personal-access-tokens | `models:read` izinli PAT |
| Özel sunucu | — | Ollama / llama-server / LM Studio, anahtarsız da olur |

- Yanıtlar akış olarak gelir; durdurma, yeniden üretme, mesaj düzenleme var.
- Akıl yürüten modellerin düşünce metni ayrı ve katlanmış gösterilir.
- API anahtarları Android anahtar deposuyla şifrelenir; sohbetler cihazda JSON
  olarak tutulur ve yedeklemeye girmez.

## Görsel üretimi (Cloudflare Workers AI)

Ücretsiz planda günde 10.000 neuron var. FLUX.1 schnell ile 4 adımlı 1024×1024 görsel
yaklaşık 58 neuron (günde ~170 görsel); SDXL Lightning, SDXL 1.0 ve DreamShaper 8 LCM
beta olduğu için neuron harcamıyor.

- **Görsel Stüdyosu**: istem, negatif istem, boyut, adım ve tohum; "İstemi geliştir"
  tarifi sohbet modeline İngilizce ayrıntılı isteme çevirtir.
- **Galeri**: her görsel künyesiyle (istem, model, boyut, tohum) cihazda saklanır;
  paylaşma, Resimler/Bulut'a kaydetme, istemi stüdyoda yeniden kullanma.
- **Sohbette**: `/görsel <tarif>` doğrudan üretir. Ayarlardaki seçenek açıksa model,
  istendiğinde yanıtın sonuna `IMAGE_PROMPT:` satırı yazar ve görsel yanıtın altına eklenir.

Kurulum: Ayarlar → Cloudflare Workers AI → hesap kimliği + "Workers AI" şablonlu API belirteci.

## Derleme

APK yalnızca GitHub Actions'ta üretilir: **Actions → Bulut APK → Run workflow**.
Sürüm `app/build.gradle.kts` içindeki `versionName`'den okunur ve `v<sürüm>`
etiketli bir Release olarak yayımlanır (artefakt kullanılmıyor).

İmza için depo gizlileri: `KEYSTORE_BASE64`, `KEY_ALIAS`, `STORE_PASSWORD`,
`KEY_PASSWORD`. Tanımlı değilse debug imzalı APK çıkar; imzası her derlemede
değiştiğinden üstüne güncelleme yapılamaz.

Yerelde doğrulama: `./gradlew :app:testDebugUnitTest`
