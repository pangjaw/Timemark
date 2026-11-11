package com.sintelis.timemark.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.exifinterface.media.ExifInterface
import com.sintelis.timemark.R
import com.sintelis.timemark.domain.LocationState
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.WatermarkConfig
import java.io.File
import java.time.LocalDateTime
import kotlin.math.min

object WatermarkRenderer {

    /**
     * Loads a captured JPEG file, applies its camera EXIF orientation so pixels are upright,
     * crops to 1:1 if requested, and stamps the Timemark-style overlay at full resolution.
     */
    fun decodeAndStamp(
        context: Context,
        rawFile: File,
        presetDateTime: LocalDateTime,
        location: LocationState,
        config: WatermarkConfig,
        squareCrop: Boolean = false,
        includeTime: Boolean = true
    ): Bitmap {
        val rawBitmap = BitmapFactory.decodeFile(rawFile.absolutePath)
            ?: error("Gagal membaca hasil tangkapan kamera.")

        val exif = ExifInterface(rawFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val upright = rotateBitmapIfNeeded(rawBitmap, orientation)
        if (upright !== rawBitmap) {
            rawBitmap.recycle()
        }

        // Crop before drawing overlay so GPS pill and timestamp remain inside 1:1 edges
        val framed = if (squareCrop && upright.width != upright.height) {
            val size = min(upright.width, upright.height)
            val x = (upright.width - size) / 2
            val y = (upright.height - size) / 2
            val cropped = Bitmap.createBitmap(upright, x, y, size, size)
            upright.recycle()
            cropped
        } else {
            upright
        }

        val mutable = framed.copy(Bitmap.Config.ARGB_8888, true)
        if (mutable !== framed) {
            framed.recycle()
        }
        val regularFace = ResourcesCompat.getFont(context, R.font.roboto_condensed_regular)
            ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        val boldFace = ResourcesCompat.getFont(context, R.font.roboto_condensed_bold)
            ?: Typeface.create("sans-serif-condensed", Typeface.BOLD)

        drawOverlay(
            bitmap = mutable,
            presetDateTime = presetDateTime,
            location = location,
            config = config,
            regularFace = regularFace,
            boldFace = boldFace,
            includeTime = includeTime
        )
        return mutable
    }

    fun drawOverlay(
        bitmap: Bitmap,
        presetDateTime: LocalDateTime,
        location: LocationState,
        config: WatermarkConfig,
        regularFace: Typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL),
        boldFace: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        includeTime: Boolean = true
    ) {
        val canvas = Canvas(bitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val shortEdge = min(w, h)
        val margin = shortEdge * 0.032f

        // 1. Top-left GPS pill
        if (config.showGpsPill) {
            drawGpsPill(
                canvas = canvas,
                text = location.formattedCoordinates(),
                x = margin,
                y = margin,
                shortEdge = shortEdge,
                typeface = regularFace
            )
        }

        // 2. Bottom-left orange accent bar + timestamp pill + 2-line address
        val dateText = if (includeTime) {
            ScheduleCalculator.formatWatermarkDateTime(presetDateTime, config.dateFormatStyle)
        } else {
            ScheduleCalculator.formatWatermarkDateOnly(presetDateTime, config.dateFormatStyle)
        }
        drawBottomLeftBlock(
            canvas = canvas,
            dateText = dateText,
            line1 = location.addressLine1,
            line2 = location.addressLine2,
            margin = margin,
            canvasWidth = w,
            canvasHeight = h,
            shortEdge = shortEdge,
            regularFace = regularFace,
            boldFace = boldFace
        )

        // 3. Bottom-right customizable branding
        if (config.brandTopText.isNotBlank() || config.brandSubText.isNotBlank()) {
            drawBottomRightBrand(
                canvas = canvas,
                topText = config.brandTopText,
                subText = config.brandSubText,
                rightX = w - margin,
                bottomY = h - margin,
                shortEdge = shortEdge,
                boldFace = boldFace
            )
        }
    }

    private fun drawGpsPill(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        shortEdge: Float,
        typeface: Typeface
    ) {
        val textSize = shortEdge * 0.034f
        val padH = shortEdge * 0.024f
        val padV = shortEdge * 0.014f
        val iconSize = textSize * 0.95f
        val gap = shortEdge * 0.012f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            this.typeface = typeface
        }
        val textWidth = textPaint.measureText(text)
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent

        val pillWidth = padH * 2 + iconSize + gap + textWidth
        val pillHeight = padV * 2 + textHeight
        val rect = RectF(x, y, x + pillWidth, y + pillHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(138, 0, 0, 0)
        }
        canvas.drawRoundRect(rect, pillHeight * 0.28f, pillHeight * 0.28f, bgPaint)

        // White pin icon
        val pinCenterX = x + padH + iconSize * 0.5f
        val pinTopY = y + padV + (textHeight - iconSize) * 0.5f
        drawPinIcon(canvas, pinCenterX, pinTopY, iconSize)

        val baseline = y + padV - fm.ascent
        canvas.drawText(text, x + padH + iconSize + gap, baseline, textPaint)
    }

