---
title: Backlog Tugas Timemark
tags:
  - tasks
  - backlog
updated: 2026-11-12
---

# Backlog Tugas Timemark

## Selesai (Done)

- [x] Setup REA (`rea-agents@6.8.0`) dengan Node.js `v24.22.2` terisolasi (`~/.local/node-rea`)
- [x] Registrasi MCP server REA untuk Antigravity (`~/.gemini/config/mcp_config.json`) dan skill lokal (`.agents/skills/reverse-engineer-anything/`)
- [x] Penyusunan [AGENTS.md](../AGENTS.md) dan struktur knowledge vault di `vault/` — [[03-Decisions/ADR-001-REA-Isolated-Runtime]]
- [x] Pull APK asli `com.oceangalaxy.camera.new` via ADB dan inspeksi statis REA — [[02-Investigations/INV-001-timemark-apk-rea]]
- [x] Implementasi proyek Android Native `android-app/` (`com.sintelis.timemark`) berbasis Kotlin + Jetpack Compose + CameraX + ExifInterface
- [x] **Perbaikan *Timestamp* Galeri, Rasio `1:1`, & Tipografi:** publikasi salinan *stamped* via `MediaStore`, *center crop* `1:1` sebelum render *watermark*, font `RobotoCondensed`, penghapusan teks bawaan `"Sintelis 1.21"` pada sub-brand, ikon app `ic_launcher`, label `Nama Aset (Optional)`, dan kredit **Dika Armansyah · NIPP 72347** — [[03-Decisions/ADR-002-Android-App-Fixes-And-UX-Decisions]]
- [x] **Navigasi, `BackHandler`, & Pemeriksaan Multi-Foto:** penataan 3 tab (`Foto Langsung` | `Jadwal Baris` | `Pengaturan`), `BackHandler` sistem HP agar kamera kembali ke menu sebelumnya, kamera kembali ke grid setelah foto terjadwal, tombol tambah baris di bawah daftar, serta opsi *toggle* & galeri multi-foto **Pemeriksaan** (`pemeriksaan_N.jpg`, tanpa jam)
- [x] **Perbaikan *Crash* & Optimasi Performa:** normalisasi skema JSON `Gson` (`SessionStore.normalizeLoadedSession`), `ThumbnailCache` (`LruCache` ~16 MB + `Dispatchers.IO` + `RGB_565`), penyimpanan sesi asinkron, filter GPS `< 25 m`, dan `assembleRelease` dengan *debug signing*
- [x] **Pembaruan UI v3 & `Foto Langsung`:** ubah teks `"Siap Foto"` → `"Ambil Foto"`, tombol hapus baris langsung di sebelah ikon pensil, dan mode kamera tertanam pada tab `Foto Langsung` dengan *tap-to-edit* Tanggal (tanpa jam), GPS/Alamat, serta Brand (`BrandEditDialog`)
- [x] Verifikasi unit test (`ScheduleCalculatorTest`: 8/8 passed), build `assembleRelease`, instalasi, dan uji *cold start* (`PID: 17147`, tanpa crash) di perangkat Android (`192.168.0.4:38191`)

## Berikutnya (Next / Uji Lapangan)

- [ ] Uji pengambilan foto langsung (`0%`, `50%`, `100%`, `Pemeriksaan`) dan tab `Foto Langsung` (*tap-to-edit* tanggal/GPS/brand), *rollback* saat hapus foto, serta ekspor `.zip` di perangkat Android
- [ ] Manajemen data dan *cache* aplikasi (ditunda sesuai arahan pengguna)
- [ ] Penyesuaian tampilan atau penambahan fitur lanjutan (mis. integrasi langsung folder `BTP BD`/`BTP JAK` bila diperlukan)
