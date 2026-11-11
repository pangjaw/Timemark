---
title: "ADR-002: Pembaruan UX Kamera, Foto Pemeriksaan, Stabilitas Gson & Optimasi Performa"
tags:
  - adr
  - decision
  - android
  - bugfix
status: accepted
date: 2026-11-12
---

# ADR-002: Pembaruan UX Kamera, Foto Pemeriksaan, Stabilitas Gson & Optimasi Performa

* **Status:** Accepted
* **Tanggal:** 2026-11-12

## Konteks

Setelah rilis awal `com.sintelis.timemark`, pengujian langsung di perangkat Android mengungkapkan beberapa bug serta kebutuhan penyesuaian alur kerja lapangan UPT Resor Sintelis 1.21 Bogor:
1. Foto yang dibuka dari galeri Android awalnya tampak tidak memuat salinan terbaru/terstampel atau tersimpan dalam rasio penuh yang memotong tepi overlay, sedangkan kebutuhan dokumentasi memerlukan bingkai persegi **`1:1`**.
2. Template *watermark* perlu menyamai font `RobotoCondensed` dari APK target (`INV-001`) tanpa teks bawaan `"Sintelis 1.21"` pada sub-brand, dilengkapi ikon aplikasi khusus (`ic_launcher`), label input `Nama Aset (Optional)`, dan kredit pembuat **Dika Armansyah · NIPP 72347**.
3. Di lapangan, tombol *Back* sistem HP saat membuka kamera sebelumnya menutup/keluar aplikasi alih-alih kembali ke menu sebelumnya, dan setelah memotret pengguna ingin memilih kolom tujuan secara sadar di menu baris aset.
4. Diperlukan mode foto **Pemeriksaan** per baris (opsional *toggle* di bawah `0% / 50% / 100%`, bisa lebih dari 1 foto, **tanpa jam**).
5. Saat pembaruan skema `timemark_session.json`, deserialisasi `Gson` melalui `Unsafe` dapat menyisipkan nilai `null` ke properti *non-nullable* Kotlin (seperti `inspectionPhotos` atau `watermarkConfig`) sehingga memicu *crash* saat aplikasi dibuka.
6. *Build* debug dan *decoding bitmap* sinkron di *main thread* serta pembaruan GPS tanpa batas jarak menyebabkan UI terasa lambat (*lemot*).
7. Diperlukan percepatan akses kamera bebas lewat tab **`Foto Langsung`** (langsung membuka viewfinder di tab dengan *tap-to-edit* elemen *watermark* tanggal/GPS-alamat/brand) serta tombol hapus baris langsung dan teks `"Ambil Foto"`.

## Keputusan

