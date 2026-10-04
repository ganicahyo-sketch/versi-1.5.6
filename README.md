# STASIUN CUACA DESA TURUS v1.5.6

Android project lengkap dengan Open-Meteo + Agronomi + Catatan Lapangan.

## Fitur
- Open-Meteo tanpa API key/akun: cuaca saat ini, ET0 FAO, VPD, hujan, radiasi, mata angin.
- ThingSpeak feed terakhir + ekspor seluruh histori CSV.
- CWT-7in1: N, P, K tersedia; pH; EC uS/cm; kelembapan; suhu tanah.
- Catatan Lapangan terpadu: pengamatan, pupuk, OPT, pengendalian, irigasi, pertumbuhan; umur tanaman dapat diinput sebagai HST/hari, minggu, bulan, atau tahun dan dikonversi ke HST.
- Analisis pH, N/P/K, EC, kelembapan, VPD, fase tanaman, kebutuhan hara screening, dan risiko OPT per komoditas yang didukung.
- Mode Konvensional/PHT atau Organik.
- Arah angin selalu ditampilkan sebagai mata angin; tidak menampilkan derajat.

## Build lokal
Workflow CI menggunakan Eclipse Temurin JDK 25; source compatibility Android tetap Java 17. Gradle 9.4.1 + Android Gradle Plugin 9.2.0.

## GitHub Actions
File `.github/workflows/build-apk.yml` membuild debug dan release, lalu mengunggah APK sebagai artifact. Jalankan dari tab Actions > Build APK.

## Catatan ilmiah
Mesin agronomi membedakan keputusan deterministik, screening berbasis ambang, dan rekomendasi yang memerlukan kalibrasi lokal. Dosis pupuk tidak boleh dianggap sebagai resep mutlak ketika persamaan STCR spesifik tanah/komoditas tidak tersedia.

## Laporan PDF
Versi ini menyediakan `LAPORAN PDF - SIMPAN / PRINT` dari dashboard. Pengguna dapat memilih laporan ringkas/lengkap dan menyimpan PDF melalui pemilih file Android atau langsung membuka dialog Print Android.
Isi laporan mencakup cuaca Open-Meteo, ET0, VPD, arah angin berbasis mata angin, tanah CWT-7in1, histori catatan/pupuk/OPT, analisis agronomi, rekomendasi teknis, rekomendasi AI terakhir (jika ada), serta catatan dasar ilmiah.


## v1.5.6 agronomi
- Interpretasi pH berbasis profil komoditas dan kebutuhan kapur berbasis pH-buffer/Al-dd/H-dd/CEC/lab.
- P/K memakai kategori berbasis metode; N hanya diklasifikasikan bila ambang metode tersedia.
- EC sensor dipisahkan dari ECe; kelembapan memakai FC/PWP/FTSW bila tersedia.
- Risk screening OPT menyebut nama organisme; bukan diagnosis.
- Mode konvensional/PHT atau organik.
- Laporan PDF menyertakan data, histori, analisis, rekomendasi, dan sumber.
