package com.sintelis.timemark.domain

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ScheduleCalculatorTest {

    @Test
    fun `thirty minute asset produces exact 0, 50, 100 timestamps`() {
        val dt0 = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60, 30, PhotoSlot.ZERO)
        val dt50 = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60, 30, PhotoSlot.FIFTY)
        val dt100 = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60, 30, PhotoSlot.HUNDRED)

        assertEquals("08:00", ScheduleCalculator.formatTimeHHmm(dt0))
        assertEquals("08:15", ScheduleCalculator.formatTimeHHmm(dt50))
        assertEquals("08:30", ScheduleCalculator.formatTimeHHmm(dt100))
    }

    @Test
    fun `odd 45 minute asset splits fifty percent to exact 22m30s`() {
        val dt50 = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60, 45, PhotoSlot.FIFTY)
        assertEquals("08:22", ScheduleCalculator.formatTimeHHmm(dt50))
        assertEquals("2026:11:12 08:22:30", ScheduleCalculator.formatExifTimestamp(dt50))
    }

    @Test
    fun `serat optik calculates duration as core count times six minutes`() {
        val foAsset = AssetSpec(
            id = 37,
            nama = "Serat Optik",
            kategori = "Telekomunikasi",
            waktu_menit = 6,
            satuan = "menit/core"
        )
        val duration = ScheduleCalculator.effectiveDurationMinutes(foAsset, coreCount = 12)
        assertEquals(72, duration)

        val dt50 = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60 + 45, duration, PhotoSlot.FIFTY)
        val dt100 = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60 + 45, duration, PhotoSlot.HUNDRED)
        assertEquals("09:21", ScheduleCalculator.formatTimeHHmm(dt50))
        assertEquals("09:57", ScheduleCalculator.formatTimeHHmm(dt100))
    }

    @Test
    fun `rechainRows chains sequential rows and respects locked manual rows`() {
        val r1 = ScheduleRow("1", 1, 8, "Axle Counter Siemens", "Sinyal", 45, labelName = "ZP 101A", startMinuteOfDay = 0)
        val r2 = ScheduleRow("2", 2, 37, "Serat Optik", "Telekomunikasi", 6, isPerCore = true, coreCount = 12, labelName = "OTB 1", startMinuteOfDay = 0)
        val r3Locked = ScheduleRow("3", 3, 18, "Penggerak Wesel Elektrik", "Sinyal", 45, labelName = "W12", startMinuteOfDay = 13 * 60, startTimeLocked = true)
        val r4 = ScheduleRow("4", 4, 14, "Location Case", "Sinyal", 30, labelName = "LC 1", startMinuteOfDay = 0)

        val chained = ScheduleCalculator.rechainRows(listOf(r1, r2, r3Locked, r4), 8 * 60)

        assertEquals(8 * 60, chained[0].startMinuteOfDay)          // 08:00
        assertEquals(8 * 60 + 45, chained[1].startMinuteOfDay)     // 08:45
        assertEquals(13 * 60, chained[2].startMinuteOfDay)         // 13:00 (locked)
        assertEquals(13 * 60 + 45, chained[3].startMinuteOfDay)    // 13:45 (chains after r3)
    }

    @Test
    fun `capture advances slot and delete rolls back preset slot`() {
        val dummy = CapturedSlotPhoto("a.jpg", null, "2026:11:12 08:00:00", "0,0")
        val row0 = ScheduleRow("1", 1, 8, "Axle Counter Siemens", "Sinyal", 45, labelName = "ZP", startMinuteOfDay = 8 * 60)

        val rowAfter0 = row0.copy(photos = mapOf(PhotoSlot.ZERO.name to dummy))
        assertEquals(PhotoSlot.FIFTY, ScheduleCalculator.nextSlotAfterCapture(rowAfter0, PhotoSlot.ZERO))

        val rowAfter50 = rowAfter0.copy(photos = rowAfter0.photos + (PhotoSlot.FIFTY.name to dummy))
        assertEquals(PhotoSlot.HUNDRED, ScheduleCalculator.nextSlotAfterCapture(rowAfter50, PhotoSlot.FIFTY))

        val rowAll = rowAfter50.copy(photos = rowAfter50.photos + (PhotoSlot.HUNDRED.name to dummy))
        assertNull(ScheduleCalculator.nextSlotAfterCapture(rowAll, PhotoSlot.HUNDRED))

        // Delete slot 50% -> rolls back preset to 50%
        val (rolledBackRow, activeSlot) = ScheduleCalculator.rollbackOnDelete(rowAll, PhotoSlot.FIFTY)
        assertEquals(PhotoSlot.FIFTY, activeSlot)
        assertNull(rolledBackRow.photoFor(PhotoSlot.FIFTY))
        assertTrue(rolledBackRow.photoFor(PhotoSlot.ZERO) != null)
        assertTrue(rolledBackRow.photoFor(PhotoSlot.HUNDRED) != null)
    }

    @Test
    fun `bundled data_acuan_tenaga_gabungan json loads all 38 assets`() {
        val jsonFile = File("src/main/assets/data_acuan_tenaga_gabungan.json")
        assertTrue("Missing asset JSON at ${jsonFile.absolutePath}", jsonFile.exists())
        val catalog = Gson().fromJson(jsonFile.readText(Charsets.UTF_8), AssetCatalog::class.java)
        assertEquals(38, catalog.aset.size)
        val seratOptik = catalog.aset.first { it.id == 37 }
        assertTrue(seratOptik.isPerCore)
        assertEquals(6, seratOptik.waktu_menit)

        val resorOnly = ScheduleCalculator.filterOperationalAssets(catalog.aset, onlyResor = true)
        assertEquals(13, resorOnly.size)
        assertEquals(
            listOf(
                "Axle Counter Siemens",
                "Axle Counter Fraucher",
                "Peraga Sinyal Elektrik",
                "Peralatan Dalam Sinyal Elektrik",
                "CTC-CTS",
                "Pintu Perlintasan",
                "Penggerak Wesel Elektrik",
                "Point Lock",
                "Catu Daya",
                "PTDS (Telekomunikasi Di Stasiun)",
                "PTLS (Telekomunikasi Di Luar Stasiun)",
                "PTPP (Telekomunikasi Di Pintu Perlintasan)",
                "Serat Optik"
            ),
            resorOnly.map { it.nama }
        )
    }

    @Test
    fun `inspection mode formats date without time and supports multiple inspection photos`() {
        val dt = ScheduleCalculator.slotDateTime("2026-11-12", 8 * 60, 45, PhotoSlot.INSPECTION)
        assertEquals("Kamis, Nov 12 2026", ScheduleCalculator.formatWatermarkDateOnly(dt, DateFormatStyle.PILL_SHORT_MONTH))
        assertEquals("Kamis, 12 Nov, 2026", ScheduleCalculator.formatWatermarkDateOnly(dt, DateFormatStyle.DAY_COMMA_MONTH))

        val p1 = CapturedSlotPhoto("pemeriksaan_1.jpg", null, "2026-11-12", "0,0")
        val p2 = CapturedSlotPhoto("pemeriksaan_2.jpg", null, "2026-11-12", "0,0")
        val row = ScheduleRow(
            rowId = "1",
            orderNumber = 1,
            assetId = 8,
            assetName = "Axle Counter Siemens",
            category = "Sinyal",
            baseMinutes = 45,
            labelName = "ZP 211",
            startMinuteOfDay = 8 * 60,
            includeInspection = true,
            inspectionPhotos = listOf(p1, p2)
        )
        assertEquals(listOf(PhotoSlot.ZERO, PhotoSlot.FIFTY, PhotoSlot.HUNDRED), row.activeSlots())
        assertEquals(3, row.totalSlotsCount())
        assertEquals(2, row.allInspectionPhotos().size)
        assertEquals(8 * 60 + 45, ScheduleCalculator.endMinuteOfDay(row))
    }

    @Test
    fun `legacy session json missing inspectionPhotos deserializes safely without NPE`() {
        val legacyJson = """
            {
              "workDateIso": "2026-11-12",
              "globalStartMinuteOfDay": 480,
              "rows": [
                {
                  "rowId": "r1",
                  "orderNumber": 1,
                  "assetId": 8,
                  "assetName": "Axle Counter Siemens",
                  "category": "Sinyal",
                  "baseMinutes": 45,
                  "labelName": "Axle Counter Siemens",
                  "startMinuteOfDay": 480,
                  "photos": {}
                }
              ]
            }
        """.trimIndent()
        val parsed = Gson().fromJson(legacyJson, SessionState::class.java)
        val row = parsed.rows.first()
        assertEquals(0, row.allInspectionPhotos().size)
        assertEquals(0, row.capturedCount())
    }
}
