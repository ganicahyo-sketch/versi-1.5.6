package id.turus.stasiuncuaca;

import java.util.Locale;

/**
 * Mesin analisis agronomi berbasis aturan ilmiah + model neraca hara.
 *
 * Catatan penting:
 * - N/P/K CWT-7in1 diperlakukan sebagai nilai input yang benar sesuai proyek,
 *   tetapi klasifikasi N/P/K diberi label SCREENING karena ambang resmi sangat
 *   bergantung metode ekstraksi, jenis tanah, dan komoditas.
 * - EC sensor tidak disamakan mentah-mentah dengan ECe laboratorium. Nilai
 *   dS/m hanya screening untuk risiko salinitas dan harus dikonfirmasi bila
 *   mendekati ambang kritis.
 * - Model dosis menggunakan baseline kebutuhan musim + kontribusi soil-test +
 *   efisiensi pemupukan + kredit riwayat. Ini STCR-style, bukan persamaan STCR
 *   spesifik satu komoditas. Persamaan spesifik harus menggantikan koefisien
 *   jika tersedia dari penelitian/lokasi.
 */
public final class AgronomyEngine {
    private AgronomyEngine() {}

    public static final class CropProfile {
        public final String name;
        public final int cycleDays;
        public final double phMin, phMax;
        public final double tempMin, tempMax;
        public final double ecThresholdDsM;
        public final double nMin, nMax;
        public final double pMin, pMax;
        public final double kMin, kMax;
        public final double seasonalN, seasonalP, seasonalK;
        public final double tBase;
        public final String source;

        CropProfile(String name, int cycleDays, double phMin, double phMax,
                    double tempMin, double tempMax, double ecThresholdDsM,
                    double nMin, double nMax, double pMin, double pMax,
                    double kMin, double kMax, double seasonalN, double seasonalP,
                    double seasonalK, double tBase, String source) {
            this.name = name;
            this.cycleDays = cycleDays;
            this.phMin = phMin;
            this.phMax = phMax;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
            this.ecThresholdDsM = ecThresholdDsM;
            this.nMin = nMin;
            this.nMax = nMax;
            this.pMin = pMin;
            this.pMax = pMax;
            this.kMin = kMin;
            this.kMax = kMax;
            this.seasonalN = seasonalN;
            this.seasonalP = seasonalP;
            this.seasonalK = seasonalK;
            this.tBase = tBase;
            this.source = source;
        }
    }

    public static CropProfile profile(String crop) {
        String c = norm(crop);
        String src = "FAO/Indonesia fertilizer baseline + crop ecology; verify local SOP.";
        if (c.contains("padi") || c.contains("rice"))
            return new CropProfile("Padi",115,5.0,7.0,20,32,3.0,20,40,8,15,80,120,80,45,20,10,src);
        if (c.contains("jagung") || c.contains("maize") || c.contains("corn"))
            return new CropProfile("Jagung",110,5.5,7.5,18,33,1.7,20,40,8,15,80,120,80,40,20,10,src);
        if (c.contains("kedelai") || c.contains("soy"))
            return new CropProfile("Kedelai",95,5.5,7.0,20,30,5.0,20,40,8,15,80,140,25,40,15,10,src);
        if (c.contains("cabai") || c.contains("chili") || c.contains("pepper"))
            return new CropProfile("Cabai",130,5.5,7.0,20,32,1.7,20,40,10,20,100,180,90,55,35,10,src);
        if (c.contains("tomat") || c.contains("tomato"))
            return new CropProfile("Tomat",110,5.5,7.0,18,30,2.5,20,40,10,20,100,180,90,60,40,10,src);
        if (c.contains("singkong") || c.contains("cassava"))
            return new CropProfile("Singkong",300,4.5,7.0,20,32,2.5,20,40,8,15,80,160,70,35,35,12,src);
        if (c.contains("ubi jalar") || c.contains("sweet potato"))
            return new CropProfile("Ubi jalar",120,5.5,6.8,20,30,1.5,20,40,8,15,80,140,60,30,30,12,src);
        if (c.contains("kentang") || c.contains("potato"))
            return new CropProfile("Kentang",100,5.0,6.5,15,25,1.7,20,40,10,20,100,180,105,70,35,7,src);
        if (c.contains("bawang merah") || c.contains("shallot") || c.contains("onion"))
            return new CropProfile("Bawang",90,5.5,6.8,15,30,1.2,20,40,10,20,80,160,100,55,35,8,src);
        if (c.contains("kelapa sawit") || c.contains("sawit") || c.contains("oil palm"))
            return new CropProfile("Kelapa sawit",3650,4.0,7.0,25,32,2.5,20,40,8,15,100,180,120,60,180,15,src);
        return new CropProfile("Komoditas umum",120,5.5,7.0,18,32,2.0,20,40,8,15,80,140,80,40,30,10,src);
    }

