package shopunilever.dominik.pl.ui.pracownik

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun wygenerujBitmapQr(
    tresc: String,
    rozmiarPx: Int = 512
): Bitmap? {
    if (tresc.isBlank()) return null
    return runCatching {
        val wskazowki = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val macierz = QRCodeWriter().encode(
            tresc,
            BarcodeFormat.QR_CODE,
            rozmiarPx,
            rozmiarPx,
            wskazowki
        )
        val szer = macierz.width
        val wys = macierz.height
        val piksele = IntArray(szer * wys)
        for (y in 0 until wys) {
            val offset = y * szer
            for (x in 0 until szer) {
                piksele[offset + x] =
                    if (macierz[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        Bitmap.createBitmap(szer, wys, Bitmap.Config.ARGB_8888).also {
            it.setPixels(piksele, 0, szer, 0, 0, szer, wys)
        }
    }.getOrNull()
}

@Composable
fun KodQrParowania(
    tresc: String,
    modifier: Modifier = Modifier,
    rozmiar: Dp = 200.dp
) {
    val bitmap = remember(tresc) { wygenerujBitmapQr(tresc) }
    Box(
        modifier = modifier
            .size(rozmiar)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Kod QR parowania",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(rozmiar - 20.dp)
            )
        }
    }
}
