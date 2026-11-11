package com.sintelis.timemark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sintelis.timemark.data.AssetCatalogRepository
import com.sintelis.timemark.data.PhotoStorageManager
import com.sintelis.timemark.data.SessionStore
import com.sintelis.timemark.domain.PhotoSlot
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.ScheduleRow
import com.sintelis.timemark.domain.SessionState
import com.sintelis.timemark.location.LocationController
import com.sintelis.timemark.ui.AddRowDialog
import com.sintelis.timemark.ui.BrandEditDialog
import com.sintelis.timemark.ui.CameraCaptureScreen
import com.sintelis.timemark.ui.CameraMode
import com.sintelis.timemark.ui.LocationEditDialog
import com.sintelis.timemark.ui.PhotoPreviewDialog
import com.sintelis.timemark.ui.RowEditDialog
import com.sintelis.timemark.ui.ScheduleGridScreen
import com.sintelis.timemark.ui.SettingsScreen
import com.sintelis.timemark.ui.SurfaceDark
import com.sintelis.timemark.ui.TimemarkTheme
import com.sintelis.timemark.ui.showAndroidDatePicker
import com.sintelis.timemark.ui.showAndroidTimePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimemarkTheme {
                TimemarkAppRoot()
            }
        }
    }
}

private enum class MainTab { DIRECT, SCHEDULE, SETTINGS }

private data class ActiveCameraTarget(
    val rowId: String? = null,
    val slot: PhotoSlot = PhotoSlot.ZERO,
    val isInspection: Boolean = false,
    val inspectionIndex: Int? = null
)

private sealed interface PreviewTarget {
    data class TimedSlot(val rowId: String, val slot: PhotoSlot) : PreviewTarget
    data class InspectionPhoto(val rowId: String, val index: Int) : PreviewTarget
}

