---
title: Timemark Knowledge Vault Index
tags:
  - index
  - moc
updated: 2026-11-12
---

# Timemark Knowledge Vault

Pusat dokumentasi teknis, catatan investigasi *reverse engineering* (REA), keputusan arsitektur (ADR), dan pelacakan tugas untuk **Project Timemark**.

* **Pemilik / Kredit Proyek:** **Dika Armansyah** · **NIPP 72347** (UPT Resor Sintelis 1.21 Bogor)

> [!TIP]
> Folder `vault/` ini dapat dibuka langsung sebagai **Obsidian Vault** (*Open folder as vault*) maupun dibaca sebagai Markdown standar di editor/Git.

---

## Peta Navigasi

| Bagian | Isi | Lokasi |
| :--- | :--- | :--- |
| **01 · Arsitektur** | Pemetaan sistem target & desain aplikasi Android Timemark | [[01-Architecture/Overview]] |
| **02 · Investigasi REA** | Panduan, template, & log temuan berbasis bukti | [[02-Investigations/README]] |
| **03 · Keputusan (ADR)** | Catatan keputusan teknis, UX kamera, & perbaikan bug | [[03-Decisions/ADR-001-REA-Isolated-Runtime]] · [[03-Decisions/ADR-002-Android-App-Fixes-And-UX-Decisions]] |
| **04 · Tugas & Backlog** | Daftar riset, implementasi, bug fix, & status progres | [[04-Tasks/Backlog]] |

---

## Aturan Proyek Terkait

* Pedoman umum agent: [GEMINI.md](../GEMINI.md)
* Panduan operasional REA & Vault: [AGENTS.md](../AGENTS.md)
* Skill REA lokal: [SKILL.md](../.agents/skills/reverse-engineer-anything/SKILL.md)

---

## Referensi Perintah Cepat (REA Terisolasi)

```powershell
# Cek kesehatan MCP & skill REA untuk Antigravity
& "$HOME\.local\node-rea\node.exe" "$HOME\.local\rea-agents\node_modules\rea-agents\scripts\rea.mjs" doctor --client antigravity --skill --json

# Uji adapter stdio MCP secara langsung
& "$HOME\.local\node-rea\node.exe" "$HOME\.local\rea-agents\node_modules\rea-agents\scripts\rea.mjs" mcp doctor --json
```