    public static String normalizeCrop(String crop) {
        String c = norm(crop);
        if (c.contains("padi") || c.contains("rice")) return "Padi";
        if (c.contains("jagung") || c.contains("maize") || c.contains("corn")) return "Jagung";
        if (c.contains("kedelai") || c.contains("soy")) return "Kedelai";
        if (c.contains("cabai") || c.contains("chili") || c.contains("pepper")) return "Cabai";
        if (c.contains("tomat") || c.contains("tomato")) return "Tomat";
        if (c.contains("singkong") || c.contains("cassava")) return "Singkong";
        if (c.contains("ubi jalar") || c.contains("sweet potato")) return "Ubi jalar";
        if (c.contains("kentang") || c.contains("potato")) return "Kentang";
        if (c.contains("bawang merah") || c.contains("shallot") || c.contains("onion")) return "Bawang";
        if (c.contains("kelapa sawit") || c.contains("sawit") || c.contains("oil palm")) return "Kelapa sawit";
        return crop == null || crop.trim().isEmpty() ? "Komoditas umum" : crop.trim();
    }

    public static String phase(int hst, String crop) {
        CropProfile p = profile(crop);
        if (hst < 0) return "Belum tanam / HST belum valid";
        if (p.name.equals("Kelapa sawit")) {
            if (hst < 365) return "TBM awal / pembentukan vegetatif";
            if (hst < 1095) return "TBM lanjut / vegetatif";
            return "TM / produksi";
        }
        if (hst <= Math.max(20, (int)(p.cycleDays * 0.18))) return "Perkecambahan / establishment";
        if (hst <= Math.max(35, (int)(p.cycleDays * 0.45))) return "Vegetatif";
        if (hst <= Math.max(60, (int)(p.cycleDays * 0.68))) return "Pembungaan / reproduktif";
        if (hst <= p.cycleDays) return "Pengisian hasil / pemasakan";
        return "Lewat umur normal panen — verifikasi varietas/rotasi";
    }

    public static double stageFraction(int hst, String crop) {
        CropProfile p = profile(crop);
        String ph = phase(hst, crop).toLowerCase(Locale.US);
        if (ph.contains("establishment")) return 0.15;
        if (ph.contains("vegetatif")) return 0.30;
        if (ph.contains("reproduktif") || ph.contains("pembungaan")) return 0.35;
        if (ph.contains("pengisian") || ph.contains("pemasakan")) return 0.20;
        return 0.25;
    }

    public static String classifyP(double value) { return classifyP(value, ""); }

    public static String classifyP(double value, String method) {
        if (Double.isNaN(value)) return "Tidak ada data";
        String m=norm(method);
        if (m.contains("mehlich-3 icp")) {
            if (value < 25) return "Rendah";
            if (value <= 45) return "Sedang";
            return "Tinggi";
        }
        if (m.contains("mehlich-3")) {
            if (value < 25) return "Rendah";
            if (value <= 45) return "Sedang";
            return "Tinggi";
        }
        if (m.contains("olsen")) {
            if (value < 11) return "Rendah";
            if (value <= 20) return "Sedang";
            return "Tinggi";
        }
        return "Metode belum dipilih — tidak diberi kelas universal";
    }
    public static String classifyK(double value) { return classifyK(value, ""); }

