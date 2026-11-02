package shopunilever.dominik.pl.ui.pracownik

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Własna ikona wyjścia ze sklepu (drzwi + strzałka). */
val IkonaWyjsciaSklepu: ImageVector
    get() {
        if (_ikonaWyjscia != null) return _ikonaWyjscia!!
        _ikonaWyjscia = ImageVector.Builder(
            name = "WyjscieSklepu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // ramka drzwi
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(4f, 4f)
                lineTo(12f, 4f)
                lineTo(12f, 20f)
                lineTo(4f, 20f)
                close()
            }
            // klamka
            path(
                fill = SolidColor(Color.White),
                stroke = null,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(9.8f, 11.2f)
                arcToRelative(1f, 1f, 0f, true, true, 0f, 2f)
                arcToRelative(1f, 1f, 0f, true, true, 0f, -2f)
            }
            // strzałka wyjścia
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 12f)
                lineTo(20f, 12f)
                moveTo(17.2f, 8.8f)
                lineTo(20.4f, 12f)
                lineTo(17.2f, 15.2f)
            }
        }.build()
        return _ikonaWyjscia!!
    }

private var _ikonaWyjscia: ImageVector? = null
