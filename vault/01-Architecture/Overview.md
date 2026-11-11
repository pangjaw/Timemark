---
title: Arsitektur Sistem Timemark
tags:
  - architecture
  - overview
status: active
updated: 2026-11-12
---

# Arsitektur Sistem Timemark

Dokumen ini memisahkan secara jelas antara **perilaku target yang diamati via REA** dan **desain aplikasi Android milik proyek ini**.

---

## 1. Ringkasan Target (Observed via REA)

* **Target:** `com.oceangalaxy.camera.new` (`base.apk` + `split_config.*.apk`, detail di [[02-Investigations/INV-001-timemark-apk-rea]]).
* **Stack Teramati:** React Native (`libreactnative.so`, `libhermes.so`) + engine media native (`libxheymediav8.so`) + proteksi `libDexHelper.so`.
* **Struktur Watermark Teramati:** JSON template `Time&Location` (`id=34`) + font `RobotoCondensed`; tata letak visual pada sampel `APRIL`: pill GPS kiri atas, bar oranye + pill jam + alamat 2 baris kiri bawah, branding kanan bawah.
* **Unknowns:** Logika internal di balik `libDexHelper.so` dan *network-verified timestamp* server Timemark tidak dibedah karena tidak diperlukan untuk app mandiri.

---

## 2. Arsitektur Aplikasi Android (`android-app/`)

* **Stack & Runtime:** Native Android **Kotlin + Jetpack Compose (Material 3) + CameraX + ExifInterface + Gson**, `minSdk 26`, `targetSdk 36`, `compileSdk 37` (Android SDK lokal; varian `release` ditandatangani dengan konfigurasi *debug* agar kompatibel dengan instalasi pembaruan langsung dan mengaktifkan optimasi runtime Compose).
* **Struktur Modul (`com.sintelis.timemark`):**
  * `domain/`
    * [`AssetModels.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/domain/AssetModels.kt) — Model katalog 38 aset, slot jadwal (`ZERO`, `FIFTY`, `HUNDRED`, dan `INSPECTION` tanpa jam), `ScheduleRow` (dengan `includeInspection` & multi-foto `inspectionPhotos`), `WatermarkConfig` (`brandSubText` default kosong `""`), dan `SessionState` (`watermarkSchemaVersion = 2`).
    * [`ScheduleCalculator.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/domain/ScheduleCalculator.kt) — Perhitungan durasi aset (termasuk kasus khusus `Serat Optik` = `jumlahCore × 6` menit), `rechainRows`, slot `0% / 50% / 100%`, *rollback* slot saat hapus foto, serta pemformatan tanggal-jam (`formatWatermarkDateTime`) dan tanggal saja (`formatWatermarkDateOnly`).
  * `data/`
    * [`AssetCatalogRepository.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/AssetCatalogRepository.kt) — Pemuatan `data_acuan_tenaga_gabungan.json` bawaan maupun import JSON kustom.
    * [`SessionStore.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/SessionStore.kt) — Persistensi `timemark_session.json` asinkron via `ioExecutor` (penulisan atomik `.tmp` → rename) serta `normalizeLoadedSession()` untuk mencegah *crash* akibat *null field* dari deserialisasi `Gson` (`Unsafe`) pada skema lama dan membersihkan `brandSubText == "Sintelis 1.21"` saat migrasi ke `watermarkSchemaVersion = 2`.
    * [`ThumbnailCache.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/ThumbnailCache.kt) — Cache *in-memory* `LruCache` (~16 MB) dengan decoding latar belakang `Dispatchers.IO`, `inSampleSize`, dan `Bitmap.Config.RGB_565` (`rememberAsyncBitmap`) untuk menjaga kelancaran scroll grid/dialog.
    * [`PhotoStorageManager.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/PhotoStorageManager.kt) — Penyimpanan JPEG + metadata EXIF (`DateTimeOriginal`, koordinat GPS), publikasi galeri Android lewat `MediaStore` (`IS_PENDING` Android Q+ / `MEDIA_SCANNER` fallback), struktur folder `Pictures/Timemark/<Tanggal>/<NN_NamaAset>/{0,50,100}.jpg` & `pemeriksaan_N.jpg`, mode langsung `Pictures/Timemark/FotoLangsung/<Tanggal>/TIMEMARK_<YYYYMMDD>_<HHmmss_SSS>.jpg`, serta ekspor `.zip` beserta `schedule_export.json`.
  * `location/`
    * [`LocationController.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/location/LocationController.kt) — GPS (`LocationManager`) + `Geocoder` Indonesia 2 baris + mode edit/kunci lokasi manual, disertai filter jarak `< 25 m` di [`MainActivity.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/MainActivity.kt) untuk mencegah *recompose* & panggilan `Geocoder` berulang.
  * `camera/`
    * [`WatermarkRenderer.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/camera/WatermarkRenderer.kt) — Koreksi orientasi EXIF, *center crop* rasio **`1:1`** sebelum rendering, dukungan `includeTime` (`true` untuk `0/50/100%`, `false` untuk `Pemeriksaan` & `Foto Langsung`), dan penggunaan font `RobotoCondensed` (`res/font/roboto_condensed_*.ttf`).
  * `ui/`
    * **Navigasi 3 Tab ([`MainActivity.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/MainActivity.kt)):** Kiri = **`Foto Langsung`** (`Icons.Filled.GridView`), Tengah (default) = **`Jadwal Baris`** (`Icons.Filled.CameraAlt`), Kanan = **`Pengaturan`** (`Icons.Filled.Settings`), dilengkapi `BackHandler` sistem HP untuk menutup layar kamera/kembali ke tab `Jadwal Baris`.
    * **Jadwal Baris ([`ScheduleGridScreen.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/ScheduleGridScreen.kt)):** Kartu baris aset (`0% | 50% | 100%`), label tombol `"Ambil Foto"`, tombol hapus baris langsung (`Icons.Filled.Delete`) di sebelah tombol edit, opsi *toggle* **"Tambah Foto Pemeriksaan"** di bawah baris `0/50/100` dengan galeri horizontal multi-foto (`pemeriksaan_N.jpg`), tombol tambah baris tambahan di bagian bawah daftar, serta dialog kredit pembuat (**Dika Armansyah · NIPP 72347**).
    * **Kamera & Live Template ([`CameraCaptureScreen.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/CameraCaptureScreen.kt), [`WatermarkPreviewOverlay.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/WatermarkPreviewOverlay.kt), [`Dialogs.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/Dialogs.kt)):** *Viewfinder* `1:1`; pada mode `Scheduled` & `Inspection` kamera otomatis kembali ke grid `Jadwal Baris` setelah pemotretan; pada mode `Foto Langsung` (`CameraMode.Direct`) kamera tetap terbuka di dalam tab dan elemen *watermark* (Tanggal tanpa jam, GPS/Alamat, Brand) dapat diketuk langsung untuk diedit (`BrandEditDialog`, `LocationEditDialog`, `DatePickerDialog`).

---

## Tautan Terkait

* [[00-Index]]
* [[02-Investigations/INV-001-timemark-apk-rea]]
* [[03-Decisions/ADR-001-REA-Isolated-Runtime]]
* [[03-Decisions/ADR-002-Android-App-Fixes-And-UX-Decisions]]
* [[04-Tasks/Backlog]]
