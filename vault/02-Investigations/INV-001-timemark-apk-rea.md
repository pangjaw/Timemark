---
title: "INV-001: Analisis Statis APK com.oceangalaxy.camera.new (Timemark)"
tags:
  - investigation
  - rea
  - android
status: completed
date: 2026-11-12
target: "com.oceangalaxy.camera.new"
---

# INV-001: Analisis Statis APK `com.oceangalaxy.camera.new` (Timemark)

## 1. Tujuan & Pertanyaan Riset

* Memastikan keaslian artefak APK target yang dianalisis.
* Mengidentifikasi arsitektur aplikasi kamera `com.oceangalaxy.camera.new`, skema *template* watermark, font, dan format penamaan/layout yang setara dengan sampel foto di `IMO/2025/DONE NEW/FOTO/APRIL`.

---

## 2. Identitas Target & Alat

### A. Artefak Awal (Stub Installer)
* **File:** `uptodown-com.oceangalaxy.camera.new.apk` (`15,790,385` bytes / `15.06 MB`)
* **SHA-256:** `8f8a7b2416d84d64d418dc4e194cd8ab6f4394d51651bf2e1aac47207503fd92`
* **Alat REA:** `rea inspect-apk-signature`, `rea inspect-artifact`

### B. Paket Asli dari Perangkat (`adb pull` via `192.168.0.4:38191`)
* `targets/com.oceangalaxy.camera.new/base.apk` (`110.85 MB`) — SHA-256: `45ddfcecd097aa3c3b557f43cab42df9bdc2c5e3709475bdeb2ed2b621de9893`
* `targets/com.oceangalaxy.camera.new/split_config.arm64_v8a.apk` (`17.79 MB`) — SHA-256: `99674ea5b284f729d84153f9edda3698771ff892d2e2e67bf1862d0fa8361ea6`
* `split_config.en.apk` (`0.66 MB`), `split_config.in.apk` (`0.85 MB`), `split_config.xxhdpi.apk` (`0.86 MB`)
* **Alat REA:** `rea inspect-apk-signature`, `rea identify-flutter-build`, `rea inspect-artifact`

---

## 3. Observed Evidence (Fakta Teramati)

1. **`uptodown-com.oceangalaxy.camera.new.apk` adalah Stub Uptodown:**
   * Signer deklarasi (`v1`/`v2`/`v3`): `CN=Uptodown Technologies SL, O=Uptodown`.
   * Native libraries: `libuptodown-native.so`, `libutd-services-native.so`, `libdatastore_shared_counter.so`. Tidak memuat aset/kode Timemark.
2. **Arsitektur `base.apk` & `split_config.arm64_v8a.apk` Asli:**
   * Signer deklarasi (`v1`/`v2`/`v3`, `strongest_scheme: v3`): `O=Google Inc., OU=Android, CN=Android` (`sha256_fingerprint: 96fcc4571a3c269d0bae4ca6fa786d236d8b61a691d58406c3805996f53f123f`).
   * `flutter_detected: false` (`identify-flutter-build`).
   * Native libraries di `split_config.arm64_v8a.apk`: `libreactnative.so`, `libhermes.so`, `libjsi.so`, `libreact_codegen_rnsvg.so`, `libxheymediav8.so`, `libxhey-plog.so`, `libimagepipeline.so`, `libmodpdfium.so`, serta pelindung `libDexHelper.so` & `libInnoSecure.so`.
   * Aset JS bundle di `base.apk`: `assets/index.android.bundle` (`828,316` bytes), `assets/verify_auth.android.jsbundle` (`1,853,832` bytes), `assets/questionaire.android.jsbundle`.
3. **Skema Template Watermark & Font di `base.apk`:**
   * `assets/watermark/default_watermarks_global.json` (15 entri) dan `assets/watermark/supported_watermarks_template_global.json` (16 entri).
   * Template `id="34"` (**`Time&Location`**) & `id="10"` (**`Custom Text`**) menggunakan warna tema `#FFC233`, teks `#FFFFFF`, dan item: `Time` (`id=1`), `Address` (`id=2`), `Lat/Long` (`id=3`), `Notes` (`id=12`), `Tags` (`id=630`), `Business Card` (`id=620`), `Map` (`id=210`).
   * Font yang dibundel di `assets/fonts/global/`: keluarga `RobotoCondensed-{Regular,Medium,Bold}.ttf`, `Montserrat-Black.ttf`, `PTMono-{Regular,Bold}.ttf`, `BigShouldersText-Medium.ttf`, `Handlee-Regular.ttf`, `BebasDaka.ttf`, `XiaoHeiNumber.ttf`.
   * Contoh penamaan file bawaan: `assets/2025-04-25 12.18.34_Gg Pandega Padma No. 6B_(Perusahaan-Timemark)_(Nama-Elvis).jpg`.
4. **Kesesuaian dengan Sampel Foto Lapangan (`APRIL`):**
   * Sampel `03_export_foto` & `04_photos_edited` menampilkan: pill koordinat kiri atas (`-6.656495, 106.810217`), garis vertikal oranye + pill gelap tanggal/jam (`Senin, Nov 09 2026 22:02`) + alamat 2 baris di kiri bawah, serta branding di kanan bawah.

---

## 4. Analisis & Sintesis

* Aplikasi `com.oceangalaxy.camera.new` dibangun menggunakan **React Native + Hermes** yang dipadukan dengan engine kamera/render media native (`libxheymediav8.so`), dengan definisi watermark berbasis JSON deklaratif.
* Untuk kebutuhan internal **UPT Resor Sintelis 1.21 Bogor** (yang membutuhkan penjadwalan otomatis `0% / 50% / 100%` per aset dan kontrol jam manual/rollback), membangun aplikasi Android Native (**Kotlin + CameraX + Canvas + ExifInterface**) jauh lebih ringan dan deterministik tanpa perlu memodifikasi APK pihak ketiga yang diproteksi `libDexHelper.so`.

---

## 5. Unknowns & Batasan

* `classes*.dex` dilindungi packer `libDexHelper.so`, dan `index.android.bundle` dikompilasi/dibundel sehingga logika bisnis internal di balik protokol *network-verified timestamp* server Timemark tidak didekompilasi pada tahap ini (tidak dibutuhkan karena aplikasi baru berjalan offline/mandiri untuk dokumentasi internal).
* Verifikasi tanda tangan APK hanya bersifat struktural (`matches_apk: true`), bukan validasi rantai sertifikat Play Store penuh.

---

## 6. Implikasi untuk Implementasi

* Layout watermark pada aplikasi `android-app/` meniru komposisi visual teramati di `APRIL` + struktur komponen template `id=34` (*GPS pill* kiri atas, garis aksen oranye + *timestamp pill* + alamat 2 baris di kiri bawah, font `sans-serif-condensed` / `RobotoCondensed`, dan teks branding kanan bawah yang dapat dikustomisasi).
