package tr.bulut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.os.Build
import tr.bulut.ui.AyarlarEkrani
import tr.bulut.ui.BulutViewModel
import tr.bulut.ui.CloudflareEkrani
import tr.bulut.ui.GaleriEkrani
import tr.bulut.ui.GorselEkrani
import tr.bulut.ui.StudyoEkrani
import tr.bulut.ui.ListeEkrani
import tr.bulut.ui.SaglayiciEkrani
import tr.bulut.ui.SohbetEkrani

class MainActivity : ComponentActivity() {

    private val vm: BulutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BulutTema {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "liste") {
                    composable("liste") {
                        ListeEkrani(
                            vm = vm,
                            sohbetAc = { nav.navigate("sohbet/$it") },
                            ayarlarAc = { nav.navigate("ayarlar") },
                            studyoAc = { nav.navigate("studyo") },
                            galeriAc = { nav.navigate("galeri") },
                        )
                    }
                    composable("sohbet/{id}", listOf(navArgument("id") { type = NavType.StringType })) { giris ->
                        SohbetEkrani(
                            vm = vm,
                            sohbetId = giris.arguments?.getString("id").orEmpty(),
                            geri = { nav.popBackStack() },
                            ayarlarAc = { nav.navigate("ayarlar") },
                            gorselAc = { nav.navigate("gorsel/$it") },
                        )
                    }
                    composable("ayarlar") {
                        AyarlarEkrani(
                            vm = vm,
                            geri = { nav.popBackStack() },
                            saglayiciAc = { nav.navigate("saglayici/$it") },
                            cloudflareAc = { nav.navigate("cloudflare") },
                        )
                    }
                    composable("cloudflare") {
                        CloudflareEkrani(vm = vm, geri = { nav.popBackStack() })
                    }
                    composable(
                        "studyo?kaynak={kaynak}",
                        listOf(navArgument("kaynak") { type = NavType.StringType; nullable = true; defaultValue = null }),
                    ) { giris ->
                        StudyoEkrani(
                            vm = vm,
                            kaynakId = giris.arguments?.getString("kaynak"),
                            geri = { nav.popBackStack() },
                            galeriAc = { nav.navigate("galeri") },
                            gorselAc = { nav.navigate("gorsel/$it") },
                            cloudflareAc = { nav.navigate("cloudflare") },
                        )
                    }
                    composable("galeri") {
                        GaleriEkrani(
                            vm = vm,
                            geri = { nav.popBackStack() },
                            gorselAc = { nav.navigate("gorsel/$it") },
                            studyoAc = { nav.navigate("studyo") },
                        )
                    }
                    composable("gorsel/{id}", listOf(navArgument("id") { type = NavType.StringType })) { giris ->
                        GorselEkrani(
                            vm = vm,
                            gorselId = giris.arguments?.getString("id").orEmpty(),
                            geri = { nav.popBackStack() },
                            studyodaAc = { nav.navigate("studyo?kaynak=$it") },
                        )
                    }
                    composable("saglayici/{id}", listOf(navArgument("id") { type = NavType.StringType })) { giris ->
                        SaglayiciEkrani(
                            vm = vm,
                            saglayiciId = giris.arguments?.getString("id").orEmpty(),
                            geri = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulutTema(icerik: @Composable () -> Unit) {
    val koyu = isSystemInDarkTheme()
    val renkler = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (koyu) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        koyu -> darkColorScheme(primary = Color(0xFFB9C3FF), secondary = Color(0xFFC3C5DD))
        else -> lightColorScheme(primary = Color(0xFF3F4E9C), secondary = Color(0xFF5A5D72))
    }
    MaterialTheme(colorScheme = renkler, content = icerik)
}