    public static String classifyK(double value, String method) {
        if (Double.isNaN(value)) return "Tidak ada data";
        String m=norm(method);
        if (m.contains("mehlich-3")) {
            if (value < 35) return "Rendah";
            if (value <= 60) return "Sedang";
            return "Tinggi";
        }
        if (m.contains("nh4oac") || m.contains("nh4 oac") || m.contains("ammonium acetate")) {
            if (value < 70) return "Rendah";
            if (value <= 150) return "Sedang";
            return "Tinggi";
        }
        return "Metode belum dipilih — tidak diberi kelas universal";
    }

    public static String classifyN(double value) {
        if (Double.isNaN(value)) return "Tidak ada data";
        return "Metode/ambang N belum diketahui";
    }

    public static String classifyN(double value, double low, double high) {
        if (Double.isNaN(value)) return "Tidak ada data";
        if (!Double.isFinite(low) || !Double.isFinite(high) || low < 0 || high <= low) return classifyN(value);
        if (value < low) return "Rendah";
        if (value <= high) return "Sedang";
        return "Tinggi";
    }

    public static String classifyPH(double ph, CropProfile p) {
        if (Double.isNaN(ph)) return "Tidak ada data";
        if (ph < p.phMin - 0.5) return "Sangat masam untuk komoditas ini";
        if (ph < p.phMin) return "Agak masam / di bawah kisaran target";
        if (ph <= p.phMax) return "Aman / sesuai kisaran";
        if (ph <= p.phMax + 0.5) return "Agak alkalis / di atas kisaran target";
        return "Terlalu alkalis untuk komoditas ini";
    }

    public static String classifyEC(double usCm, CropProfile p) {
        if (Double.isNaN(usCm)) return "Tidak ada data";
        double ds = usCm / 1000.0;
        if (ds < p.ecThresholdDsM * 0.5) return "Rendah";
        if (ds <= p.ecThresholdDsM) return "Cukup / masih dalam batas screening";
        if (ds <= p.ecThresholdDsM * 1.5) return "Tinggi / perlu waspada";
        return "Sangat tinggi / risiko salinitas";
    }

    public static String classifyMoisture(double pct, String crop) {
        if (Double.isNaN(pct)) return "Tidak ada data";
        if (pct < 20) return "Kering";
        if (pct < 35) return "Cenderung kering";
        if (pct <= 70) return "Aman / cukup";
        if (pct <= 85) return "Lembap tinggi";
        return "Terlalu lembap / cek drainase";
    }

    public static String classifyMoisture(double pct) {
        return classifyMoisture(pct, "");
    }

    public static String classifyECe(double eceDsM) {
        if (Double.isNaN(eceDsM)) return "Tidak ada data";
        if (eceDsM < 2) return "Non-saline";
        if (eceDsM < 4) return "Sedikit salin";
        if (eceDsM < 8) return "Moderat salin";
        if (eceDsM < 16) return "Sangat salin";
        return "Ekstrem salin";
    }

    public static double soilStockKgHa(double concentrationMgKg, double bulkDensityGcm3, double depthCm) {
        if (!Double.isFinite(concentrationMgKg) || !Double.isFinite(bulkDensityGcm3) || !Double.isFinite(depthCm)) return Double.NaN;
        if (concentrationMgKg < 0 || bulkDensityGcm3 <= 0 || depthCm <= 0) return Double.NaN;
        // mg/kg × g/cm³ × cm × 0.10 = kg/ha.
        return concentrationMgKg * bulkDensityGcm3 * depthCm * 0.10;
    }

