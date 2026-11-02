package shopunilever.dominik.pl.firebase

/**
 * Stałe Firebase projektu sklepunilever-app (własny projekt aplikacji Sklep Unilever).
 */
object StaleFirebase {
    const val NAZWA_APKI = "sklepUnilever"

    const val API_KEY_ANDROID = "AIzaSyDh3LsPO5qiGPbkxRuXRe43TcZzgBqVUK8"
    const val APP_ID_ANDROID = "1:10704936907:android:101d45c96a9c9df8cee04e"
    const val PROJECT_ID = "sklepunilever-app"
    const val AUTH_DOMAIN = "sklepunilever-app.firebaseapp.com"
    const val STORAGE_BUCKET = "sklepunilever-app.firebasestorage.app"
    const val DATABASE_URL =
        "https://sklepunilever-app-default-rtdb.europe-west1.firebasedatabase.app"
    const val MESSAGING_SENDER_ID = "10704936907"

    const val DLUGOSC_KODU = 8
    const val PREFIX_QR = "SKLEP-UNILEVER:"
    const val ALFABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    object Sciezki {
        const val TRANSMITTERS = "transmitters"
        const val PAIRING_CODES = "pairing_codes"
        const val RECEIVER_SESSIONS = "receiver_sessions"
        const val ANNOUNCEMENTS = "announcements"
    }
}
