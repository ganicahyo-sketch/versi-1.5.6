# STASIUN CUACA DESA TURUS v1.5.6 — Open-Meteo + Agronomi

Patch ini memperluas v1.5.3 dengan mesin analisis agronomi terpadu.

## Perubahan utama
- Catatan Lapangan menjadi timeline terpadu untuk pengamatan, pemupukan, OPT, tanah, air, cuaca, tindakan, dan hasil.
- Pilihan budidaya: Konvensional/PHT atau Organik.
- Tanggal tanam + HST otomatis memprediksi fase tanaman menggunakan crop profile.
- Input CWT-7in1 RS485/USB: pH, kelembapan, suhu tanah, EC µS/cm, N, P, K.
- Interpretasi pH: aman/masam/alkalis + tindakan; dosis kapur tidak ditebak dari pH saja, dianjurkan pH-buffer/kemasaman/Al-dd.
- Interpretasi N/P/K: rendah/sedang/tinggi + kecukupan screening terhadap komoditas/fase.
- EC: screening rendah/cukup/tinggi dengan konversi µS/cm → dS/m dan peringatan bahwa ECe laboratorium tidak identik dengan EC sensor.
- Kelembapan tanah: kering/cenderung kering/aman/lembap tinggi/terlalu lembap.
- VPD: umumnya aman/waspada/tinggi berdasarkan fisiologi tanaman.
- Cuaca: suhu, RH, hujan, ET0, tekanan, cahaya, PAR, lama penyinaran, angin dan kecocokan ekologi komoditas.
- Prediksi OPT berbasis cuaca + histori dengan nama organisme, antara lain rice blast, wereng batang cokelat, penggerek batang, fall armyworm, bulai, antraknosa, thrips, late blight, early blight, karat kedelai.
- Model degree-day untuk fase/serangga serta STCR-style nutrient balance untuk screening kebutuhan pupuk fase.
- Catatan satu paragraf dengan kisi-kisi pengisian agar NLP/AI dapat membaca pola kejadian.
- Konteks AI diperkuat dengan evidence brief dan histori Catatan Lapangan.
- Arah angin ditampilkan sebagai 16 mata angin + Tenang/Variabel, tanpa derajat.

## Batas penting
Ambang N/P/K tersedia tidak universal karena metode ekstraksi, tanah, dan komoditas berbeda. Ambang di mesin adalah screening. Rekomendasi final mengutamakan PUTS, rekomendasi P/K spesifik lokasi, petak omisi, analisis laboratorium, atau persamaan STCR yang telah dikalibrasi.

EC sensor lapang juga bukan pengganti ECe saturation paste. Gunakan terutama untuk screening dan tren.

Dosis kapur/dolomit tidak boleh ditentukan dari pH saja. Uji buffer/kemasaman/Al-dd/CEC tetap prioritas.

Prediksi OPT bukan diagnosis. Verifikasi dengan gejala, populasi, luas serangan, dan musuh alami.