    public static String soilWaterAssessment(double moisturePct, double fcPct, double pwpPct, double depthCm, double et0Mm, String crop) {
        if (!Double.isFinite(moisturePct)) return "DATA AIR TANAH TIDAK ADA";
        if (Double.isFinite(fcPct) && Double.isFinite(pwpPct) && fcPct > pwpPct) {
            double ftsw=(moisturePct-pwpPct)/(fcPct-pwpPct);
            ftsw=Math.max(0,Math.min(1,ftsw));
            double rawFrac=0.5;
            if (Double.isFinite(et0Mm)) rawFrac = et0Mm >= 5 ? 0.3 : (et0Mm <= 2 ? 0.7 : 0.5);
            String state=ftsw<rawFrac?"KURANG / mendekati di bawah RAW":(ftsw>0.9?"TINGGI / dekat FC":"AMAN / dalam zona air tersedia");
            return String.format(Locale.US,"%s; FTSW %.2f; TAW lapisan %.1f mm; RAW perkiraan %.1f mm",state,ftsw,Math.max(0,(fcPct-pwpPct)*depthCm*0.1),Math.max(0,(fcPct-pwpPct)*depthCm*0.1*rawFrac));
        }
        return classifyMoisture(moisturePct,crop)+"; FC/PWP belum tersedia, jadi persentase sensor hanya screening.";
    }

    public static String vpdCombinedStatus(double vpd, double moisturePct, double fcPct, double pwpPct, double depthCm, double et0Mm, String crop) {
        if (!Double.isFinite(vpd)) return "VPD belum tersedia.";
        String water=soilWaterAssessment(moisturePct,fcPct,pwpPct,depthCm,et0Mm,crop);
        boolean high=vpd>2.0; boolean dry=water.startsWith("KURANG") || (!Double.isFinite(fcPct) && moisturePct<30);
        if (high && dry) return "PERHATIAN TINGGI: VPD tinggi + air tanah rendah dapat meningkatkan stres air tanaman.";
        if (high) return "WASPADA: VPD tinggi; pantau kehilangan air, layu, bunga/daun dan ketersediaan air.";
        return "Kombinasi VPD dan air tanah belum menunjukkan tekanan tinggi; tetap pantau tren.";
    }

    public static String temperatureStatus(double tempC, String crop) {
        if (!Double.isFinite(tempC)) return "Tidak ada data";
        CropProfile p=profile(crop);
        if (tempC<p.tempMin) return "Di bawah kisaran ekologis profil";
        if (tempC>p.tempMax) return "Di atas kisaran ekologis profil";
        return "Dalam kisaran ekologis profil";
    }

    public static String nutrientSufficiency(double soilValue, String nutrient, String crop, int hst) {
        if (Double.isNaN(soilValue)) return "Tidak dapat dinilai — data belum ada";
        String cls;
        if (nutrient.equals("N")) cls = classifyN(soilValue);
        else if (nutrient.equals("P")) cls = classifyP(soilValue);
        else cls = classifyK(soilValue);
        if ("Rendah".equals(cls)) return "BELUM CUKUP / berpotensi membatasi fase ini";
        if ("Sedang".equals(cls)) return "CUKUP SEMENTARA, tetapi sesuaikan dengan target hasil dan fase";
        return "CUKUP–TINGGI; jangan menambah rutin tanpa dasar kebutuhan";
    }

    public static String classifyVpd(double vpd) {
        if (Double.isNaN(vpd)) return "Tidak ada data";
        if (vpd < 0.4) return "Sangat rendah";
        if (vpd <= 1.5) return "Umumnya aman";
        if (vpd <= 2.0) return "Waspada";
        return "Tinggi / berpotensi stress air";
    }

    public static double vpd(double t, double rh) {
        if (Double.isNaN(t) || Double.isNaN(rh)) return Double.NaN;
        double es = 0.6108 * Math.exp((17.27 * t) / (t + 237.3));
        return Math.max(0, es * (1.0 - rh / 100.0));
    }

    public static double gdd(double tAvg, double tBase) {
        if (Double.isNaN(tAvg) || Double.isNaN(tBase)) return Double.NaN;
        return Math.max(0, tAvg - tBase);
    }

