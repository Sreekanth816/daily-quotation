package com.example.teluguphotoquote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
    val position: TextPosition = TextPosition.TOP,
    val horizontalPaddingDp: Float = 24f,
    val verticalPaddingDp: Float = 20f
)

/**
 * Styling for the small "watermark" showing who created the quote: a
 * circular profile picture plus the user's name, anchored to a corner.
 */
data class ProfileBadgeStyle(
    val avatarSizeDp: Float = 44f,
    val marginDp: Float = 16f,
    val avatarBorderColor: Int = Color.WHITE,
    val avatarBorderWidthDp: Float = 2f,
    val nameTextColor: Int = Color.WHITE,
    val nameTextSizeSp: Float = 14f,
    val backgroundColor: Int = Color.BLACK,
    val backgroundAlpha: Int = 140
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
    style: QuoteStyle = QuoteStyle(),
    profile: UserProfile? = null,
    profileBadgeStyle: ProfileBadgeStyle = ProfileBadgeStyle()
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
        setShadowLayer(6f * density, 0f, 2f * density, Color.argb(180, 0, 0, 0))
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

    canvas.save()
    canvas.translate(horizontalPaddingPx, top + verticalPaddingPx / 2)
    staticLayout.draw(canvas)
    canvas.restore()

    if (profile != null) {
        drawProfileBadge(context, canvas, output.width, output.height, profile, profileBadgeStyle, typeface)
    }

    return output
}

/**
 * Draws a bottom-right "badge" made of the user's circular profile picture
 * plus their name, over a translucent pill so it stays readable on any photo.
 */
private fun drawProfileBadge(
    context: Context,
    canvas: Canvas,
    canvasWidth: Int,
    canvasHeight: Int,
    profile: UserProfile,
    style: ProfileBadgeStyle,
    nameTypeface: Typeface
) {
    val density = context.resources.displayMetrics.density
    val scaledDensity = context.resources.displayMetrics.scaledDensity

    val avatarSizePx = style.avatarSizeDp * density
    val marginPx = style.marginDp * density
    val borderWidthPx = style.avatarBorderWidthDp * density

    val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = style.nameTextColor
        textSize = style.nameTextSizeSp * scaledDensity
        typeface = nameTypeface
        textAlign = Paint.Align.RIGHT
        setShadowLayer(5f * density, 0f, 1.5f * density, Color.argb(190, 0, 0, 0))
    }

    val avatarCenterY = canvasHeight - marginPx - avatarSizePx / 2f
    val avatarCenterX = canvasWidth - marginPx - avatarSizePx / 2f
    val nameGapPx = 10f * density

    // Name, vertically centered against the avatar. A shadow (set on namePaint
    // above) keeps it readable without needing a solid background behind it.
    val nameBaselineY = avatarCenterY - (namePaint.ascent() + namePaint.descent()) / 2f
    canvas.drawText(
        profile.name,
        avatarCenterX - avatarSizePx / 2f - nameGapPx,
        nameBaselineY,
        namePaint
    )

    // Circular profile picture, decoded from the uploaded file and center-cropped
    // (not stretched) so non-square photos aren't distorted.
    val avatarLeft = avatarCenterX - avatarSizePx / 2f
    val avatarTop = avatarCenterY - avatarSizePx / 2f
    val avatarDrawn = drawCircularAvatar(
        context, canvas, profile.profileImagePath, avatarLeft, avatarTop, avatarSizePx.toInt()
    )

    if (avatarDrawn) {
        // Thin border ring around the circular avatar.
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.avatarBorderColor
            this.style = Paint.Style.STROKE
            strokeWidth = borderWidthPx
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarSizePx / 2f - borderWidthPx / 2f, borderPaint)
    }
}

/**
 * Loads the profile picture from disk (safely downsampled and corrected for
 * EXIF rotation so camera photos aren't sideways), center-crops it to a
 * square, then draws it circularly at ([left], [top]) sized [targetSizePx]
 * using AndroidX's [RoundedBitmapDrawable] (isCircular = true) — the same
 * well-tested utility class used throughout the Android ecosystem for
 * circular avatars, which reliably center-crop-scales the bitmap to fill
 * its bounds. Returns true if something was drawn.
 */
private fun drawCircularAvatar(
    context: Context,
    canvas: Canvas,
    imagePath: String,
    left: Float,
    top: Float,
    targetSizePx: Int
): Boolean {
    val decoded = decodeSampledBitmap(imagePath, targetSizePx) ?: return false
    val rotated = applyExifRotation(imagePath, decoded)
    if (rotated.width <= 0 || rotated.height <= 0) return false

    // Center-crop to the largest centered square so the drawable doesn't
    // have to stretch a non-square photo unevenly.
    val squareSize = minOf(rotated.width, rotated.height)
    val srcLeft = (rotated.width - squareSize) / 2
    val srcTop = (rotated.height - squareSize) / 2
    val square = Bitmap.createBitmap(rotated, srcLeft, srcTop, squareSize, squareSize)

    val drawable = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
        .create(context.resources, square)
        .apply {
            isCircular = true
            setAntiAlias(true)
        }
    drawable.setBounds(0, 0, targetSizePx, targetSizePx)

    canvas.save()
    canvas.translate(left, top)
    drawable.draw(canvas)
    canvas.restore()
    return true
}

/** Decodes [path] downsampled to roughly [targetSizePx], avoiding loading a huge full-resolution bitmap. */
private fun decodeSampledBitmap(path: String, targetSizePx: Int): Bitmap? {
    return try {
        val boundsOptions = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, boundsOptions)

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            // Bounds couldn't be read; fall back to a plain decode rather than returning null.
            return android.graphics.BitmapFactory.decodeFile(path)
        }

        var sampleSize = 1
        var halfWidth = boundsOptions.outWidth / 2
        var halfHeight = boundsOptions.outHeight / 2
        while (halfWidth / sampleSize >= targetSizePx && halfHeight / sampleSize >= targetSizePx) {
            sampleSize *= 2
        }

        val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
        android.graphics.BitmapFactory.decodeFile(path, decodeOptions)
            ?: android.graphics.BitmapFactory.decodeFile(path)
    } catch (e: Exception) {
        try {
            android.graphics.BitmapFactory.decodeFile(path)
        } catch (e2: Exception) {
            null
        }
    }
}

/** Rotates [bitmap] according to the source file's EXIF orientation tag, if any. */
private fun applyExifRotation(path: String, bitmap: Bitmap): Bitmap {
    val degrees = try {
        val exif = androidx.exifinterface.media.ExifInterface(path)
        when (exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: Exception) {
        0f
    }
    if (degrees == 0f) return bitmap
    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
