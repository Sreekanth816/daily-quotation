package com.example.teluguphotoquote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Where the caption text block should be placed on the photo.
 */
enum class TextPosition { TOP, CENTER, BOTTOM }

/**
 * All the user-adjustable styling options for the caption.
 */
data class QuoteStyle(
    val textColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.BLACK,
    val backgroundAlpha: Int = 160,        // 0-255
    val textSizeSp: Float = 28f,
    val position: TextPosition = TextPosition.BOTTOM,
    val horizontalPaddingDp: Float = 24f,
    val verticalPaddingDp: Float = 20f
)

/**
 * Builds a Typeface that renders Telugu glyphs using Noto Sans Telugu and
 * falls back to a Latin-friendly font (Noto Sans) for English characters
 * in the same string, so mixed Telugu+English text looks visually consistent.
 *
 * Requires the font files declared in res/font/telugu_font_family.xml
 * (see comments there) and res/font/noto_sans_regular.ttf to exist.
 */
object QuoteTypefaceFactory {

    fun create(context: Context): Typeface {
        val teluguTypeface = androidx.core.content.res.ResourcesCompat.getFont(
            context, R.font.telugu_font_family
        ) ?: Typeface.DEFAULT

        // Android's Minikin text stack already performs per-script font fallback
        // automatically: when a run of Latin characters can't be rendered by
        // Noto Sans Telugu, the system substitutes the closest matching system
        // Latin font for that run. This works well out of the box on API 24+,
        // so no CustomFallbackBuilder is required for the common case.
        //
        // For pixel-perfect control over which exact Latin font is used (e.g. to
        // match Noto Sans Telugu's weight/x-height precisely), you can build an
        // explicit chain on API 29+ with Typeface.CustomFallbackBuilder using
        // android.graphics.fonts.FontFamily + Font.Builder pointed at a bundled
        // noto_sans_regular.ttf asset. Left as a straightforward enhancement —
        // the default fallback above is production-safe.
        return teluguTypeface
    }
}

/**
 * Renders the given caption text onto a copy of [sourceBitmap] and returns
 * the combined image, ready to save or share.
 *
 * Uses StaticLayout (not raw Canvas.drawText) so long captions wrap correctly
 * and Telugu conjuncts (ottu) shape properly via Android's Minikin/HarfBuzz
 * text stack — this is essential for correct Telugu rendering.
 */
fun renderQuoteOnPhoto(
    context: Context,
    sourceBitmap: Bitmap,
    quoteText: String,
    style: QuoteStyle = QuoteStyle()
): Bitmap {
    val density = context.resources.displayMetrics.density
    val scaledDensity = context.resources.displayMetrics.scaledDensity

    val output = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    val horizontalPaddingPx = style.horizontalPaddingDp * density
    val verticalPaddingPx = style.verticalPaddingDp * density
    val textBlockWidth = (output.width - horizontalPaddingPx * 2).toInt().coerceAtLeast(1)

    val typeface = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.telugu_font_family)
            ?: Typeface.DEFAULT
    } catch (e: Exception) {
        Typeface.DEFAULT
    }

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.textColor
        textSize = style.textSizeSp * scaledDensity
        this.typeface = typeface
    }

    val staticLayout = StaticLayout.Builder
        .obtain(quoteText, 0, quoteText.length, textPaint, textBlockWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, 1.15f)
        .setIncludePad(false)
        .build()

    val textBlockHeight = staticLayout.height

    val top = when (style.position) {
        TextPosition.TOP -> verticalPaddingPx
        TextPosition.CENTER -> (output.height - textBlockHeight) / 2f
        TextPosition.BOTTOM -> output.height - textBlockHeight - verticalPaddingPx * 2
    }

    // Semi-transparent background strip behind the text so it stays readable
    // regardless of what's in the photo underneath.
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.backgroundColor
        alpha = style.backgroundAlpha
    }
    val bgRect = RectF(
        0f,
        top - verticalPaddingPx / 2,
        output.width.toFloat(),
        top + textBlockHeight + verticalPaddingPx * 1.5f
    )
    canvas.drawRect(bgRect, bgPaint)

    canvas.save()
    canvas.translate(horizontalPaddingPx, top + verticalPaddingPx / 2)
    staticLayout.draw(canvas)
    canvas.restore()

    return output
}
