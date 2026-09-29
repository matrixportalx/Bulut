package tr.bulut.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tr.bulut.ag.Istemci
import tr.bulut.ag.Protokol
import tr.bulut.veri.AnahtarKasasi
import tr.bulut.veri.Ayarlar
import tr.bulut.veri.Depo
import tr.bulut.veri.Mesaj
import tr.bulut.veri.Rol
import tr.bulut.veri.Saglayici
import tr.bulut.veri.Sohbet
import java.util.UUID

/**
 * Tek ViewModel: üretim burada yaşadığı için ekran döndürme ya da ekranlar
 * arası geçiş yanıtı kesmez.
 */
class BulutViewModel(uygulama: Application) : AndroidViewModel(uygulama) {

    private val depo = Depo(uygulama)
    private val kasa = AnahtarKasasi(uygulama)
    private val istemci = Istemci()

    private val _ayarlar = MutableStateFlow(Ayarlar())
    val ayarlar: StateFlow<Ayarlar> = _ayarlar.asStateFlow()

    private val _sohbetler = MutableStateFlow<List<Sohbet>>(emptyList())
    val sohbetler: StateFlow<List<Sohbet>> = _sohbetler.asStateFlow()

    /** Yanıtı hâlâ akmakta olan sohbetlerin kimlikleri. */
    private val _uretenler = MutableStateFlow<Set<String>>(emptySet())
    val uretenler: StateFlow<Set<String>> = _uretenler.asStateFlow()

    /** Anahtarı kayıtlı sağlayıcılar; anahtarın kendisi arayüze çıkmaz. */
    private val _anahtarlilar = MutableStateFlow<Set<String>>(emptySet())
    val anahtarlilar: StateFlow<Set<String>> = _anahtarlilar.asStateFlow()

