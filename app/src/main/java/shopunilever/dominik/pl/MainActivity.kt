package shopunilever.dominik.pl

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import shopunilever.dominik.pl.ui.IntentyNawigacji
import shopunilever.dominik.pl.ui.NawigacjaAplikacji
import shopunilever.dominik.pl.ui.theme.SklepUnileverTheme
import shopunilever.dominik.pl.update.AktualizacjaAplikacji

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        obsluzIntentParowania(intent)

        setContent {
            SklepUnileverTheme {
                NawigacjaAplikacji()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        obsluzIntentParowania(intent)
    }

    override fun onResume() {
        super.onResume()
        // Każde wejście / powrót do apki — pracownik i rodzina (overlay globalny).
        lifecycleScope.launch {
            AktualizacjaAplikacji.sprawdzPrzyStarcie(this@MainActivity)
        }
    }

    private fun obsluzIntentParowania(intent: Intent?) {
        if (intent?.getBooleanExtra("otworz_urzadzenia", false) == true) {
            IntentyNawigacji.zadajOtworzUrzadzenia()
            intent.removeExtra("otworz_urzadzenia")
        }
    }
}
