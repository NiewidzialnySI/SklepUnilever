package shopunilever.dominik.pl.firebase

/**
 * Stałe Firebase sklepik-unilever — te same co Harmonogram / Sklepik APK.
 */
object StaleFirebase {
    const val NAZWA_APKI = "sklepik"

    const val API_KEY_ANDROID = "AIzaSyAti-875aeT2Byc_TBBXcaX_coglyTeHmo"
    const val APP_ID_ANDROID = "1:1008882819160:android:5dd9b9cfdc3fe7731e2a2d"
    const val PROJECT_ID = "sklepik-unilever"
    const val AUTH_DOMAIN = "sklepik-unilever.firebaseapp.com"
    const val STORAGE_BUCKET = "sklepik-unilever.firebasestorage.app"
    const val DATABASE_URL =
        "https://sklepik-unilever-default-rtdb.europe-west1.firebasedatabase.app"
    const val MESSAGING_SENDER_ID = "1008882819160"

    const val DLUGOSC_KODU = 8
    const val PREFIX_QR = "SKLEPIK-UNILEVER:"
    const val ALFABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    object Sciezki {
        const val TRANSMITTERS = "transmitters"
        const val PAIRING_CODES = "pairing_codes"
        const val RECEIVER_SESSIONS = "receiver_sessions"
        const val ANNOUNCEMENTS = "announcements"
    }
}
