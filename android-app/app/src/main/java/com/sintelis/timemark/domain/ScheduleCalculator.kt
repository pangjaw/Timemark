package com.sintelis.timemark.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

object ScheduleCalculator {

    private val indonesianDays = mapOf(
        DayOfWeek.MONDAY to "Senin",
        DayOfWeek.TUESDAY to "Selasa",
        DayOfWeek.WEDNESDAY to "Rabu",
        DayOfWeek.THURSDAY to "Kamis",
        DayOfWeek.FRIDAY to "Jumat",
        DayOfWeek.SATURDAY to "Sabtu",
        DayOfWeek.SUNDAY to "Minggu"
    )

    private val indonesianShortMonths = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    private data class ResorAssetPreset(val id: Int, val displayName: String)

    private val resorAssetPresets = listOf(
        ResorAssetPreset(8, "Axle Counter Siemens"),
        ResorAssetPreset(10, "Axle Counter Fraucher"),
        ResorAssetPreset(6, "Peraga Sinyal Elektrik"),
        ResorAssetPreset(5, "Peralatan Dalam Sinyal Elektrik"),
        ResorAssetPreset(7, "CTC-CTS"),
        ResorAssetPreset(16, "Pintu Perlintasan"),
        ResorAssetPreset(18, "Penggerak Wesel Elektrik"),
        ResorAssetPreset(15, "Point Lock"),
        ResorAssetPreset(17, "Catu Daya"),
        ResorAssetPreset(34, "PTDS (Telekomunikasi Di Stasiun)"),
        ResorAssetPreset(35, "PTLS (Telekomunikasi Di Luar Stasiun)"),
        ResorAssetPreset(36, "PTPP (Telekomunikasi Di Pintu Perlintasan)"),
        ResorAssetPreset(37, "Serat Optik")
    )

    fun filterOperationalAssets(allAssets: List<AssetSpec>, onlyResor: Boolean): List<AssetSpec> {
        if (!onlyResor) return allAssets
        val byId = allAssets.associateBy { it.id }
        return resorAssetPresets.mapNotNull { preset ->
            byId[preset.id]?.copy(nama = preset.displayName)
        }
    }

    fun effectiveDurationMinutes(asset: AssetSpec, coreCount: Int = 1): Int {
        return if (asset.isPerCore) {
            coreCount.coerceAtLeast(1) * asset.waktu_menit.coerceAtLeast(1)
        } else {
            asset.waktu_menit.coerceAtLeast(1)
        }
    }

    fun slotDateTime(
        workDateIso: String,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        slot: PhotoSlot
    ): LocalDateTime {
        val baseDate = LocalDate.parse(workDateIso)
        val dayOffset = startMinuteOfDay.floorDiv(1440).toLong()
        val minuteInDay = ((startMinuteOfDay % 1440) + 1440) % 1440
        val startDateTime = LocalDateTime.of(
            baseDate.plusDays(dayOffset),
            LocalTime.of(minuteInDay / 60, minuteInDay % 60)
        )
        val offsetSeconds = when (slot) {
            PhotoSlot.ZERO -> 0L
            PhotoSlot.FIFTY -> (durationMinutes * 30.0).roundToLong()
            PhotoSlot.HUNDRED -> durationMinutes * 60L
            PhotoSlot.INSPECTION -> 0L
        }
        return startDateTime.plusSeconds(offsetSeconds)
    }

    fun slotDateTime(workDateIso: String, row: ScheduleRow, slot: PhotoSlot): LocalDateTime {
        return slotDateTime(
            workDateIso = workDateIso,
            startMinuteOfDay = row.startMinuteOfDay,
            durationMinutes = row.effectiveDurationMinutes(),
            slot = slot
        )
    }

    fun endMinuteOfDay(row: ScheduleRow): Int {
        return row.startMinuteOfDay + row.effectiveDurationMinutes()
    }

    /**
     * Recalculates sequential start times across rows.
     * Row 0 starts at [globalStartMinuteOfDay] unless manually locked.
     * Row i > 0 starts at Row i-1's 100% completion minute unless manually locked.
     */
    fun rechainRows(rows: List<ScheduleRow>, globalStartMinuteOfDay: Int): List<ScheduleRow> {
        if (rows.isEmpty()) return emptyList()
        val result = ArrayList<ScheduleRow>(rows.size)
        var cursorMinute = globalStartMinuteOfDay
        rows.forEachIndexed { index, row ->
            val startMin = if (row.startTimeLocked) row.startMinuteOfDay else cursorMinute
            val updated = row.copy(
                orderNumber = index + 1,
                startMinuteOfDay = startMin
            )
            result.add(updated)
            cursorMinute = startMin + updated.effectiveDurationMinutes()
        }
        return result
    }

    fun firstEmptySlot(row: ScheduleRow): PhotoSlot {
        return PhotoSlot.ordered().firstOrNull { !row.photos.containsKey(it.name) } ?: PhotoSlot.ZERO
    }

    /**
     * Advances to the next empty slot after [justCaptured], or wraps to an earlier empty slot.
     * Returns null when all 3 timed slots are complete.
     */
    fun nextSlotAfterCapture(row: ScheduleRow, justCaptured: PhotoSlot): PhotoSlot? {
        val ordered = PhotoSlot.ordered()
        val currentIdx = ordered.indexOf(justCaptured)
        for (i in (currentIdx + 1) until ordered.size) {
            if (!row.photos.containsKey(ordered[i].name)) {
                return ordered[i]
            }
        }
        return ordered.firstOrNull { !row.photos.containsKey(it.name) }
    }

    /**
     * Removes [deletedSlot] from [row] and returns the updated row paired with the
     * rolled-back slot that the camera preset should revert to.
     */
    fun rollbackOnDelete(row: ScheduleRow, deletedSlot: PhotoSlot): Pair<ScheduleRow, PhotoSlot> {
        val updatedPhotos = row.photos.toMutableMap()
        updatedPhotos.remove(deletedSlot.name)
        return row.copy(photos = updatedPhotos) to deletedSlot
    }

    fun formatTimeHHmm(dateTime: LocalDateTime): String {
        return String.format(Locale.US, "%02d:%02d", dateTime.hour, dateTime.minute)
    }

    fun formatExifTimestamp(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US))
    }

    fun formatWatermarkDateTime(dateTime: LocalDateTime, style: DateFormatStyle): String {
        val datePart = formatWatermarkDateOnly(dateTime, style)
        val time = formatTimeHHmm(dateTime)
        return "$datePart $time"
    }

    fun formatWatermarkDateOnly(dateTime: LocalDateTime, style: DateFormatStyle): String {
        val dayName = indonesianDays[dateTime.dayOfWeek] ?: "Senin"
        val monthShort = indonesianShortMonths[dateTime.monthValue - 1]
        val day = String.format(Locale.US, "%02d", dateTime.dayOfMonth)
        return when (style) {
            DateFormatStyle.PILL_SHORT_MONTH ->
                "$dayName, $monthShort $day ${dateTime.year}"
            DateFormatStyle.DAY_COMMA_MONTH ->
                "$dayName, $day $monthShort, ${dateTime.year}"
        }
    }

    fun formatIndonesianDateOnly(dateIso: String): String {
        val date = LocalDate.parse(dateIso)
        val dayName = indonesianDays[date.dayOfWeek] ?: "Senin"
        val monthShort = indonesianShortMonths[date.monthValue - 1]
        return "$dayName, ${date.dayOfMonth} $monthShort ${date.year}"
    }

    fun sanitizeFolderName(raw: String): String {
        return raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .ifBlank { "Aset" }
    }
}