    public static String weatherStatus(double t, double rh, double rain, double et0, double vpd, String crop) {
        CropProfile p = profile(crop);
        StringBuilder s = new StringBuilder();
        boolean bad = false;
        if (!Double.isNaN(t)) {
            if (t < p.tempMin) { s.append("suhu di bawah kisaran "); bad = true; }
            else if (t > p.tempMax) { s.append("suhu di atas kisaran "); bad = true; }
        }
        if (!Double.isNaN(vpd) && vpd > 2.0) { if (s.length() > 0) s.append("; "); s.append("VPD tinggi"); bad = true; }
        if (!Double.isNaN(rain) && rain > 80) { if (s.length() > 0) s.append("; "); s.append("hujan sangat tinggi"); bad = true; }
        if (!Double.isNaN(rh) && rh > 90) { if (s.length() > 0) s.append("; "); s.append("RH sangat tinggi"); bad = true; }
        if (!Double.isNaN(et0) && !Double.isNaN(rain) && rain < et0) { if (s.length() > 0) s.append("; "); s.append("hujan < ET0"); bad = true; }
        if (!bad) return "CUACA CENDERUNG SESUAI untuk " + p.name;
        return "CUACA PERLU DIWASPADAI: " + s;
    }

    public static double nutrientNeed(double soilValue, String nutrient, String crop, int hst, double recentCreditKgHa, double yieldTargetTPerHa) {
        return nutrientNeed(soilValue,nutrient,crop,hst,recentCreditKgHa,yieldTargetTPerHa,"",20,1.30);
    }

    public static double nutrientNeed(double soilValue, String nutrient, String crop, int hst, double recentCreditKgHa, double yieldTargetTPerHa, String method, double depthCm, double bulkDensityGcm3) {
        CropProfile p=profile(crop);
        double seasonal=nutrient.equals("N")?p.seasonalN:(nutrient.equals("P")?p.seasonalP:p.seasonalK);
        double yieldScale=1.0;
        if (Double.isFinite(yieldTargetTPerHa) && yieldTargetTPerHa>0) yieldScale=Math.max(0.5,Math.min(1.8,yieldTargetTPerHa/Math.max(0.1,defaultYield(p))));
        double stageNeed=seasonal*stageFraction(hst,crop)*yieldScale;
        double support=0; String cls;
        if (nutrient.equals("N")) cls=classifyN(soilValue); else if (nutrient.equals("P")) cls=classifyP(soilValue,method); else cls=classifyK(soilValue,method);
        if ("Rendah".equals(cls)) support=0.20;
        else if ("Sedang".equals(cls)) support=0.50;
        else if ("Tinggi".equals(cls)) support=0.80;
        double gross=stageNeed*(1.0-support);
        double recovery=nutrient.equals("N")?0.50:(nutrient.equals("P")?0.30:0.60);
        double need=Math.max(0,gross/Math.max(0.1,recovery)-Math.max(0,recentCreditKgHa));
        // The stock is reported separately; it is not treated as fully plant-available uptake.
        return need;
    }

    public static String nutrientFormulaText() {
        return "Kebutuhan fase ≈ kebutuhan musim × fraksi fase × skala target hasil × (1−dukungan soil-test) ÷ efisiensi pemulihan − kredit pupuk. Stok tanah = konsentrasi × BD × kedalaman × 0,10 kg/ha; stok bukan serapan tanaman. Model ini screening/STCR-style, bukan QUEFTS atau STCR terkalibrasi.";
    }

    private static double defaultYield(CropProfile p) {
        if (p.name.equals("Padi")) return 6;
        if (p.name.equals("Jagung")) return 7;
        if (p.name.equals("Kedelai")) return 2.5;
        if (p.name.equals("Tomat")) return 40;
        if (p.name.equals("Cabai")) return 8;
        return 5;
    }

