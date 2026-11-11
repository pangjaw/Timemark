package com.sintelis.timemark.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sintelis.timemark.camera.WatermarkRenderer
import com.sintelis.timemark.domain.LocationState
import com.sintelis.timemark.domain.PhotoSlot
import com.sintelis.timemark.domain.ScheduleCalculator
import com.sintelis.timemark.domain.ScheduleRow
import com.sintelis.timemark.domain.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

sealed interface CameraMode {
    data class Scheduled(
        val workDateIso: String,
        val row: ScheduleRow,
        val activeSlot: PhotoSlot
    ) : CameraMode

    data class Inspection(
        val workDateIso: String,
        val row: ScheduleRow,
        val photoIndex: Int? = null
    ) : CameraMode

    data class Direct(val presetDateTime: LocalDateTime) : CameraMode
}

@Composable
fun CameraCaptureScreen(
    mode: CameraMode,
    location: LocationState,
    watermarkConfig: WatermarkConfig,
    hasCameraPermission: Boolean,
    showBackButton: Boolean = true,
    onRequestCameraPermission: () -> Unit,
    onBack: () -> Unit,
    onSelectScheduledSlot: (PhotoSlot) -> Unit,
    onEditLocation: () -> Unit,
    onEditTime: () -> Unit,
    onEditDate: (() -> Unit)? = null,
    onEditBrand: (() -> Unit)? = null,
    onDeleteActiveSlot: () -> Unit,
    onPhotoCaptured: suspend (stampedBitmap: Bitmap, presetDateTime: LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isCapturing by remember { mutableStateOf(false) }

    val activePresetDateTime = when (mode) {
        is CameraMode.Scheduled ->
            ScheduleCalculator.slotDateTime(mode.workDateIso, mode.row, mode.activeSlot)
        is CameraMode.Inspection ->
            ScheduleCalculator.slotDateTime(mode.workDateIso, mode.row, PhotoSlot.INSPECTION)
        is CameraMode.Direct -> mode.presetDateTime
    }
    val modeRequiresTime = when (mode) {
        is CameraMode.Scheduled -> mode.activeSlot.requiresTime
        is CameraMode.Inspection -> false
        is CameraMode.Direct -> false
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember(flashMode) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
    }

    DisposableEffect(hasCameraPermission, flashMode) {
        if (!hasCameraPermission) {
            onDispose { }
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            providerFuture.addListener({
                runCatching {
                    val cameraProvider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                }.onFailure { err ->
                    Toast.makeText(context, "Kamera error: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }, executor)
            onDispose {
                runCatching { providerFuture.get().unbindAll() }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Header + Slot Chips (Scheduled Mode) or Direct Mode Hint
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CanvasDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    when (mode) {
                        is CameraMode.Scheduled -> {
                            Text(
                                "${mode.row.orderNumber}. ${mode.row.labelName}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is CameraMode.Inspection -> {
                            val suffix = mode.photoIndex?.let { "#${it + 1}" } ?: "#${mode.row.allInspectionPhotos().size + 1}"
                            Text(
                                "${mode.row.orderNumber}. ${mode.row.labelName} · Periksa $suffix",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is CameraMode.Direct -> {
                            Column {
                                Text(
                                    "Foto Langsung",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Ketuk Tanggal, GPS/Alamat, atau Brand di template untuk edit",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (mode is CameraMode.Scheduled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TimemarkOrange.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${mode.row.effectiveDurationMinutes()} mnt",
                                color = TimemarkOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (mode is CameraMode.Inspection) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TimemarkGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Tanpa Jam",
                                color = TimemarkGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (mode is CameraMode.Scheduled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhotoSlot.ordered().forEach { slot ->
                        val isActive = slot == mode.activeSlot
                        val isCaptured = mode.row.photoFor(slot) != null
                        val slotTime = ScheduleCalculator.formatTimeHHmm(
                            ScheduleCalculator.slotDateTime(mode.workDateIso, mode.row, slot)
                        )
                        val bg = when {
                            isActive -> TimemarkOrange
                            isCaptured -> TimemarkGreen.copy(alpha = 0.22f)
                            else -> SurfaceDark
                        }
                        val border = when {
                            isActive -> TimemarkGold
                            isCaptured -> TimemarkGreen.copy(alpha = 0.6f)
                            else -> Color(0xFF334155)
                        }
                        val prefix = when {
                            isCaptured -> "✓"
                            isActive -> "●"
                            else -> "○"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(10.dp))
                                .clickable { onSelectScheduledSlot(slot) }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$prefix ${slot.label} · $slotTime",
                                color = if (isActive) Color.White else if (isCaptured) TimemarkGreen else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Viewfinder + Live Watermark Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasCameraPermission) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Izin kamera diperlukan untuk mengambil foto",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = onRequestCameraPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = TimemarkOrange)
                    ) {
                        Text("Berikan Izin Kamera")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    val badgeLabel = when (mode) {
                        is CameraMode.Scheduled -> "Slot ${mode.activeSlot.label}"
                        is CameraMode.Inspection -> "Pemeriksaan"
                        is CameraMode.Direct -> null
                    }
                    WatermarkPreviewOverlay(
                        presetDateTime = activePresetDateTime,
                        location = location,
                        config = watermarkConfig,
                        slotBadgeText = badgeLabel,
                        includeTime = modeRequiresTime,
                        onClickDate = if (mode is CameraMode.Direct) onEditDate else null,
                        onClickLocation = onEditLocation,
                        onClickBrand = onEditBrand
                    )
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CanvasDark)
                .padding(horizontal = 16.dp, vertical = if (mode is CameraMode.Direct) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (mode !is CameraMode.Direct) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CameraUtilityButton(
                        icon = Icons.Filled.EditLocationAlt,
                        label = if (location.isManualLocked) "Lokasi 🔒" else "Edit Lokasi",
                        onClick = onEditLocation,
                        modifier = Modifier.weight(1f)
                    )
                    CameraUtilityButton(
                        icon = Icons.Filled.Schedule,
                        label = if (modeRequiresTime) "Ubah Jam" else "Tanpa Jam",
                        tint = if (modeRequiresTime) Color.White else Color(0xFF64748B),
                        enabled = modeRequiresTime,
                        onClick = onEditTime,
                        modifier = Modifier.weight(1f)
                    )
                    if (mode is CameraMode.Scheduled) {
                        val slotHasPhoto = mode.row.photoFor(mode.activeSlot) != null
                        CameraUtilityButton(
                            icon = Icons.Filled.Delete,
                            label = "Hapus Slot",
                            tint = if (slotHasPhoto) TimemarkRed else Color(0xFF64748B),
                            enabled = slotHasPhoto,
                            onClick = onDeleteActiveSlot,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                            else -> ImageCapture.FLASH_MODE_AUTO
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    val flashIcon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                        ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                        else -> Icons.Filled.FlashAuto
                    }
                    Icon(flashIcon, contentDescription = "Flash", tint = Color.White)
                }

                // Shutter button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .background(if (isCapturing) Color.Gray else TimemarkOrange)
                        .clickable(enabled = hasCameraPermission && !isCapturing) {
                            isCapturing = true
                            val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                            val options = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                            imageCapture.takePicture(
                                options,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        scope.launch {
                                            try {
                                                val stamped = withContext(Dispatchers.Default) {
                                                    WatermarkRenderer.decodeAndStamp(
                                                        context = context,
                                                        rawFile = tempFile,
                                                        presetDateTime = activePresetDateTime,
                                                        location = location,
                                                        config = watermarkConfig,
                                                        squareCrop = true,
                                                        includeTime = modeRequiresTime
                                                    )
                                                }
                                                onPhotoCaptured(stamped, activePresetDateTime)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Gagal memproses foto: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                tempFile.delete()
                                                isCapturing = false
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        tempFile.delete()
                                        isCapturing = false
                                        Toast.makeText(
                                            context,
                                            "Gagal mengambil foto: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            "FOTO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Empty spacer to balance shutter layout
                Box(modifier = Modifier.size(46.dp))
            }
        }
    }
}

@Composable
private fun CameraUtilityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = if (enabled) tint else Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
