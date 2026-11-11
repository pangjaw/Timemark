# AGENTS.md — Panduan Operasional Agent (Project Timemark)

Dokumen ini melengkapi [GEMINI.md](GEMINI.md) dengan aturan operasional spesifik untuk **Project Timemark**: riset / *reverse engineering* target Timemark berbasis **REA (*Reverse Engineer Anything*)** serta perancangan dan implementasi server/fiturnya.

---

## 1. Tujuan & Ruang Lingkup Proyek

1. **Investigasi & Analisis Target (REA):** Membedah arsitektur, protokol, API, struktur paket, dan perilaku *runtime* target Timemark secara lokal berbasis bukti (*evidence-first*).
2. **Rekonstruksi & Implementasi Server:** Membangun ulang atau mengembangkan layanan server/fitur yang dibutuhkan secara bersih, teruji, dan minimal (*KISS / YAGNI*).
3. **Dokumentasi Terpusat (`vault/`):** Seluruh temuan investigasi, arsitektur, keputusan teknis, dan backlog tugas dipelihara di folder [`vault/`](vault/00-Index.md).

---

## 2. Lingkungan & Konfigurasi Tooling (Windows)

* **Node.js Global (`v24.16.0`):** Terpasang di `C:\Program Files\nodejs\node.exe`. **Jangan mengubah atau meng-upgrade versi global ini** agar proyek lain tidak terdampak.
* **Runtime Khusus REA (`v24.22.2` terisolasi):**
  * Node executable: `C:\Users\dikarm\.local\node-rea\node.exe`
  * Paket REA (`rea-agents@6.8.0`): `C:\Users\dikarm\.local\rea-agents\node_modules\rea-agents\scripts\rea.mjs`
  * Registrasi MCP Antigravity: `C:\Users\dikarm\.gemini\config\mcp_config.json` (entri `mcpServers.rea`)
  * Skill lokal proyek: [`.agents/skills/reverse-engineer-anything/SKILL.md`](.agents/skills/reverse-engineer-anything/SKILL.md)
* **Shell PowerShell:** Jika eksekusi `npm` / `npx` terblokir `ExecutionPolicy` pada `.ps1`, panggil `npm.cmd` / `npx.cmd` atau gunakan executable absolut di atas.
* **Diagnosis Cepat REA (jika MCP bermasalah):**
  ```powershell
  & "$HOME\.local\node-rea\node.exe" "$HOME\.local\rea-agents\node_modules\rea-agents\scripts\rea.mjs" doctor --client antigravity --skill --json
  ```

---

## 3. Aturan Kerja Reverse Engineering (REA)

1. **Kapan Memakai REA vs Tool Biasa:**
   * Gunakan tool MCP/CLI **REA** hanya saat menyelidiki artefak biner, paket terkompilasi/bundel (APK, ASAR, EXE/PE, ELF, .NET, Web/HAR), hasil dekompilasi, atau perilaku *runtime* yang kode sumbernya tidak tersedia.
   * Untuk kode sumber yang ditulis sendiri di repositori ini, gunakan tool pembacaan/pencarian file biasa—jangan menjalankan *readiness check* REA berulang kali tanpa indikasi error.
2. **Disiplin Bukti (*Evidence-First*):**
   * Setiap kesimpulan teknis hasil REA **wajib** menyertakan bukti teramati (jalur artefak/file, hash/versi, simbol/alamat/endpoint/payload, atau output tool) serta mencatat bagian yang masih belum terkonfirmasi (`unknowns` / batasan analisis).
   * Pisahkan secara tegas antara **fakta teramati** dari target dan **hipotesis/inferensi** agent.
3. **Kepatuhan Hukum & Keamanan:**
   * Analisis hanya dilakukan pada perangkat lunak/target milik sendiri atau yang memiliki izin sah untuk diteliti demi interoperabilitas, riset, dan pemeliharaan sistem.
   * Dilarang keras menyimpan kredensial, token otentikasi, API key, kunci privat, atau data sensitif pengguna dari hasil tangkapan jaringan/ekstraksi ke dalam `vault/` atau kode sumber.

---

## 4. Alur Kerja: Investigasi → Desain → Implementasi

1. **Riset Target:** Jalankan investigasi terarah dengan REA → catat temuan mentah & bukti terverifikasi di [`vault/02-Investigations/`](vault/02-Investigations/README.md).
2. **Pemetaan Arsitektur & Keputusan:** Perbarui [`vault/01-Architecture/Overview.md`](vault/01-Architecture/Overview.md) dan catat keputusan arsitektural penting sebagai ADR di [`vault/03-Decisions/`](vault/03-Decisions/).
3. **Implementasi Terkontrol:** Tulis kode server/fitur secara modular, lintas-platform (*OS agnostic*), dengan cakupan perubahan minimal.
4. **Verifikasi Mandiri:** Jalankan uji sintaks, unit test, atau build check sebelum mengakhiri giliran, lalu perbarui status di [`vault/04-Tasks/Backlog.md`](vault/04-Tasks/Backlog.md).

---

## 5. Sinkronisasi Knowledge Vault (`vault/`)

* Gunakan format Markdown standar yang kompatibel dengan Obsidian (`[[wiki-link]]` atau tautan Markdown relatif).
* Setelah menyelesaikan investigasi REA yang signifikan atau perubahan desain penting, perbarui catatan terkait di `vault/` dan tautkan ke dokumen indeks [`vault/00-Index.md`](vault/00-Index.md).
* Jangan menduplikasi isi kode panjang ke dalam vault; cukup simpan ringkasan kontrak, alur, skema data, serta tautan ke berkas terkait.