    public static String limeAdvice(double ph, CropProfile p, String cultivation, double phBuffer, double alDd, double hDd, double cec, double omPct, double labLimeKgHa) {
        if (!Double.isFinite(ph)) return "pH belum diisi.";
        boolean organic=cultivation!=null&&norm(cultivation).contains("organik");
        String tail=organic?" Mode organik: utamakan bahan organik matang dan input yang diizinkan; jangan menganggap bahan organik otomatis menggantikan kebutuhan kapur.":"";
        if (ph<p.phMin) {
            if (Double.isFinite(labLimeKgHa) && labLimeKgHa>0) return "pH rendah. Ikuti kebutuhan kapur/dolomit hasil laboratorium: "+String.format(Locale.US,"%.1f",labLimeKgHa)+" kg/ha, dengan pilihan bahan disesuaikan Ca/Mg dan SOP."+tail;
            if (Double.isFinite(phBuffer)||Double.isFinite(alDd)||Double.isFinite(hDd)||Double.isFinite(cec)) return "pH rendah. Parameter buffer/kemasaman/CEC sudah tersedia; gunakan hasil uji untuk menghitung kebutuhan kapur, jangan dari pH tunggal."+tail;
            return "pH rendah. Belum aman menghitung dolomit dari pH saja. Tambahkan pH-buffer, Al-dd/H-dd, CEC, atau kebutuhan kapur hasil laboratorium."+tail;
        }
        if (ph>p.phMax) return "pH tinggi. Hentikan koreksi dengan kapur/dolomit. Periksa air irigasi/alkalinitas dan lakukan pengasaman hanya bila kebutuhan telah dihitung."+tail;
        return "pH berada dalam kisaran target komoditas. Pertahankan bahan organik dan pantau tren pH."+tail;
    }

    public static String amendmentAdvice(double ph, CropProfile p, String cultivation) {
        return limeAdvice(ph,p,cultivation,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN);
    }

    public static String optRisk(String crop, double t, double rh, double rain, double wind, String historyText, int hst) {
        String base = optRisk(crop,t,rh,rain,wind,historyText);
        String ph = phase(hst,crop);
        if (hst >= 0 && ph != null && !ph.contains("Belum")) return base + "\nFase tanaman: " + ph + ". Gunakan fase ini untuk menentukan bagian tanaman yang harus dipantau.";
        return base;
    }