    private val isler = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _ayarlar.value = depo.ayarlariOku()
            _sohbetler.value = depo.sohbetleriOku()
            anahtarlariYenile()
        }
    }

    // ---- Sağlayıcılar ----

    fun kullanilabilirMi(s: Saglayici) = s.kullanilabilir(_anahtarlilar.value)

    fun anahtarOku(id: String): String = kasa.oku(id)

    fun saglayiciKaydet(saglayici: Saglayici, anahtar: String) {
        kasa.yaz(saglayici.id, anahtar)
        anahtarlariYenile()
        saglayiciGuncelle(saglayici)
    }

    /** Anahtara dokunmadan yalnızca tanımı (adres, model listesi) değiştirir. */
    fun saglayiciGuncelle(saglayici: Saglayici) {
        ayarlariDegistir { a ->
            a.copy(saglayicilar = a.saglayicilar.map { if (it.id == saglayici.id) saglayici else it })
        }
    }

    /** Sunucudan model listesini çeker ve kaydeder; hata metnini döndürür. */
    suspend fun modelleriGetir(saglayici: Saglayici, anahtar: String): Result<List<String>> =
        runCatching {
            istemci.modeller(saglayici.tabanUrl, saglayici.modelListesiUrl, anahtar, saglayici.modelSoneki)
        }.onSuccess { liste ->
            if (liste.isNotEmpty()) ayarlariDegistir { a ->
                a.copy(saglayicilar = a.saglayicilar.map { if (it.id == saglayici.id) it.copy(modeller = liste) else it })
            }
        }

    fun ayarlariDegistir(degisim: (Ayarlar) -> Ayarlar) {
        _ayarlar.update(degisim)
        val yeni = _ayarlar.value
        viewModelScope.launch(Dispatchers.IO) { depo.ayarlariYaz(yeni) }
    }

    private fun anahtarlariYenile() {
        _anahtarlilar.value = _ayarlar.value.saglayicilar.map { it.id }.filter { kasa.varMi(it) }.toSet()
    }

    // ---- Sohbetler ----

    /** Yeni sohbeti son kullanılan modelle açar; hiçbir sağlayıcı hazır değilse null. */
    fun yeniSohbet(): String? {
        val a = _ayarlar.value
        val hazirlar = a.saglayicilar.filter { kullanilabilirMi(it) && it.gorunenModeller.isNotEmpty() }
        val saglayici = hazirlar.firstOrNull { it.id == a.sonSaglayici } ?: hazirlar.firstOrNull() ?: return null
        val model = a.sonModel.takeIf { saglayici.id == a.sonSaglayici && it.isNotBlank() }
            ?: saglayici.gorunenModeller.first()
        val sohbet = Sohbet(id = UUID.randomUUID().toString(), baslik = "", saglayiciId = saglayici.id, model = model)
        _sohbetler.update { listOf(sohbet) + it }
        return sohbet.id
    }

    fun sohbet(id: String): Sohbet? = _sohbetler.value.firstOrNull { it.id == id }

    fun modelSec(sohbetId: String, saglayiciId: String, model: String) {
        sohbetGuncelle(sohbetId, kaydet = true) { it.copy(saglayiciId = saglayiciId, model = model) }
        ayarlariDegistir { it.copy(sonSaglayici = saglayiciId, sonModel = model) }
    }

    fun sohbetSil(id: String) {
        durdur(id)
        _sohbetler.update { liste -> liste.filterNot { it.id == id } }
        viewModelScope.launch(Dispatchers.IO) { depo.sohbetSil(id) }
    }

    /** Hiç mesajı olmayan sohbet diske yazılmaz; ekrandan çıkınca listeden de düşer. */
    fun bosSohbetiAt(id: String) {
        if (sohbet(id)?.mesajlar?.isEmpty() == true) _sohbetler.update { l -> l.filterNot { it.id == id } }
    }

    fun gonder(sohbetId: String, metin: String) {
        val temiz = metin.trim()
        if (temiz.isEmpty() || sohbetId in _uretenler.value) return
        sohbetGuncelle(sohbetId, kaydet = true) { s ->
            s.copy(
                baslik = s.baslik.ifBlank { baslikUret(temiz) },
                mesajlar = s.mesajlar + Mesaj(Rol.KULLANICI, temiz),
            )
        }
        uret(sohbetId)
    }

    /** Son asistan yanıtını (ya da hatasını) atıp aynı soruyu yeniden sorar. */
    fun yenidenUret(sohbetId: String) {
        if (sohbetId in _uretenler.value) return
        sohbetGuncelle(sohbetId, kaydet = false) { s ->
            s.copy(mesajlar = s.mesajlar.dropLastWhile { it.rol == Rol.ASISTAN })
        }
        uret(sohbetId)
    }

    /** Kullanıcının bir mesajını düzenleyip ondan sonrasını yeniden üretir. */
    fun duzenleVeGonder(sohbetId: String, indeks: Int, yeniMetin: String) {
        if (sohbetId in _uretenler.value || yeniMetin.isBlank()) return
        sohbetGuncelle(sohbetId, kaydet = false) { s -> s.copy(mesajlar = s.mesajlar.take(indeks)) }
        gonder(sohbetId, yeniMetin)
    }

    fun durdur(sohbetId: String) {
        isler.remove(sohbetId)?.cancel()
    }

    private fun uret(sohbetId: String) {
        val sohbet = sohbet(sohbetId) ?: return
        val a = _ayarlar.value
        val saglayici = a.saglayicilar.firstOrNull { it.id == sohbet.saglayiciId }
        val gecmis = sohbet.mesajlar.filter { it.hata == null && it.icerik.isNotEmpty() }.takeLast(a.gecmisSiniri)
        val istek = buildList {
            if (a.sistemIstemi.isNotBlank()) add(Protokol.IstekMesaji("system", a.sistemIstemi))
            gecmis.forEach { add(Protokol.IstekMesaji(if (it.rol == Rol.KULLANICI) "user" else "assistant", it.icerik)) }
        }
        sohbetGuncelle(sohbetId, kaydet = false) {
            it.copy(mesajlar = it.mesajlar + Mesaj(Rol.ASISTAN, "", model = sohbet.model))
        }
        _uretenler.update { it + sohbetId }

        isler[sohbetId] = viewModelScope.launch {
            val icerik = StringBuilder()
            val dusunce = StringBuilder()
            var hata: String? = null
            try {
                if (saglayici == null) throw IllegalStateException("Sağlayıcı bulunamadı; modeli yeniden seçin.")
                val anahtar = withContext(Dispatchers.IO) { kasa.oku(saglayici.id) }
                if (anahtar.isBlank() && !saglayici.anahtarIstege) {
                    throw IllegalStateException("${saglayici.ad} için API anahtarı girilmemiş.")
                }
                var sonYayin = 0L
                istemci.akis(saglayici.tabanUrl, anahtar, sohbet.model, istek, a.sicaklik) { parca ->
                    icerik.append(parca.icerik)
                    dusunce.append(parca.dusunce)
                    // Her parçada arayüzü yeniden çizmek uzun yanıtta takılma yapıyor.
                    val simdi = System.currentTimeMillis()
                    if (simdi - sonYayin > 50) {
                        sonYayin = simdi
                        sonMesajiYaz(sohbetId, icerik.toString(), dusunce.toString(), null)
                    }
                }
                if (icerik.isEmpty() && dusunce.isEmpty()) hata = "Model boş yanıt döndürdü."
            } catch (e: CancellationException) {
                // Kullanıcı durdurdu: gelen kadarı kalır.
            } catch (e: Exception) {
                hata = e.message ?: e.javaClass.simpleName
            } finally {
                sonMesajiYaz(sohbetId, icerik.toString(), dusunce.toString(), hata)
                // Durdurulup hiç metin gelmemişse boş balon bırakma.
                if (hata == null && icerik.isEmpty() && dusunce.isEmpty()) {
                    sohbetGuncelle(sohbetId, kaydet = false) { it.copy(mesajlar = it.mesajlar.dropLast(1)) }
                }
                sohbet(sohbetId)?.let { s -> withContext(Dispatchers.IO + NonCancellable) { depo.sohbetYaz(s) } }
                isler.remove(sohbetId)
                _uretenler.update { it - sohbetId }
            }
        }
    }

    private fun sonMesajiYaz(sohbetId: String, icerik: String, dusunce: String, hata: String?) {
        sohbetGuncelle(sohbetId, kaydet = false) { s ->
            val son = s.mesajlar.lastOrNull()?.takeIf { it.rol == Rol.ASISTAN } ?: return@sohbetGuncelle s
            s.copy(mesajlar = s.mesajlar.dropLast(1) + son.copy(icerik = icerik, dusunce = dusunce, hata = hata))
        }
    }

    private fun sohbetGuncelle(id: String, kaydet: Boolean, degisim: (Sohbet) -> Sohbet) {
        var yeni: Sohbet? = null
        _sohbetler.update { liste ->
            liste.map { if (it.id == id) degisim(it).copy(guncellendi = System.currentTimeMillis()).also { s -> yeni = s } else it }
                .sortedByDescending { it.guncellendi }
        }
        val kayit = yeni
        if (kaydet && kayit != null && kayit.mesajlar.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) { depo.sohbetYaz(kayit) }
        }
    }

    private fun baslikUret(metin: String): String {
        val tek = metin.replace(Regex("\\s+"), " ")
        return if (tek.length <= 40) tek else tek.take(40).trimEnd() + "…"
    }
}
