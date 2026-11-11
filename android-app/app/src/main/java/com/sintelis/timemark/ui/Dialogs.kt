package com.sintelis.timemark.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sintelis.timemark.data.rememberAsyncBitmap
import com.sintelis.timemark.domain.AssetSpec
import com.sintelis.timemark.domain.CapturedSlotPhoto
import com.sintelis.timemark.domain.DateFormatStyle
import com.sintelis.timemark.domain.LocationState
import com.sintelis.timemark.domain.PhotoSlot
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.ScheduleRow
import com.sintelis.timemark.domain.WatermarkConfig
import java.io.File
import java.time.LocalDate

@Composable
fun AddRowDialog(
    assets: List<AssetSpec>,
    nextRowNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (asset: AssetSpec, labelPrefix: String, coreCount: Int, rowCount: Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedAsset by remember { mutableStateOf(assets.firstOrNull()) }
    var labelInput by remember { mutableStateOf("") }
    var coreText by remember { mutableStateOf("12") }
    var batchCountText by remember { mutableStateOf("1") }

    val categories = listOf("Semua", "Sinyal", "Telekomunikasi")
    val filtered = remember(assets, search, selectedCategory) {
        assets.filter { spec ->
            val catMatch = selectedCategory == "Semua" ||
                spec.kategori.equals(selectedCategory, ignoreCase = true)
            val qMatch = search.isBlank() || spec.nama.contains(search, ignoreCase = true)
            catMatch && qMatch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Baris Aset", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Cari jenis aset") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CanvasDark)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.id }) { spec ->
                        val isSelected = selectedAsset?.id == spec.id
                        val durationLabel = if (spec.isPerCore) {
                            "${spec.waktu_menit} mnt/core"
                        } else {
                            "${spec.waktu_menit} mnt"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) TimemarkOrange.copy(alpha = 0.22f)
                                    else SurfaceDark
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TimemarkOrange else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedAsset = spec }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    spec.nama,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    spec.kategori,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                durationLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TimemarkGold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Nama Aset (Optional)") },
                    placeholder = {
                        Text(
                            "Contoh: ZP 211 BOO-CLT (kosong = ${selectedAsset?.nama ?: "nama aset"})",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedAsset?.isPerCore == true) {
                        OutlinedTextField(
                            value = coreText,
                            onValueChange = { coreText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Jumlah Core") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = batchCountText,
                        onValueChange = { batchCountText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Jumlah Baris") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                selectedAsset?.let { spec ->
                    val cores = coreText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val dur = ScheduleCalculator.effectiveDurationMinutes(spec, cores)
                    val count = batchCountText.toIntOrNull()?.coerceIn(1, 30) ?: 1
                    Text(
                        text = "Durasi per baris: $dur menit · Total dibuat: $count baris",
                        fontSize = 12.sp,
                        color = TimemarkGold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedAsset != null,
                onClick = {
                    val asset = selectedAsset ?: return@Button
                    val cores = coreText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val count = batchCountText.toIntOrNull()?.coerceIn(1, 30) ?: 1
                    onConfirm(asset, labelInput.trim(), cores, count)
                }
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun LocationEditDialog(
    initial: LocationState,
    onRefreshGps: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (LocationState) -> Unit
) {
    var latText by remember(initial) { mutableStateOf(String.format(java.util.Locale.US, "%.6f", initial.latitude)) }
    var lngText by remember(initial) { mutableStateOf(String.format(java.util.Locale.US, "%.6f", initial.longitude)) }
    var line1 by remember(initial) { mutableStateOf(initial.addressLine1) }
    var line2 by remember(initial) { mutableStateOf(initial.addressLine2) }
    var locked by remember(initial) { mutableStateOf(initial.isManualLocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit & Kunci Lokasi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kunci lokasi manual (abaikan GPS bergerak)", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }
                OutlinedButton(
                    onClick = onRefreshGps,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ambil GPS Perangkat Sekarang")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it },
                    label = { Text("Alamat Baris 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line2,
                    onValueChange = { line2 = it },
                    label = { Text("Alamat Baris 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = latText.toDoubleOrNull() ?: initial.latitude
                val lng = lngText.toDoubleOrNull() ?: initial.longitude
                onSave(
                    initial.copy(
                        latitude = lat,
                        longitude = lng,
                        addressLine1 = line1.trim(),
                        addressLine2 = line2.trim(),
                        isManualLocked = locked
                    )
                )
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun RowEditDialog(
    row: ScheduleRow,
    onDismiss: () -> Unit,
    onSave: (label: String, startMinuteOfDay: Int, locked: Boolean, coreCount: Int) -> Unit,
    onDeleteRow: () -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(row.labelName) }
    var startMinute by remember { mutableIntStateOf(row.startMinuteOfDay) }
    var locked by remember { mutableStateOf(row.startTimeLocked) }
    var coreText by remember { mutableStateOf(row.coreCount.toString()) }

    val hh = ((startMinute % 1440) + 1440) % 1440 / 60
    val mm = ((startMinute % 1440) + 1440) % 1440 % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Baris #${row.orderNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${row.assetName} (${row.category})", fontSize = 12.sp, color = TimemarkGold)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nama Aset (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (row.isPerCore) {
                    OutlinedTextField(
                        value = coreText,
                        onValueChange = { coreText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Jumlah Core (${row.baseMinutes} mnt/core)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            startMinute = h * 60 + m
                            locked = true
                        }, hh, mm, true).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(String.format(java.util.Locale.US, "Jam Mulai Baris: %02d:%02d", hh, mm))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kunci jam mulai baris ini (jangan sambung otomatis)", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }
                TextButton(
                    onClick = onDeleteRow,
                    colors = ButtonDefaults.textButtonColors(contentColor = TimemarkRed)
                ) {
                    Text("Hapus Baris Ini")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cores = coreText.toIntOrNull()?.coerceAtLeast(1) ?: row.coreCount
                onSave(label.trim().ifBlank { row.assetName }, startMinute, locked, cores)
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun PhotoPreviewDialog(
    title: String,
    photo: CapturedSlotPhoto,
    deleteButtonLabel: String,
    onDismiss: () -> Unit,
    onDeleteAndRollback: () -> Unit,
    onRetake: () -> Unit
) {
    val bitmap = rememberAsyncBitmap(photo.localPath, maxDimensionPx = 1280)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Preview Foto",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("File foto tidak ditemukan", color = Color.Gray)
                }
            }
            Text(
                "EXIF Stamp: ${photo.capturedAtTimestamp} · GPS: ${photo.stampedLocationText}",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDeleteAndRollback,
                    colors = ButtonDefaults.buttonColors(containerColor = TimemarkRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(deleteButtonLabel, fontSize = 12.sp)
                }
                Button(
                    onClick = onRetake,
                    colors = ButtonDefaults.buttonColors(containerColor = TimemarkOrange),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Foto Ulang", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Tutup")
            }
        }
    }
}

fun showAndroidDatePicker(
    context: android.content.Context,
    currentIso: String,
    onPicked: (String) -> Unit
) {
    val date = runCatching { LocalDate.parse(currentIso) }.getOrElse { LocalDate.now() }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onPicked(LocalDate.of(year, month + 1, dayOfMonth).toString())
        },
        date.year,
        date.monthValue - 1,
        date.dayOfMonth
    ).show()
}

fun showAndroidTimePicker(
    context: android.content.Context,
    minuteOfDay: Int,
    onPicked: (Int) -> Unit
) {
    val normalized = ((minuteOfDay % 1440) + 1440) % 1440
    TimePickerDialog(
        context,
        { _, hour, minute ->
            onPicked(hour * 60 + minute)
        },
        normalized / 60,
        normalized % 60,
        true
    ).show()
}

@Composable
fun BrandEditDialog(
    initial: WatermarkConfig,
    onDismiss: () -> Unit,
    onSave: (WatermarkConfig) -> Unit
) {
    var topText by remember(initial) { mutableStateOf(initial.brandTopText) }
    var subText by remember(initial) { mutableStateOf(initial.brandSubText) }
    var showGps by remember(initial) { mutableStateOf(initial.showGpsPill) }
    var dateStyle by remember(initial) { mutableStateOf(initial.dateFormatStyle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Template Brand & Format", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = topText,
                    onValueChange = { topText = it },
                    label = { Text("Brand Baris 1 (kosongkan jika tanpa brand)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subText,
                    onValueChange = { subText = it },
                    label = { Text("Sub-Brand Baris 2 (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Pill Koordinat GPS", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = showGps, onCheckedChange = { showGps = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isShort = dateStyle == DateFormatStyle.PILL_SHORT_MONTH
                    OutlinedButton(
                        onClick = { dateStyle = DateFormatStyle.PILL_SHORT_MONTH },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isShort) "✓ Nov 12 2026" else "Nov 12 2026", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { dateStyle = DateFormatStyle.DAY_COMMA_MONTH },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (!isShort) "✓ 12 Nov, 2026" else "12 Nov, 2026", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    initial.copy(
                        brandTopText = topText.trim(),
                        brandSubText = subText.trim(),
                        showGpsPill = showGps,
                        dateFormatStyle = dateStyle
                    )
                )
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