1. **Rasio `1:1` & Publikasi Galeri `MediaStore` (`WatermarkRenderer`, `PhotoStorageManager`):**
   * [`CameraCaptureScreen.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/CameraCaptureScreen.kt) mengunci *viewfinder* ke `aspectRatio(1f)`, dan [`WatermarkRenderer.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/camera/WatermarkRenderer.kt) menerapkan *center crop* `1:1` **sebelum** menggambar overlay agar pill GPS dan blok tanggal tetap presisi di dalam tepi foto.
   * [`PhotoStorageManager.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/PhotoStorageManager.kt) menghapus entri `MediaStore` lama sebelum menulis ulang slot yang sama dan memublikasikan JPEG bertimestamp lewat `MediaStore` (`IS_PENDING` pada Android Q+).
2. **Tipografi `RobotoCondensed`, Branding Bersih & Kredit:**
   * Font `roboto_condensed_regular.ttf`, `roboto_condensed_medium.ttf`, dan `roboto_condensed_bold.ttf` dibundel di `res/font/`.
   * `WatermarkConfig.brandSubText` default dikosongkan (`""`), input label pada `AddRowDialog` diberi keterangan `Nama Aset (Optional)`, aset ikon launcher di-generate ke `mipmap-*`, dan kredit **Dika Armansyah · NIPP 72347** ditampilkan di layar utama, dialog info, dan Pengaturan.
3. **Navigasi 3 Tab, `BackHandler`, & Tab `Foto Langsung`:**
   * [`MainActivity.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/MainActivity.kt) menempatkan **`Foto Langsung`** (`GridView`) di kiri, **`Jadwal Baris`** (`CameraAlt`) di tengah sebagai tab default, dan **`Pengaturan`** di kanan.
   * `BackHandler(enabled = activeCamera != null || currentTab != MainTab.SCHEDULE)` menangani tombol *Back* sistem agar menutup kamera lebih dulu atau kembali ke `Jadwal Baris`.
   * Pada mode `Scheduled` & `Inspection`, kamera menutup otomatis (`activeCamera = null`) setelah setiap foto; pada `Foto Langsung` (`CameraMode.Direct`), kamera tertanam langsung di tab, menggunakan format tanggal tanpa jam (`includeTime = false`), menyimpan ke `Pictures/Timemark/FotoLangsung/<Tanggal>/TIMEMARK_<YYYYMMDD>_<HHmmss_SSS>.jpg`, dan memungkinkan klik langsung pada pill Tanggal, GPS/Alamat, atau Brand di [`WatermarkPreviewOverlay.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/WatermarkPreviewOverlay.kt) (`BrandEditDialog`, `LocationEditDialog`, `DatePickerDialog`).
4. **Mode Pemeriksaan Multi-Foto (Tanpa Jam):**
   * [`ScheduleGridScreen.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/ui/ScheduleGridScreen.kt) menambahkan *toggle* **"Tambah Foto Pemeriksaan"** di bawah baris `0% / 50% / 100%` beserta deretan foto horizontal + tombol tambah (`pemeriksaan_N.jpg`). Slot `INSPECTION` tidak memengaruhi rantai jam `ScheduleCalculator.rechainRows(...)`.
   * Label sel target diubah menjadi `"Ambil Foto"` / `"Ambil Foto Pemeriksaan"`, dan ikon `Icons.Filled.Delete` ditambahkan di sebelah `Icons.Filled.Edit` untuk hapus baris langsung.
5. **Stabilitas Startup & Optimasi Performa:**
   * [`SessionStore.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/SessionStore.kt) menambahkan `normalizeLoadedSession()` (migrasi ke `watermarkSchemaVersion = 2`) dan [`AssetModels.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/domain/AssetModels.kt) menyediakan `safePhotos()` / `allInspectionPhotos()` agar JSON sesi lama tidak menyebabkan `NullPointerException`.
   * [`ThumbnailCache.kt`](../../android-app/app/src/main/java/com/sintelis/timemark/data/ThumbnailCache.kt) memindahkan decoding *thumbnail* ke `Dispatchers.IO` dengan `inSampleSize` + `RGB_565` dan cache `LruCache` (~16 MB); `SessionStore.save()` menulis file JSON secara asinkron di `ioExecutor`; pembaruan GPS disaring ambang jarak `25 m`.
   * [`app/build.gradle.kts`](../../android-app/app/build.gradle.kts) menandatangani build `release` dengan `signingConfigs.getByName("debug")` sehingga instalasi harian menggunakan `assembleRelease` yang lebih responsif dibanding debug.

## Konsekuensi

* Upgrade antar versi APK (`app-release.apk`) tetap mempertahankan `timemark_session.json` tanpa *crash* skema lama dan tanpa harus *uninstall*.
* Fitur manajemen pembersihan data & cache secara manual di UI sengaja ditunda sesuai arahan pengguna ([[04-Tasks/Backlog]]).
* Pengambilan foto di *viewfinder* `1:1` menghasilkan file persegi konsisten antara *live preview*, hasil foto internal, salinan galeri, dan paket ekspor `.zip`.

## Tautan Terkait

* [[00-Index]]
* [[01-Architecture/Overview]]
* [[04-Tasks/Backlog]]
