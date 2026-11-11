package com.sintelis.timemark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sintelis.timemark.domain.AssetCatalog
import com.sintelis.timemark.domain.DateFormatStyle
import com.sintelis.timemark.domain.LocationState
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.SessionState
import com.sintelis.timemark.domain.WatermarkConfig

@Composable
fun ManualSetupScreen(
    session: SessionState,
    onDateChanged: (String) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    onToggleRealtime: (Boolean) -> Unit,
    onOpenLocationEditor: () -> Unit,
    onOpenManualCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalized = ((session.manualMinuteOfDay % 1440) + 1440) % 1440
    val timeText = String.format(java.util.Locale.US, "%02d:%02d WIB", normalized / 60, normalized % 60)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Set Manual & Preset Lokasi", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            "Gunakan mode ini untuk mengambil foto tunggal dengan tanggal/jam bebas serta koordinat & alamat yang dapat dikunci manual.",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )

        // Date & Time Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tanggal & Jam Stamp", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (session.manualFollowRealtime) "Real-time" else "Manual",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.manualFollowRealtime) TimemarkGreen else TimemarkOrange
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CanvasDark)
                        .clickable(enabled = !session.manualFollowRealtime) {
                            showAndroidDatePicker(context, session.manualDateIso, onDateChanged)
                        }
                        .padding(10.dp)
                ) {
                    Text("Tanggal", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        ScheduleCalculator.formatIndonesianDateOnly(session.manualDateIso),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CanvasDark)
                        .clickable(enabled = !session.manualFollowRealtime) {
                            showAndroidTimePicker(context, session.manualMinuteOfDay, onMinuteChanged)
                        }
                        .padding(10.dp)
                ) {
                    Text("Jam", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        timeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TimemarkOrange
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ikuti jam perangkat saat ini", fontSize = 13.sp, color = Color.White)
                Switch(
                    checked = session.manualFollowRealtime,
                    onCheckedChange = onToggleRealtime
                )
            }
        }

        // Location Card
        LocationSummaryCard(
            location = session.location,
            onEdit = onOpenLocationEditor
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onOpenManualCamera,
            colors = ButtonDefaults.buttonColors(containerColor = TimemarkOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Buka Kamera Set Manual", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun SettingsScreen(
    config: WatermarkConfig,
    location: LocationState,
    catalog: AssetCatalog,
    isCustomCatalog: Boolean,
    onConfigChange: (WatermarkConfig) -> Unit,
    onOpenLocationEditor: () -> Unit,
    onImportCatalogClick: () -> Unit,
    onResetCatalogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pengaturan Watermark & Data Aset", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Branding & Format Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Tampilan Watermark", fontWeight = FontWeight.Bold, color = Color.White)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tampilkan Pill Koordinat GPS (kiri atas)", fontSize = 13.sp, color = Color.White)
                Switch(
                    checked = config.showGpsPill,
                    onCheckedChange = { onConfigChange(config.copy(showGpsPill = it)) }
                )
            }

            OutlinedTextField(
                value = config.brandTopText,
                onValueChange = { onConfigChange(config.copy(brandTopText = it)) },
                label = { Text("Teks Branding Kanan Bawah (Baris 1)") },
                placeholder = { Text("Kosongkan jika tanpa teks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = config.brandSubText,
                onValueChange = { onConfigChange(config.copy(brandSubText = it)) },
                label = { Text("Sub-teks Branding Kanan Bawah (Baris 2)") },
                placeholder = { Text("Mis. Sintelis 1.21") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Format Tanggal", fontSize = 12.sp, color = Color(0xFF94A3B8))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val style1 = config.dateFormatStyle == DateFormatStyle.PILL_SHORT_MONTH
                OutlinedButton(
                    onClick = { onConfigChange(config.copy(dateFormatStyle = DateFormatStyle.PILL_SHORT_MONTH)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (style1) "✓ Nov 09 2026" else "Nov 09 2026",
                        fontSize = 12.sp,
                        color = if (style1) TimemarkOrange else Color.White
                    )
                }
                OutlinedButton(
                    onClick = { onConfigChange(config.copy(dateFormatStyle = DateFormatStyle.DAY_COMMA_MONTH)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (!style1) "✓ 09 Nov, 2026" else "09 Nov, 2026",
                        fontSize = 12.sp,
                        color = if (!style1) TimemarkOrange else Color.White
                    )
                }
            }
        }

        LocationSummaryCard(location = location, onEdit = onOpenLocationEditor)

        // Asset Catalog Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Katalog Data Acuan Aset", fontWeight = FontWeight.Bold, color = Color.White)
            val displayedCount = ScheduleCalculator.filterOperationalAssets(
                catalog.aset,
                config.onlyResorAssets
            ).size
            Text(
                text = "$displayedCount aset ditampilkan (total katalog: ${catalog.aset.size}) · ${if (isCustomCatalog) "JSON Import Kustom" else "Bawaan (data_acuan_tenaga_gabungan.json)"}",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tampilkan hanya 13 aset aktif resor", fontSize = 13.sp, color = Color.White)
                Switch(
                    checked = config.onlyResorAssets,
                    onCheckedChange = { onConfigChange(config.copy(onlyResorAssets = it)) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onImportCatalogClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TimemarkOrange),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import JSON", fontSize = 12.sp)
                }
                if (isCustomCatalog) {
                    OutlinedButton(
                        onClick = onResetCatalogClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Bawaan", fontSize = 12.sp)
                    }
                }
            }
        }

        // App Credit Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Tentang Aplikasi & Kredit", fontWeight = FontWeight.Bold, color = Color.White)
            Text("Timemark Sintelis v1.0.0", fontSize = 13.sp, color = TimemarkOrange, fontWeight = FontWeight.SemiBold)
            Text("Dikembangkan oleh: Dika Armansyah", fontSize = 12.sp, color = Color.White)
            Text("NIPP: 72347", fontSize = 12.sp, color = TimemarkGold, fontWeight = FontWeight.SemiBold)
            Text("UPT Resor Sintelis 1.21 Bogor", fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun LocationSummaryCard(
    location: LocationState,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lokasi GPS & Alamat", fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                if (location.isManualLocked) "🔒 Terkunci Manual" else if (location.hasGpsFix) "📡 GPS Aktif" else "Default",
                fontSize = 12.sp,
                color = if (location.isManualLocked) TimemarkGold else TimemarkGreen
            )
        }
        Text(location.formattedCoordinates(), fontSize = 12.sp, color = Color.White)
        Text(location.addressLine1, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(location.addressLine2, fontSize = 12.sp, color = Color(0xFF94A3B8))
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Edit / Kunci Lokasi Manual")
        }
    }
}
