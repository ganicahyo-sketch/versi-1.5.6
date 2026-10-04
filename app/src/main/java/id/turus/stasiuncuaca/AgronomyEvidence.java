package id.turus.stasiuncuaca;

/** Ringkasan bukti yang ditanamkan ke aplikasi agar AI memakai rujukan yang dapat dilacak. */
public final class AgronomyEvidence {
    private AgronomyEvidence() {}

    public static String brief() {
        return "" +
            "1) FAO-56 (Allen et al., 1998): ET0 dihitung dengan Penman-Monteith dan ETc memakai koefisien tanaman/fase.\n" +
            "2) FAO salinity guidance: salinitas dinilai paling tepat dengan ECe root-zone; ambang berbeda menurut komoditas. Contoh ECe threshold: padi ~3.0 dS/m, jagung ~1.7 dS/m, kedelai ~5.0 dS/m, tomat ~2.5 dS/m, pepper ~1.5-1.7 dS/m. EC sensor lapang tidak boleh disetarakan langsung dengan ECe tanpa verifikasi metode.\n" +
            "3) Pertanian Indonesia 2024: dosis NPK tanaman pangan ditentukan menurut kebutuhan tanaman + status hara tanah; N berlebih dapat menurunkan efisiensi, P dapat terakumulasi, K dapat mengalami luxury consumption.\n" +
            "4) STCR: kebutuhan pupuk dihitung dari kebutuhan hara target, kontribusi tanah, kontribusi pupuk/bahan organik, dan efisiensi; koefisien harus spesifik komoditas/tanah/lokasi bila tersedia.\n" +
            "5) Meta-analisis Journal of Environmental Management 2023: amelioran dapat menaikkan pH dan hasil, tetapi respons bergantung tanah. Untuk tanah sangat masam, kapur merupakan intervensi penting; pada pH lebih tinggi, bahan organik/manure/straw lebih tepat sebagai perbaikan pendukung. Dosis kapur tidak ditentukan dari pH saja.\n" +
            "6) VPD: review fisiologi tanaman melaporkan VPD sekitar 0.5-1.5 kPa umumnya sesuai untuk banyak tanaman; sekitar 1.7-2.0 kPa dapat meningkatkan stress terutama saat air terbatas.\n" +
            "7) Forecast penyakit foliar: suhu + kelembapan + hujan + leaf wetness adalah variabel penting; model generik memakai temperatur dan durasi basah.\n" +
            "8) Padi blast: model forecasting memakai suhu, RH, hujan, angin, dan durasi RH tinggi; kondisi suhu harian rata-rata sekitar 22-26 C, kelembapan tinggi dan hujan dapat meningkatkan risiko.\n" +
            "9) Fall armyworm: model perkembangan menunjukkan optimum sekitar 28-30 C dan pendekatan degree-day dapat memperkirakan perkembangan stadium.\n" +
            "10) Semua prediksi OPT di aplikasi adalah peringatan dini. Diagnosis akhir harus diverifikasi dengan gejala, populasi, luas serangan, umur tanaman, dan observasi lapang.";
    }

    public static String citations() {
        return "FAO Irrigation and Drainage Paper 56 (Allen et al., 1998); FAO Water Quality/Crop Salt Tolerance; " +
               "Widowati et al. (2024) Recomm. NPK Tanaman Pangan Edisi 2, Pertanian Press; " +
               "Journal of Environmental Management 345 (2023) 118531, DOI 10.1016/j.jenvman.2023.118531; " +
               "Phytopathology (2023) rice panicle blast forecasting; APS Plant Disease Lessons late blight/forecasting; " +
               "Insects (2022) temperature-dependent development of Spodoptera frugiperda.";
    }
}