    public static String optRisk(String crop, double t, double rh, double rain, double wind, String historyText) {
        String c = norm(crop);
        StringBuilder s = new StringBuilder();
        double score;
        if (c.contains("padi") || c.contains("rice")) {
            score = 0;
            if (!Double.isNaN(t) && t >= 20 && t <= 28) score += 30;
            if (!Double.isNaN(rh) && rh >= 80) score += 30;
            if (!Double.isNaN(rain) && rain >= 5) score += 20;
            if (!Double.isNaN(wind) && wind < 3.5) score += 10;
            s.append("Padi — Pyricularia oryzae (blas): ").append(level(score)).append(". ");
            if (score >= 50) s.append("Cuaca mendukung kelembapan infeksi; periksa bercak daun/malai dan tingkatkan scouting.");
            else s.append("Pantau bercak daun terutama setelah periode lembap/hujan.");
            s.append("\nPadi — Nilaparvata lugens (wereng batang cokelat): risiko awal ")
                    .append(level((!Double.isNaN(t) && t >= 25 && t <= 32 ? 45 : 15) + (!Double.isNaN(rh) && rh >= 70 ? 30 : 0)))
                    .append("; validasi dengan jumlah wereng per rumpun dan keberadaan musuh alami.");
            s.append("\nPadi — penggerek batang: pantau sundep/beluk dan telur/larva; keputusan pengendalian tetap berdasarkan pengamatan populasi.");
        } else if (c.contains("jagung") || c.contains("maize") || c.contains("corn")) {
            double faw = (!Double.isNaN(t) && t >= 26 && t <= 31 ? 50 : 20) + (!Double.isNaN(rain) && rain >= 5 ? 10 : 0);
            s.append("Jagung — Spodoptera frugiperda (fall armyworm): ").append(level(faw)).append(". ");
            if (faw >= 50) s.append("Suhu mendukung perkembangan; periksa pucuk/whorl dan frass setiap scouting.");
            else s.append("Tetap lakukan scouting pucuk, terutama tanaman muda.");
            double dm = (!Double.isNaN(rh) && rh >= 80 ? 35 : 0) + (!Double.isNaN(rain) && rain >= 10 ? 35 : 0) + (!Double.isNaN(t) && t >= 18 && t <= 28 ? 20 : 0);
            s.append("\nJagung — Peronosclerospora spp. (bulai): ").append(level(dm)).append("; verifikasi gejala belang/klorosis.");
        } else if (c.contains("cabai") || c.contains("chili") || c.contains("pepper")) {
            double anth = (!Double.isNaN(rh) && rh >= 80 ? 30 : 0) + (!Double.isNaN(rain) && rain >= 5 ? 35 : 0) + (!Double.isNaN(t) && t >= 24 && t <= 30 ? 25 : 0);
            s.append("Cabai — Colletotrichum spp. (antraknosa): ").append(level(anth)).append(". Periksa buah bercak cekung dan busuk, terutama setelah hujan/kelembapan tinggi.");
            double thrips = (!Double.isNaN(t) && t >= 25 && t <= 33 ? 35 : 15) + (!Double.isNaN(rh) && rh < 75 ? 20 : 5);
            s.append("\nCabai — Thrips spp.: ").append(level(thrips)).append("; periksa pucuk dan daun muda.");
        } else if (c.contains("tomat") || c.contains("tomato")) {
            double lb = (!Double.isNaN(t) && t >= 15 && t <= 26 ? 30 : 0) + (!Double.isNaN(rh) && rh >= 90 ? 35 : 0) + (!Double.isNaN(rain) && rain >= 5 ? 30 : 0);
            s.append("Tomat — Phytophthora infestans (hawar daun/later blight): ").append(level(lb)).append(". Waspadai cuaca sejuk-basah dengan kelembapan tinggi dan periode daun basah.");
            double ab = (!Double.isNaN(rh) && rh >= 90 ? 25 : 0) + (!Double.isNaN(rain) && rain >= 5 ? 25 : 0) + (!Double.isNaN(t) && t >= 20 && t <= 30 ? 25 : 0);
            s.append("\nTomat — Alternaria solani (early blight): ").append(level(ab)).append("; pantau bercak konsentris pada daun tua.");
        } else if (c.contains("kedelai") || c.contains("soy")) {
            double rust = (!Double.isNaN(rh) && rh >= 80 ? 35 : 0) + (!Double.isNaN(rain) && rain >= 5 ? 30 : 0) + (!Double.isNaN(t) && t >= 20 && t <= 28 ? 20 : 0);
            s.append("Kedelai — Phakopsora pachyrhizi (karat kedelai): ").append(level(rust)).append("; periksa pustula di bawah daun setelah periode lembap.");
            s.append("\nKedelai — hama pengisap/pemakan polong: risiko tidak dapat dipastikan hanya dari cuaca; masukkan populasi dan stadia ke catatan OPT.");
        } else {
            s.append("Komoditas belum memiliki model OPT lokal di mesin. Jangan menebak nama OPT hanya dari cuaca. Masukkan nama OPT + gejala + populasi/luas serangan pada Catatan Lapangan agar AI dapat mencocokkan pustaka.");
        }
        if (historyText != null && !historyText.trim().isEmpty()) {
            s.append("\nRiwayat lapang: ").append(historyText.trim());
        }
        return s.toString();
    }

    private static String level(double score) {
        if (score >= 75) return "TINGGI";
        if (score >= 50) return "SEDANG-TINGGI";
        if (score >= 30) return "SEDANG";
        return "RENDAH";
    }

    public static String evidenceBrief() { return AgronomyEvidence.brief(); }
    public static String evidenceCitations() { return AgronomyEvidence.citations(); }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.US).trim();
    }
}
