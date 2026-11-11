package com.sintelis.timemark.domain

data class AssetCatalog(
    val dokumen: String = "",
    val keterangan: String = "",
    val aset: List<AssetSpec> = emptyList()
)

data class AssetSpec(
    val id: Int,
    val nama: String,
    val pelaksana: Int = 1,
    val pelaksana_lanjutan: Int = 0,
    val kategori: String,
    val waktu_menit: Int,
    val satuan: String? = null,
    val catatan: String? = null
) {
    val isPerCore: Boolean
        get() = satuan?.contains("core", ignoreCase = true) == true
}

enum class PhotoSlot(val percent: Int?, val label: String, val fileName: String) {
    ZERO(0, "0%", "0.jpg"),
    FIFTY(50, "50%", "50.jpg"),
    HUNDRED(100, "100%", "100.jpg"),
    INSPECTION(null, "Periksa", "pemeriksaan.jpg");

    val requiresTime: Boolean
        get() = this != INSPECTION

    companion object {
        fun ordered(): List<PhotoSlot> = listOf(ZERO, FIFTY, HUNDRED)
        fun allWithInspection(): List<PhotoSlot> = listOf(ZERO, FIFTY, HUNDRED, INSPECTION)
    }
}

data class CapturedSlotPhoto(
    val localPath: String,
    val mediaStoreUri: String? = null,
    val capturedAtTimestamp: String,
    val stampedLocationText: String
)

data class ScheduleRow(
    val rowId: String,
    val orderNumber: Int,
    val assetId: Int,
    val assetName: String,
    val category: String,
    val baseMinutes: Int,
    val isPerCore: Boolean = false,
    val coreCount: Int = 1,
    val labelName: String,
    val startMinuteOfDay: Int,
    val startTimeLocked: Boolean = false,
    val includeInspection: Boolean = false,
    val photos: Map<String, CapturedSlotPhoto> = emptyMap(),
    val inspectionPhotos: List<CapturedSlotPhoto> = emptyList()
) {
    fun effectiveDurationMinutes(): Int =
        if (isPerCore) (coreCount.coerceAtLeast(1) * baseMinutes) else baseMinutes

    fun activeSlots(): List<PhotoSlot> = PhotoSlot.ordered()

    fun totalSlotsCount(): Int = activeSlots().size

    @Suppress("USELESS_ELVIS")
    fun safePhotos(): Map<String, CapturedSlotPhoto> = photos ?: emptyMap()

    fun photoFor(slot: PhotoSlot): CapturedSlotPhoto? = safePhotos()[slot.name]

    fun capturedCount(): Int = activeSlots().count { safePhotos().containsKey(it.name) }

    @Suppress("USELESS_ELVIS")
    fun allInspectionPhotos(): List<CapturedSlotPhoto> {
        val list = inspectionPhotos ?: emptyList()
        val legacy = safePhotos()[PhotoSlot.INSPECTION.name]
        return if (legacy != null && list.none { it.localPath == legacy.localPath }) {
            listOf(legacy) + list
        } else {
            list
        }
    }
}

data class LocationState(
    val latitude: Double = -6.597147,
    val longitude: Double = 106.806039,
    val addressLine1: String = "Area Stasiun / Jalur KA",
    val addressLine2: String = "UPT Resor Sintelis 1.21 Bogor",
    val isManualLocked: Boolean = false,
    val hasGpsFix: Boolean = false
) {
    fun formattedCoordinates(): String =
        String.format(java.util.Locale.US, "%.6f, %.6f", latitude, longitude)
}

enum class DateFormatStyle {
    PILL_SHORT_MONTH,   // Senin, Nov 09 2026 22:02
    DAY_COMMA_MONTH     // Senin, 09 Nov, 2026 22:02
}

data class WatermarkConfig(
    val showGpsPill: Boolean = true,
    val brandTopText: String = "Timemark",
    val brandSubText: String = "",
    val dateFormatStyle: DateFormatStyle = DateFormatStyle.PILL_SHORT_MONTH,
    val onlyResorAssets: Boolean = true
)

data class SessionState(
    val workDateIso: String,
    val globalStartMinuteOfDay: Int = 8 * 60,
    val rows: List<ScheduleRow> = emptyList(),
    val location: LocationState = LocationState(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val manualDateIso: String = workDateIso,
    val manualMinuteOfDay: Int = 8 * 60,
    val manualFollowRealtime: Boolean = false,
    val watermarkSchemaVersion: Int = 2
)
