---
title: "ADR-001: Isolasi Runtime Node.js Khusus REA"
tags:
  - adr
  - decision
  - tooling
status: accepted
date: 2026-11-11
---

# ADR-001: Isolasi Runtime Node.js Khusus REA

* **Status:** Accepted
* **Tanggal:** 2026-11-11

## Konteks

* `rea-agents@6.8.0` mensyaratkan Node.js versi `^22.23.4 || ^24.22.1 || >=26.12.1`.
* Mesin pengembang saat ini menjalankan Node.js global `v24.16.0` (`C:\Program Files\nodejs\node.exe`) yang juga dipakai oleh proyek lain di luar repositori Timemark.
* Meng-upgrade Node.js secara global berisiko memengaruhi proyek lain (terutama dependensi *native addon* atau kompatibilitas versi).

## Keputusan

Menginstal Node.js **`v24.22.2` LTS** (verifikasi SHA-256 resmi) secara portabel di folder terisolasi `~/.local/node-rea/` dan memasang `rea-agents@6.8.0` di `~/.local/rea-agents/`, lalu mendaftarkan path absolutnya di `C:\Users\dikarm\.gemini\config\mcp_config.json` untuk client `antigravity`.

## Konsekuensi

* **Positif:**
  * Node.js global (`v24.16.0`) tetap utuh dan nol risiko regresi bagi proyek lain.
  * Server MCP `rea` di Antigravity menggunakan instalasi permanen (tidak terhapus pembersihan cache `npx`).
* **Negatif / Catatan:**
  * Pembaruan `rea-agents` di masa depan perlu dijalankan terhadap prefix `~/.local/rea-agents` menggunakan `~/.local/node-rea/npm.cmd`.
  * Engine analisis *native* opsional (Ghidra, IDA, JADX, Kuna) belum dipasang dan dapat ditambahkan terpisah bila dibutuhkan.