    private fun drawPinIcon(canvas: Canvas, centerX: Float, topY: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val radius = size * 0.36f
        val circleY = topY + radius
        canvas.drawCircle(centerX, circleY, radius, paint)

        val path = Path().apply {
            moveTo(centerX - radius * 0.85f, circleY + radius * 0.35f)
            lineTo(centerX + radius * 0.85f, circleY + radius * 0.35f)
            lineTo(centerX, topY + size)
            close()
        }
        canvas.drawPath(path, paint)

        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 30, 30, 30)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, circleY, radius * 0.4f, holePaint)
    }

    private fun drawBottomLeftBlock(
        canvas: Canvas,
        dateText: String,
        line1: String,
        line2: String,
        margin: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        shortEdge: Float,
        regularFace: Typeface,
        boldFace: Typeface
    ) {
        val dateTextSize = shortEdge * 0.036f
        val addrTextSize = shortEdge * 0.028f
        val barWidth = (shortEdge * 0.0085f).coerceAtLeast(4f)
        val barGap = shortEdge * 0.018f
        val pillPadH = shortEdge * 0.02f
        val pillPadV = shortEdge * 0.01f
        val blockSpacing = shortEdge * 0.014f
        val lineSpacing = shortEdge * 0.008f

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dateTextSize
            typeface = boldFace
        }
        val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = addrTextSize
            typeface = regularFace
            setShadowLayer(shortEdge * 0.008f, 0f, shortEdge * 0.002f, Color.argb(225, 0, 0, 0))
        }

        val dateFm = datePaint.fontMetrics
        val addrFm = addrPaint.fontMetrics
        val datePillH = pillPadV * 2 + (dateFm.descent - dateFm.ascent)
        val addrLineH = addrFm.descent - addrFm.ascent

        val hasLine1 = line1.isNotBlank()
        val hasLine2 = line2.isNotBlank()
        val addrBlockH = when {
            hasLine1 && hasLine2 -> addrLineH * 2 + lineSpacing
            hasLine1 || hasLine2 -> addrLineH
            else -> 0f
        }
        val totalHeight = datePillH + (if (addrBlockH > 0f) blockSpacing + addrBlockH else 0f)
        val blockTop = canvasHeight - margin - totalHeight
        val contentLeft = margin + barWidth + barGap

        // Orange vertical accent bar
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF7A00")
        }
        val barRect = RectF(margin, blockTop, margin + barWidth, canvasHeight - margin)
        canvas.drawRoundRect(barRect, barWidth * 0.5f, barWidth * 0.5f, barPaint)

        // Timestamp pill
        val maxDateWidth = canvasWidth * 0.68f - contentLeft
        val dateTextWidth = datePaint.measureText(dateText).coerceAtMost(maxDateWidth)
        val pillRect = RectF(
            contentLeft,
            blockTop,
            contentLeft + dateTextWidth + pillPadH * 2,
            blockTop + datePillH
        )
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(148, 0, 0, 0)
        }
        canvas.drawRoundRect(pillRect, datePillH * 0.24f, datePillH * 0.24f, pillBg)
        canvas.drawText(
            dateText,
            contentLeft + pillPadH,
            blockTop + pillPadV - dateFm.ascent,
            datePaint
        )

        // Two address lines
        val maxAddrWidth = canvasWidth * 0.74f - contentLeft
        var currentY = blockTop + datePillH + blockSpacing - addrFm.ascent
        if (hasLine1) {
            val clipped1 = ellipsize(line1, addrPaint, maxAddrWidth)
            canvas.drawText(clipped1, contentLeft, currentY, addrPaint)
            currentY += addrLineH + lineSpacing
        }
        if (hasLine2) {
            val clipped2 = ellipsize(line2, addrPaint, maxAddrWidth)
            canvas.drawText(clipped2, contentLeft, currentY, addrPaint)
        }
    }

    private fun drawBottomRightBrand(
        canvas: Canvas,
        topText: String,
        subText: String,
        rightX: Float,
        bottomY: Float,
        shortEdge: Float,
        boldFace: Typeface
    ) {
        val topSize = shortEdge * 0.036f
        val subSize = shortEdge * 0.028f
        val lineGap = shortEdge * 0.004f

        val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = topSize
            typeface = boldFace
            setShadowLayer(shortEdge * 0.008f, 0f, shortEdge * 0.002f, Color.argb(220, 0, 0, 0))
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = subSize
            typeface = boldFace
            textAlign = Paint.Align.RIGHT
            setShadowLayer(shortEdge * 0.008f, 0f, shortEdge * 0.002f, Color.argb(220, 0, 0, 0))
        }

        val subFm = subPaint.fontMetrics
        val subHeight = if (subText.isNotBlank()) (subFm.descent - subFm.ascent) else 0f

        if (subText.isNotBlank()) {
            canvas.drawText(subText, rightX, bottomY - subFm.descent, subPaint)
        }

        if (topText.isNotBlank()) {
            val topBaseline = bottomY - (if (subHeight > 0f) subHeight + lineGap else 0f) - topPaint.fontMetrics.descent
            if (topText.equals("Timemark", ignoreCase = true)) {
                val prefix = "Time"
                val suffix = "mark"
                val totalW = topPaint.measureText(prefix) + topPaint.measureText(suffix)
                val startX = rightX - totalW
                topPaint.color = Color.parseColor("#FFC233")
                canvas.drawText(prefix, startX, topBaseline, topPaint)
                topPaint.color = Color.WHITE
                canvas.drawText(suffix, startX + topPaint.measureText(prefix), topBaseline, topPaint)
            } else {
                topPaint.color = Color.WHITE
                topPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(topText, rightX, topBaseline, topPaint)
            }
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end) + ellipsis
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
