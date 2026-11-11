package com.sintelis.timemark.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sintelis.timemark.data.rememberAsyncBitmap
import com.sintelis.timemark.domain.CapturedSlotPhoto
import com.sintelis.timemark.domain.PhotoSlot
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.ScheduleRow
import com.sintelis.timemark.domain.SessionState
import java.io.File

@Composable
fun ScheduleGridScreen(
    session: SessionState,
    onPickWorkDate: (String) -> Unit,
    onPickGlobalStartTime: (Int) -> Unit,
    onOpenAddRow: () -> Unit,
    onExportZip: () -> Unit,
    onClearAllRows: () -> Unit,
    onOpenRowCamera: (row: ScheduleRow, slot: PhotoSlot) -> Unit,
    onOpenCellPreview: (row: ScheduleRow, slot: PhotoSlot) -> Unit,
    onDeleteSlotDirect: (row: ScheduleRow, slot: PhotoSlot) -> Unit,
    onEditRow: (ScheduleRow) -> Unit,
    onDeleteRowDirect: (ScheduleRow) -> Unit,
    onToggleInspection: (row: ScheduleRow, enabled: Boolean) -> Unit,
    onAddInspectionPhoto: (row: ScheduleRow) -> Unit,
    onOpenInspectionPreview: (row: ScheduleRow, index: Int) -> Unit,
    onDeleteInspectionPhoto: (row: ScheduleRow, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreditDialog by remember { mutableStateOf(false) }
    val startHh = ((session.globalStartMinuteOfDay % 1440) + 1440) % 1440 / 60
    val startMm = ((session.globalStartMinuteOfDay % 1440) + 1440) % 1440 % 60
    val startLabel = String.format(java.util.Locale.US, "%02d:%02d", startHh, startMm)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
    ) {
        // Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Timemark Sintelis",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ScheduleCalculator.formatIndonesianDateOnly(session.workDateIso),
                            fontSize = 12.sp,
                            color = TimemarkGold,
                            modifier = Modifier.clickable {
                                showAndroidDatePicker(context, session.workDateIso, onPickWorkDate)
                            }
                        )
                        Text("·", color = Color(0xFF64748B))
                        Text(
                            text = "Mulai $startLabel",
                            fontSize = 12.sp,
                            color = TimemarkOrange,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                showAndroidTimePicker(
                                    context,
                                    session.globalStartMinuteOfDay,
                                    onPickGlobalStartTime
                                )
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showCreditDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Tentang Aplikasi",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                    if (session.rows.isNotEmpty()) {
                        IconButton(onClick = onClearAllRows) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Reset Baris",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenAddRow,
                    colors = ButtonDefaults.buttonColors(containerColor = TimemarkOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah Baris Aset", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExportZip,
                    enabled = session.rows.any { it.capturedCount() > 0 },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ZIP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (session.rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = TimemarkOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Belum ada baris pekerjaan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Tekan '+ Tambah Baris Aset' untuk memilih jenis aset Sintelis dan menyiapkan preset foto 0%, 50%, dan 100%.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(session.rows, key = { it.rowId }) { row ->
                    ScheduleRowCard(
                        workDateIso = session.workDateIso,
                        row = row,
                        onClickRow = {
                            val targetSlot = ScheduleCalculator.firstEmptySlot(row)
                            onOpenRowCamera(row, targetSlot)
                        },
                        onClickCell = { slot ->
                            if (row.photoFor(slot) != null) {
                                onOpenCellPreview(row, slot)
                            } else {
                                onOpenRowCamera(row, slot)
                            }
                        },
                        onDeleteCell = { slot -> onDeleteSlotDirect(row, slot) },
                        onEditRow = { onEditRow(row) },
                        onDeleteRow = { onDeleteRowDirect(row) },
                        onToggleInspection = { enabled -> onToggleInspection(row, enabled) },
                        onAddInspectionPhoto = { onAddInspectionPhoto(row) },
                        onOpenInspectionPreview = { idx -> onOpenInspectionPreview(row, idx) },
                        onDeleteInspectionPhoto = { idx -> onDeleteInspectionPhoto(row, idx) }
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAddRow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tambah Baris Aset", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Dika Armansyah · NIPP 72347",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }

    if (showCreditDialog) {
        AlertDialog(
            onDismissRequest = { showCreditDialog = false },
            title = { Text("Timemark Sintelis", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Versi 1.0.0", fontSize = 13.sp, color = TimemarkOrange, fontWeight = FontWeight.SemiBold)
                    Text("Dikembangkan oleh: Dika Armansyah", fontSize = 13.sp, color = Color.White)
                    Text("NIPP: 72347", fontSize = 13.sp, color = TimemarkGold, fontWeight = FontWeight.SemiBold)
                    Text("UPT Resor Sintelis 1.21 Bogor", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreditDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun ScheduleRowCard(
    workDateIso: String,
    row: ScheduleRow,
    onClickRow: () -> Unit,
    onClickCell: (PhotoSlot) -> Unit,
    onDeleteCell: (PhotoSlot) -> Unit,
    onEditRow: () -> Unit,
    onDeleteRow: () -> Unit,
    onToggleInspection: (Boolean) -> Unit,
    onAddInspectionPhoto: () -> Unit,
    onOpenInspectionPreview: (Int) -> Unit,
    onDeleteInspectionPhoto: (Int) -> Unit
) {
    val firstEmpty = ScheduleCalculator.firstEmptySlot(row)
    val totalSlots = row.totalSlotsCount()
    val allDone = row.capturedCount() == totalSlots
    val activeSlots = PhotoSlot.ordered()
    val inspectionList = row.allInspectionPhotos()
    val durMinutes = row.effectiveDurationMinutes()
    val subtitle = if (row.isPerCore) {
        "${row.assetName} (${row.coreCount} core × ${row.baseMinutes}m) · $durMinutes mnt"
    } else {
        "${row.assetName} · $durMinutes mnt"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                color = if (allDone) TimemarkGreen.copy(alpha = 0.55f) else Color(0xFF334155),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClickRow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${row.orderNumber}. ${row.labelName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (allDone) TimemarkGreen.copy(alpha = 0.2f)
                            else CardDark
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${row.capturedCount()}/$totalSlots",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (allDone) TimemarkGreen else Color.White
                    )
                }
                IconButton(
                    onClick = onEditRow,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Baris",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteRow,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Hapus Baris",
                        tint = TimemarkRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3 Columns: 0% | 50% | 100%
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeSlots.forEach { slot ->
                val photo = row.photoFor(slot)
                val slotTime = ScheduleCalculator.formatTimeHHmm(
                    ScheduleCalculator.slotDateTime(workDateIso, row, slot)
                )
                val isNextTarget = !allDone && photo == null && slot == firstEmpty

                SlotCell(
                    slot = slot,
                    timeLabel = slotTime,
                    photoPath = photo?.localPath,
                    isNextTarget = isNextTarget,
                    compactMode = false,
                    onClick = { onClickCell(slot) },
                    onDelete = { onDeleteCell(slot) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Option row below 0/50/100 to enable/disable Inspection photos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CanvasDark)
                .clickable { onToggleInspection(!row.includeInspection) }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tambah Foto Pemeriksaan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    if (row.includeInspection) {
                        "Aktif (${inspectionList.size} foto) · Tanpa jam (tanggal saja)"
                    } else {
                        "Nonaktif · Hanya 0%, 50%, 100%"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Switch(
                checked = row.includeInspection,
                onCheckedChange = onToggleInspection
            )
        }

        // Inspection photo section below the ON/OFF option
        if (row.includeInspection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CanvasDark)
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (inspectionList.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(inspectionList) { index, photo ->
                            InspectionPhotoTile(
                                indexOneBased = index + 1,
                                photo = photo,
                                onClick = { onOpenInspectionPreview(index) },
                                onDelete = { onDeleteInspectionPhoto(index) }
                            )
                        }
                    }
                }

                Button(
                    onClick = onAddInspectionPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = TimemarkOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (inspectionList.isEmpty()) Icons.Filled.CameraAlt else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (inspectionList.isEmpty()) {
                            "Ambil Foto Pemeriksaan"
                        } else {
                            "Tambah Foto Pemeriksaan (+1)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionPhotoTile(
    indexOneBased: Int,
    photo: CapturedSlotPhoto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbnail = rememberAsyncBitmap(photo.localPath, maxDimensionPx = 220)
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, TimemarkGreen.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = "Foto Pemeriksaan #$indexOneBased",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                "Periksa #$indexOneBased",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(TimemarkRed)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Hapus Foto Pemeriksaan",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun SlotCell(
    slot: PhotoSlot,
    timeLabel: String,
    photoPath: String?,
    isNextTarget: Boolean,
    compactMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhoto = photoPath != null && File(photoPath).exists()
    val borderColor = when {
        isNextTarget -> TimemarkOrange
        hasPhoto -> TimemarkGreen.copy(alpha = 0.65f)
        else -> Color(0xFF334155)
    }

    val thumbnail = rememberAsyncBitmap(photoPath, maxDimensionPx = 220)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CanvasDark)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isNextTarget) TimemarkOrange.copy(alpha = 0.2f)
                    else CardDark
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                slot.label,
                fontSize = if (compactMode) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNextTarget) TimemarkOrange else Color.White,
                maxLines = 1
            )
            Text(
                timeLabel,
                fontSize = if (compactMode) 9.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isNextTarget) TimemarkGold else Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = "Foto ${slot.label}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(TimemarkRed)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Hapus & Rollback",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TimemarkGreen)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isNextTarget) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = TimemarkOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Ambil Foto",
                            fontSize = if (compactMode) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TimemarkOrange
                        )
                    } else {
                        Text(
                            "Kosong",
                            fontSize = if (compactMode) 10.sp else 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}