@Composable
private fun TimemarkAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val catalogRepo = remember { AssetCatalogRepository(context) }
    val sessionStore = remember { SessionStore(context) }
    val storageManager = remember { PhotoStorageManager(context) }
    val locationController = remember { LocationController(context) }

    var catalog by remember { mutableStateOf(catalogRepo.loadCatalog()) }
    var isCustomCatalog by remember { mutableStateOf(catalogRepo.isUsingCustomCatalog()) }
    var session by remember { mutableStateOf(sessionStore.load()) }

    var currentTab by remember { mutableStateOf(MainTab.SCHEDULE) }
    var activeCamera by remember { mutableStateOf<ActiveCameraTarget?>(null) }

    var showAddRowDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<ScheduleRow?>(null) }
    var previewTarget by remember { mutableStateOf<PreviewTarget?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun updateSession(newState: SessionState) {
        session = newState
        sessionStore.save(newState)
    }

    fun refreshLocationFromGps() {
        locationController.startUpdates { lat, lng ->
            val currentLoc = session.location
            if (currentLoc.isManualLocked) return@startUpdates
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                currentLoc.latitude,
                currentLoc.longitude,
                lat,
                lng,
                results
            )
            // Avoid recomposing UI & hitting Geocoder on sub-25m GPS jitter once a fix is present
            if (currentLoc.hasGpsFix && results[0] < 25f) return@startUpdates

            scope.launch {
                val resolved = locationController.reverseGeocode(lat, lng)
                session = session.copy(
                    location = locationController.applyGpsFixIfUnlocked(
                        current = session.location,
                        lat = lat,
                        lng = lng,
                        resolvedAddress = resolved
                    )
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        val locGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            locationController.hasLocationPermission()
        if (locGranted) {
            refreshLocationFromGps()
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val loaded = catalogRepo.importCatalogFromUri(uri)
            catalog = loaded
            isCustomCatalog = true
            Toast.makeText(context, "Berhasil import ${loaded.aset.size} aset", Toast.LENGTH_SHORT).show()
        }.onFailure { err ->
            Toast.makeText(context, "Gagal import JSON: ${err.message}", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        val needed = buildList {
            if (!hasCameraPermission) add(Manifest.permission.CAMERA)
            if (!locationController.hasLocationPermission()) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            refreshLocationFromGps()
        }
        onDispose { locationController.stopUpdates() }
    }

    BackHandler(enabled = activeCamera != null || currentTab != MainTab.SCHEDULE) {
        if (activeCamera != null) {
            activeCamera = null
        } else {
            currentTab = MainTab.SCHEDULE
        }
    }

    // Active full-screen camera mode (Scheduled / Inspection)
    val camTarget = activeCamera
    val scheduledRow = camTarget?.rowId?.let { id -> session.rows.firstOrNull { it.rowId == id } }
    if (camTarget != null && scheduledRow != null) {
        val mode = if (camTarget.isInspection) {
            CameraMode.Inspection(
                workDateIso = session.workDateIso,
                row = scheduledRow,
                photoIndex = camTarget.inspectionIndex
            )
        } else {
            CameraMode.Scheduled(
                workDateIso = session.workDateIso,
                row = scheduledRow,
                activeSlot = camTarget.slot
            )
        }

        CameraCaptureScreen(
            mode = mode,
            location = session.location,
            watermarkConfig = session.watermarkConfig,
            hasCameraPermission = hasCameraPermission,
            showBackButton = true,
            onRequestCameraPermission = {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            },
            onBack = { activeCamera = null },
            onSelectScheduledSlot = { slot ->
                activeCamera = camTarget.copy(slot = slot)
            },
            onEditLocation = { showLocationDialog = true },
            onEditTime = {
                if (mode is CameraMode.Scheduled) {
                    editingRow = mode.row
                }
            },
            onEditBrand = { showBrandDialog = true },
            onDeleteActiveSlot = {
                if (mode is CameraMode.Scheduled) {
                    mode.row.photoFor(mode.activeSlot)?.let { storageManager.deleteCapturedPhoto(it) }
                    val (updatedRow, rolledBackSlot) = ScheduleCalculator.rollbackOnDelete(
                        mode.row,
                        mode.activeSlot
                    )
                    val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                    updateSession(session.copy(rows = newRows))
                    activeCamera = camTarget.copy(slot = rolledBackSlot)
                    Toast.makeText(
                        context,
                        "Foto ${mode.activeSlot.label} dihapus, preset jam dikembalikan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onPhotoCaptured = { stampedBitmap, presetDateTime ->
                when (mode) {
                    is CameraMode.Scheduled -> {
                        val savedPhoto = withContext(Dispatchers.IO) {
                            storageManager.saveScheduledPhoto(
                                bitmap = stampedBitmap,
                                workDateIso = session.workDateIso,
                                row = mode.row,
                                slot = mode.activeSlot,
                                presetDateTime = presetDateTime,
                                location = session.location
                            )
                        }
                        stampedBitmap.recycle()
                        val updatedPhotos = mode.row.photos.toMutableMap().apply {
                            put(mode.activeSlot.name, savedPhoto)
                        }
                        val updatedRow = mode.row.copy(photos = updatedPhotos)
                        val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                        updateSession(session.copy(rows = newRows))

                        val total = updatedRow.totalSlotsCount()
                        val msg = if (updatedRow.capturedCount() == total) {
                            "$total/$total foto lengkap untuk ${updatedRow.labelName}!"
                        } else {
                            "Foto ${mode.activeSlot.label} tersimpan. Pilih kolom berikutnya."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        activeCamera = null
                    }
                    is CameraMode.Inspection -> {
                        val existingList = mode.row.allInspectionPhotos()
                        val replaceTarget = mode.photoIndex?.let { existingList.getOrNull(it) }
                        val savedPhoto = withContext(Dispatchers.IO) {
                            storageManager.saveInspectionPhoto(
                                bitmap = stampedBitmap,
                                workDateIso = session.workDateIso,
                                row = mode.row,
                                existingToReplace = replaceTarget,
                                presetDateTime = presetDateTime,
                                location = session.location
                            )
                        }
                        stampedBitmap.recycle()
                        val updatedList = existingList.toMutableList().apply {
                            val idx = mode.photoIndex
                            if (idx != null && idx in indices) {
                                set(idx, savedPhoto)
                            } else {
                                add(savedPhoto)
                            }
                        }
                        val cleanedPhotos = mode.row.photos.toMutableMap().apply {
                            remove(PhotoSlot.INSPECTION.name)
                        }
                        val updatedRow = mode.row.copy(
                            photos = cleanedPhotos,
                            inspectionPhotos = updatedList
                        )
                        val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                        updateSession(session.copy(rows = newRows))
                        Toast.makeText(
                            context,
                            "Foto pemeriksaan tersimpan (${updatedList.size} foto)",
                            Toast.LENGTH_SHORT
                        ).show()
                        activeCamera = null
                    }
                    is CameraMode.Direct -> Unit
                }
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.DIRECT,
                        onClick = { currentTab = MainTab.DIRECT },
                        icon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                        label = { Text("Foto Langsung") }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SCHEDULE,
                        onClick = { currentTab = MainTab.SCHEDULE },
                        icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        label = { Text("Jadwal Baris") }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Pengaturan") }
                    )
                }
            }
        ) { innerPadding ->
            when (currentTab) {
                MainTab.DIRECT -> {
                    val directDate = runCatching { LocalDate.parse(session.manualDateIso) }
                        .getOrElse { LocalDate.now() }
                    CameraCaptureScreen(
                        mode = CameraMode.Direct(LocalDateTime.of(directDate, LocalTime.NOON)),
                        location = session.location,
                        watermarkConfig = session.watermarkConfig,
                        hasCameraPermission = hasCameraPermission,
                        showBackButton = false,
                        onRequestCameraPermission = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        },
                        onBack = { currentTab = MainTab.SCHEDULE },
                        onSelectScheduledSlot = {},
                        onEditLocation = { showLocationDialog = true },
                        onEditTime = {},
                        onEditDate = {
                            showAndroidDatePicker(context, session.manualDateIso) { picked ->
                                updateSession(session.copy(manualDateIso = picked))
                            }
                        },
                        onEditBrand = { showBrandDialog = true },
                        onDeleteActiveSlot = {},
                        onPhotoCaptured = { stampedBitmap, presetDateTime ->
                            withContext(Dispatchers.IO) {
                                storageManager.saveManualPhoto(
                                    bitmap = stampedBitmap,
                                    presetDateTime = presetDateTime,
                                    location = session.location
                                )
                            }
                            stampedBitmap.recycle()
                            Toast.makeText(context, "Foto tersimpan ke Galeri", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                MainTab.SCHEDULE -> ScheduleGridScreen(
                    session = session,
                    onPickWorkDate = { dateIso -> updateSession(session.copy(workDateIso = dateIso)) },
                    onPickGlobalStartTime = { startMin ->
                        val chained = ScheduleCalculator.rechainRows(session.rows, startMin)
                        updateSession(session.copy(globalStartMinuteOfDay = startMin, rows = chained))
                    },
                    onOpenAddRow = { showAddRowDialog = true },
                    onExportZip = {
                        scope.launch {
                            runCatching {
                                val zipFile = withContext(Dispatchers.IO) {
                                    storageManager.exportSessionZip(session)
                                }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    zipFile
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Bagikan ZIP Timemark"))
                            }.onFailure { err ->
                                Toast.makeText(context, "Gagal export ZIP: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onClearAllRows = {
                        session.rows.forEach { row ->
                            row.photos.values.forEach { storageManager.deleteCapturedPhoto(it) }
                            row.allInspectionPhotos().forEach { storageManager.deleteCapturedPhoto(it) }
                        }
                        updateSession(session.copy(rows = emptyList()))
                    },
                    onOpenRowCamera = { row, slot ->
                        activeCamera = ActiveCameraTarget(rowId = row.rowId, slot = slot)
                    },
                    onOpenCellPreview = { row, slot ->
                        previewTarget = PreviewTarget.TimedSlot(row.rowId, slot)
                    },
                    onDeleteSlotDirect = { row, slot ->
                        row.photoFor(slot)?.let { storageManager.deleteCapturedPhoto(it) }
                        val (updatedRow, _) = ScheduleCalculator.rollbackOnDelete(row, slot)
                        val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                        updateSession(session.copy(rows = newRows))
                        Toast.makeText(
                            context,
                            "Slot ${slot.label} dihapus & siap foto ulang pada jam slot tersebut",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onEditRow = { row -> editingRow = row },
                    onDeleteRowDirect = { target ->
                        target.photos.values.forEach { storageManager.deleteCapturedPhoto(it) }
                        target.allInspectionPhotos().forEach { storageManager.deleteCapturedPhoto(it) }
                        val remaining = session.rows.filterNot { it.rowId == target.rowId }
                        val chained = ScheduleCalculator.rechainRows(remaining, session.globalStartMinuteOfDay)
                        updateSession(session.copy(rows = chained))
                        Toast.makeText(context, "Baris ${target.labelName} dihapus", Toast.LENGTH_SHORT).show()
                    },
                    onToggleInspection = { row, enabled ->
                        val newRows = session.rows.map {
                            if (it.rowId == row.rowId) it.copy(includeInspection = enabled) else it
                        }
                        updateSession(session.copy(rows = newRows))
                    },
                    onAddInspectionPhoto = { row ->
                        activeCamera = ActiveCameraTarget(
                            rowId = row.rowId,
                            isInspection = true,
                            inspectionIndex = null
                        )
                    },
                    onOpenInspectionPreview = { row, index ->
                        previewTarget = PreviewTarget.InspectionPhoto(row.rowId, index)
                    },
                    onDeleteInspectionPhoto = { row, index ->
                        val list = row.allInspectionPhotos().toMutableList()
                        if (index in list.indices) {
                            storageManager.deleteCapturedPhoto(list.removeAt(index))
                        }
                        val cleanedPhotos = row.photos.toMutableMap().apply {
                            remove(PhotoSlot.INSPECTION.name)
                        }
                        val updatedRow = row.copy(photos = cleanedPhotos, inspectionPhotos = list)
                        val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                        updateSession(session.copy(rows = newRows))
                        Toast.makeText(context, "Foto pemeriksaan dihapus", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.SETTINGS -> SettingsScreen(
                    config = session.watermarkConfig,
                    location = session.location,
                    catalog = catalog,
                    isCustomCatalog = isCustomCatalog,
                    onConfigChange = { cfg -> updateSession(session.copy(watermarkConfig = cfg)) },
                    onOpenLocationEditor = { showLocationDialog = true },
                    onImportCatalogClick = { jsonImportLauncher.launch(arrayOf("application/json", "*/*")) },
                    onResetCatalogClick = {
                        catalog = catalogRepo.resetToBundled()
                        isCustomCatalog = false
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Dialogs
    if (showAddRowDialog) {
        val visibleAssets = remember(catalog, session.watermarkConfig.onlyResorAssets) {
            ScheduleCalculator.filterOperationalAssets(
                catalog.aset,
                session.watermarkConfig.onlyResorAssets
            )
        }
        AddRowDialog(
            assets = visibleAssets,
            nextRowNumber = session.rows.size + 1,
            onDismiss = { showAddRowDialog = false },
            onConfirm = { spec, labelPrefix, coreCount, batchCount ->
                val newEntries = (1..batchCount).map { idx ->
                    val order = session.rows.size + idx
                    val finalLabel = when {
                        labelPrefix.isNotBlank() && batchCount > 1 -> "$labelPrefix #$idx"
                        labelPrefix.isNotBlank() -> labelPrefix
                        batchCount > 1 -> "${spec.nama} #$idx"
                        else -> spec.nama
                    }
                    ScheduleRow(
                        rowId = UUID.randomUUID().toString(),
                        orderNumber = order,
                        assetId = spec.id,
                        assetName = spec.nama,
                        category = spec.kategori,
                        baseMinutes = spec.waktu_menit,
                        isPerCore = spec.isPerCore,
                        coreCount = coreCount,
                        labelName = finalLabel,
                        startMinuteOfDay = session.globalStartMinuteOfDay
                    )
                }
                val chained = ScheduleCalculator.rechainRows(
                    session.rows + newEntries,
                    session.globalStartMinuteOfDay
                )
                updateSession(session.copy(rows = chained))
                showAddRowDialog = false
            }
        )
    }

    if (showLocationDialog) {
        LocationEditDialog(
            initial = session.location,
            onRefreshGps = {
                updateSession(session.copy(location = session.location.copy(isManualLocked = false)))
                refreshLocationFromGps()
                Toast.makeText(context, "Memperbarui GPS perangkat...", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLocationDialog = false },
            onSave = { updatedLoc ->
                updateSession(session.copy(location = updatedLoc))
                showLocationDialog = false
            }
        )
    }

    if (showBrandDialog) {
        BrandEditDialog(
            initial = session.watermarkConfig,
            onDismiss = { showBrandDialog = false },
            onSave = { updatedConfig ->
                updateSession(session.copy(watermarkConfig = updatedConfig))
                showBrandDialog = false
            }
        )
    }

    editingRow?.let { target ->
        RowEditDialog(
            row = target,
            onDismiss = { editingRow = null },
            onSave = { label, startMinute, locked, cores ->
                val updated = session.rows.map {
                    if (it.rowId == target.rowId) {
                        it.copy(
                            labelName = label,
                            startMinuteOfDay = startMinute,
                            startTimeLocked = locked,
                            coreCount = cores
                        )
                    } else it
                }
                val chained = ScheduleCalculator.rechainRows(updated, session.globalStartMinuteOfDay)
                updateSession(session.copy(rows = chained))
                editingRow = null
            },
            onDeleteRow = {
                target.photos.values.forEach { storageManager.deleteCapturedPhoto(it) }
                target.allInspectionPhotos().forEach { storageManager.deleteCapturedPhoto(it) }
                val remaining = session.rows.filterNot { it.rowId == target.rowId }
                val chained = ScheduleCalculator.rechainRows(remaining, session.globalStartMinuteOfDay)
                updateSession(session.copy(rows = chained))
                editingRow = null
            }
        )
    }

    previewTarget?.let { target ->
        when (target) {
            is PreviewTarget.TimedSlot -> {
                val row = session.rows.firstOrNull { it.rowId == target.rowId }
                val photo = row?.photoFor(target.slot)
                if (row != null && photo != null) {
                    val slotTimeLabel = ScheduleCalculator.formatTimeHHmm(
                        ScheduleCalculator.slotDateTime(session.workDateIso, row, target.slot)
                    )
                    PhotoPreviewDialog(
                        title = "${row.orderNumber}. ${row.labelName} — Slot ${target.slot.label} ($slotTimeLabel)",
                        photo = photo,
                        deleteButtonLabel = "Hapus & Rollback",
                        onDismiss = { previewTarget = null },
                        onDeleteAndRollback = {
                            storageManager.deleteCapturedPhoto(photo)
                            val (updatedRow, rolledBackSlot) = ScheduleCalculator.rollbackOnDelete(row, target.slot)
                            val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                            updateSession(session.copy(rows = newRows))
                            previewTarget = null
                            activeCamera = ActiveCameraTarget(
                                rowId = updatedRow.rowId,
                                slot = rolledBackSlot
                            )
                        },
                        onRetake = {
                            previewTarget = null
                            activeCamera = ActiveCameraTarget(
                                rowId = row.rowId,
                                slot = target.slot
                            )
                        }
                    )
                } else {
                    previewTarget = null
                }
            }
            is PreviewTarget.InspectionPhoto -> {
                val row = session.rows.firstOrNull { it.rowId == target.rowId }
                val list = row?.allInspectionPhotos().orEmpty()
                val photo = list.getOrNull(target.index)
                if (row != null && photo != null) {
                    PhotoPreviewDialog(
                        title = "${row.orderNumber}. ${row.labelName} — Pemeriksaan #${target.index + 1} (Tanpa Jam)",
                        photo = photo,
                        deleteButtonLabel = "Hapus Foto",
                        onDismiss = { previewTarget = null },
                        onDeleteAndRollback = {
                            storageManager.deleteCapturedPhoto(photo)
                            val updatedList = list.toMutableList().apply { removeAt(target.index) }
                            val cleanedPhotos = row.photos.toMutableMap().apply {
                                remove(PhotoSlot.INSPECTION.name)
                            }
                            val updatedRow = row.copy(photos = cleanedPhotos, inspectionPhotos = updatedList)
                            val newRows = session.rows.map { if (it.rowId == updatedRow.rowId) updatedRow else it }
                            updateSession(session.copy(rows = newRows))
                            previewTarget = null
                        },
                        onRetake = {
                            previewTarget = null
                            activeCamera = ActiveCameraTarget(
                                rowId = row.rowId,
                                isInspection = true,
                                inspectionIndex = target.index
                            )
                        }
                    )
                } else {
                    previewTarget = null
                }
            }
        }
    }
}
