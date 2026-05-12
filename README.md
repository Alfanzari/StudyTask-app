# StudyTask 📚

Aplikasi manajemen tugas untuk pelajar dan mahasiswa berbasis Android.

| | |
|---|---|
| **Nama** | Syauqi Al Fanzari |
| **NIM** | 607062400008 |
| **Kelas** | D3IF-48-03 |

---

## Deskripsi

**StudyTask** adalah aplikasi task manager yang dirancang khusus untuk membantu pelajar dan mahasiswa mengelola tugas sehari-hari. Aplikasi ini memungkinkan pengguna untuk menambah, mengedit, dan menghapus tugas dengan kategori, prioritas, dan due date. Dilengkapi fitur Pomodoro Timer untuk sesi belajar terfokus, statistik progress harian, streak aktivitas, grafik mingguan, serta export data ke CSV. Preferensi pengguna seperti dark mode dan nama disimpan menggunakan DataStore, sedangkan data tugas disimpan secara lokal menggunakan Room Database.

---

## Fitur

- ✅ Tambah, edit, dan hapus tugas dengan konfirmasi
- 🏷️ Kategori tugas (Kuliah, Kerja, Personal, Belanja, Kesehatan, Umum)
- 🚦 Prioritas tugas (High / Medium / Low)
- 📅 Due date dengan countdown deadline di card tugas
- 🍅 Pomodoro Timer (25 menit fokus + 5 menit istirahat) dengan notifikasi
- 📊 Statistik progress dengan donut chart dan grafik mingguan
- 🔥 Streak aktivitas harian
- 🔍 Search dan filter tugas berdasarkan prioritas & kategori
- 🌙 Dark mode yang tersimpan otomatis
- 👤 Setup dan edit nama pengguna
- 📤 Export data tugas ke file CSV
- 🎉 Animasi konfetti saat semua tugas selesai

---

## Teknologi yang Digunakan

- **Kotlin** + **Jetpack Compose**
- **Room Database** (migrasi v1 → v5)
- **DataStore Preferences**
- **Kotlin Coroutines**
- **Material Design 3**
- **Konfetti** - animasi konfetti

---

## Cara Install

1. Clone repository ini
```bash
   git clone https://github.com/syauqialfanzari0008/StudyTask.git
```
2. Buka project dengan **Android Studio Panda**
3. Tunggu Gradle sync selesai
4. Build dan jalankan di emulator atau device fisik
    - Minimum SDK: **API 26 (Android 8.0)**
    - Target SDK: **API 34**

---

## Referensi

- [Room Database](https://developer.android.com/training/data-storage/room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Jetpack Compose](https://developer.android.com/jetpack/compose/documentation)
- [Kotlin Coroutines](https://developer.android.com/kotlin/coroutines)
- [Konfetti Library](https://github.com/DanielMartinus/Konfetti)