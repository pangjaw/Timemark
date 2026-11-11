package com.sintelis.timemark.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.google.gson.GsonBuilder
import com.sintelis.timemark.domain.CapturedSlotPhoto
import com.sintelis.timemark.domain.LocationState
import com.sintelis.timemark.domain.PhotoSlot
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.ScheduleRow
import com.sintelis.timemark.domain.SessionState
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PhotoStorageManager(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun saveScheduledPhoto(
        bitmap: Bitmap,
        workDateIso: String,
        row: ScheduleRow,
        slot: PhotoSlot,
        presetDateTime: LocalDateTime,
        location: LocationState
    ): CapturedSlotPhoto {
        val rowFolderName = buildRowFolderName(row)
        val relativeDir = "Pictures/Timemark/$workDateIso/$rowFolderName"
        val internalDir = File(context.filesDir, "photos/$workDateIso/$rowFolderName")
        internalDir.mkdirs()

        // Remove any previous MediaStore entry for this slot before replacing
        row.photoFor(slot)?.let { deleteCapturedPhoto(it) }

        val internalFile = File(internalDir, slot.fileName)
        writeJpegWithExif(bitmap, internalFile, presetDateTime, location)

        val mediaUri = publishToMediaStore(
            sourceFile = internalFile,
            relativePath = relativeDir,
            displayName = slot.fileName
        )

        return CapturedSlotPhoto(
            localPath = internalFile.absolutePath,
            mediaStoreUri = mediaUri?.toString(),
            capturedAtTimestamp = ScheduleCalculator.formatExifTimestamp(presetDateTime),
            stampedLocationText = location.formattedCoordinates()
        )
    }

    fun saveInspectionPhoto(
        bitmap: Bitmap,
        workDateIso: String,
        row: ScheduleRow,
        existingToReplace: CapturedSlotPhoto?,
        presetDateTime: LocalDateTime,
        location: LocationState
    ): CapturedSlotPhoto {
        val rowFolderName = buildRowFolderName(row)
        val relativeDir = "Pictures/Timemark/$workDateIso/$rowFolderName"
        val internalDir = File(context.filesDir, "photos/$workDateIso/$rowFolderName")
        internalDir.mkdirs()

        val fileName = if (existingToReplace != null) {
            deleteCapturedPhoto(existingToReplace)
            File(existingToReplace.localPath).name
        } else {
            nextAvailableInspectionFileName(internalDir)
        }

        val internalFile = File(internalDir, fileName)
        writeJpegWithExif(bitmap, internalFile, presetDateTime, location)

        val mediaUri = publishToMediaStore(
            sourceFile = internalFile,
            relativePath = relativeDir,
            displayName = fileName
        )

        return CapturedSlotPhoto(
            localPath = internalFile.absolutePath,
            mediaStoreUri = mediaUri?.toString(),
            capturedAtTimestamp = workDateIso,
            stampedLocationText = location.formattedCoordinates()
        )
    }

    private fun nextAvailableInspectionFileName(dir: File): String {
        var idx = 1
        while (File(dir, "pemeriksaan_$idx.jpg").exists()) {
            idx++
        }
        return "pemeriksaan_$idx.jpg"
    }

    fun saveManualPhoto(
        bitmap: Bitmap,
        presetDateTime: LocalDateTime,
        location: LocationState
    ): Pair<File, Uri?> {
        val dateStr = presetDateTime.toLocalDate().toString()
        val now = LocalDateTime.now()
        val uniqueSuffix = String.format(
            java.util.Locale.US,
            "%02d%02d%02d_%03d",
            now.hour,
            now.minute,
            now.second,
            (System.currentTimeMillis() % 1000).toInt()
        )
        val fileName = "TIMEMARK_${dateStr.replace("-", "")}_$uniqueSuffix.jpg"
        val relativeDir = "Pictures/Timemark/FotoLangsung/$dateStr"
        val internalDir = File(context.filesDir, "photos/FotoLangsung/$dateStr")
        internalDir.mkdirs()

        val internalFile = File(internalDir, fileName)
        writeJpegWithExif(bitmap, internalFile, presetDateTime, location)
        val mediaUri = publishToMediaStore(internalFile, relativeDir, fileName)
        return internalFile to mediaUri
    }

    fun deleteCapturedPhoto(photo: CapturedSlotPhoto) {
        runCatching {
            val file = File(photo.localPath)
            if (file.exists()) {
                file.delete()
            }
        }
        photo.mediaStoreUri?.let { uriStr ->
            runCatching {
                context.contentResolver.delete(Uri.parse(uriStr), null, null)
            }
        }
    }

    fun exportSessionZip(session: SessionState): File {
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val zipFile = File(exportDir, "Timemark_${session.workDateIso}.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val summary = mapOf(
                "workDateIso" to session.workDateIso,
                "exportedAt" to LocalDateTime.now().toString(),
                "rows" to session.rows.map { row ->
                    mapOf(
                        "order" to row.orderNumber,
                        "labelName" to row.labelName,
                        "assetName" to row.assetName,
                        "category" to row.category,
                        "durationMinutes" to row.effectiveDurationMinutes(),
                        "includeInspection" to row.includeInspection,
                        "photos" to PhotoSlot.ordered().associate { slot ->
                            slot.fileName to ScheduleCalculator
                                .slotDateTime(session.workDateIso, row, slot)
                                .toString()
                        },
                        "inspectionPhotos" to row.allInspectionPhotos().map { photo ->
                            mapOf(
                                "file" to File(photo.localPath).name,
                                "date" to session.workDateIso
                            )
                        }
                    )
                }
            )
            zos.putNextEntry(ZipEntry("schedule_export.json"))
            zos.write(gson.toJson(summary).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            for (row in session.rows) {
                val folder = buildRowFolderName(row)
                for (slot in PhotoSlot.ordered()) {
                    val photo = row.photoFor(slot) ?: continue
                    val file = File(photo.localPath)
                    if (!file.exists()) continue
                    zos.putNextEntry(ZipEntry("$folder/${slot.fileName}"))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
                for (photo in row.allInspectionPhotos()) {
                    val file = File(photo.localPath)
                    if (!file.exists()) continue
                    zos.putNextEntry(ZipEntry("$folder/${file.name}"))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return zipFile
    }

    private fun buildRowFolderName(row: ScheduleRow): String {
        val prefix = String.format(java.util.Locale.US, "%02d", row.orderNumber)
        val safeLabel = ScheduleCalculator.sanitizeFolderName(row.labelName)
        return "${prefix}_$safeLabel"
    }

    private fun writeJpegWithExif(
        bitmap: Bitmap,
        destFile: File,
        presetDateTime: LocalDateTime,
        location: LocationState
    ) {
        FileOutputStream(destFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, fos)
        }
        val exif = ExifInterface(destFile.absolutePath)
        val exifDate = ScheduleCalculator.formatExifTimestamp(presetDateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDate)
        exif.setAttribute(ExifInterface.TAG_MAKE, "Timemark Sintelis")
        exif.setAttribute(
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            "${location.addressLine1}, ${location.addressLine2}"
        )
        exif.setLatLong(location.latitude, location.longitude)
        exif.saveAttributes()
    }

    private fun publishToMediaStore(
        sourceFile: File,
        relativePath: String,
        displayName: String
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val publicRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val sub = relativePath.removePrefix("Pictures/")
            val targetDir = File(publicRoot, sub).apply { mkdirs() }
            val targetFile = File(targetDir, displayName)
            sourceFile.copyTo(targetFile, overwrite = true)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }
}
