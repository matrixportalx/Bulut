package tr.bulut.veri

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API anahtarlarını Android anahtar deposundaki bir AES anahtarıyla şifreleyip
 * saklar. Şifreleme anahtarı cihazdan çıkamaz; kayıt kopyalansa da çözülemez.
 */
class AnahtarKasasi(context: Context) {

    private val tercihler = context.getSharedPreferences("anahtarlar", Context.MODE_PRIVATE)

    fun oku(saglayiciId: String): String {
        val kayit = tercihler.getString(saglayiciId, null) ?: return ""
        return runCatching {
            val bayt = Base64.decode(kayit, Base64.NO_WRAP)
            val iv = bayt.copyOfRange(0, IV_BOYU)
            val sifreli = bayt.copyOfRange(IV_BOYU, bayt.size)
            val sifre = Cipher.getInstance(DONUSUM)
            sifre.init(Cipher.DECRYPT_MODE, anahtar(), GCMParameterSpec(128, iv))
            String(sifre.doFinal(sifreli), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun yaz(saglayiciId: String, deger: String) {
        if (deger.isBlank()) {
            tercihler.edit().remove(saglayiciId).apply()
            return
        }
        val sifre = Cipher.getInstance(DONUSUM)
        sifre.init(Cipher.ENCRYPT_MODE, anahtar())
        val kayit = sifre.iv + sifre.doFinal(deger.trim().toByteArray(Charsets.UTF_8))
        tercihler.edit().putString(saglayiciId, Base64.encodeToString(kayit, Base64.NO_WRAP)).apply()
    }

    fun varMi(saglayiciId: String): Boolean = tercihler.contains(saglayiciId)

    private fun anahtar(): SecretKey {
        val depo = KeyStore.getInstance(DEPO).apply { load(null) }
        (depo.getEntry(TAKMA_AD, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val uretec = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, DEPO)
        uretec.init(
            KeyGenParameterSpec.Builder(TAKMA_AD, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return uretec.generateKey()
    }

    private companion object {
        const val DEPO = "AndroidKeyStore"
        const val TAKMA_AD = "bulut_api_anahtarlari"
        const val DONUSUM = "AES/GCM/NoPadding"
        const val IV_BOYU = 12
    }
}
