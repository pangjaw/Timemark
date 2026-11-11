---
title: Panduan Log Investigasi REA
tags:
  - rea
  - investigations
updated: 2026-11-11
---

# Log Investigasi REA

Folder ini menyimpan seluruh hasil investigasi *reverse engineering* menggunakan REA (`rea-agents@6.8.0`).

## Standar Penamaan Berkas

Gunakan format berurutan agar riwayat riset mudah ditelusuri:

```text
INV-001-<topik-singkat>.md
```

Contoh: `INV-001-apk-auth-flow.md`, `INV-002-api-signature-scheme.md`.

## Aturan Pencatatan

1. Mulai setiap catatan baru dengan menyalin [[_Template-Investigation]].
2. Catat **identitas artefak** (path, ukuran, atau SHA-256) sebelum menyimpulkan perilaku.
3. Pisahkan bagian **Observed Evidence** (fakta dari output tool REA) dari **Analysis / Hypothesis**.
4. Daftarkan seluruh **Unknowns & Limitations** yang masih terbuka.
5. **Dilarang** menempelkan token rahasia, cookie produksi, password, atau API key ke dalam catatan investigasi.
