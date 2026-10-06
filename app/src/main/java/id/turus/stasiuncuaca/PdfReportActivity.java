package id.turus.stasiuncuaca;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Laporan PDF mandiri: cuaca, tanah, histori, analisis, rekomendasi, dan AI.
 * Menggunakan PdfDocument bawaan Android - tanpa library tambahan.
 */
public class PdfReportActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter PRINT_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));

    private Spinner reportMode;
    private TextView reportInfo;
    private File lastPdf;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_pdf_report);
        reportMode = findViewById(R.id.pdfReportMode);
        reportInfo = findViewById(R.id.pdfReportInfo);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Laporan lengkap", "Laporan ringkas"});
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportMode.setAdapter(a);
        findViewById(R.id.pdfBack).setOnClickListener(v -> finish());
        findViewById(R.id.pdfSave).setOnClickListener(v -> createAndSave());
        findViewById(R.id.pdfPrint).setOnClickListener(v -> createAndPrint());
        reportInfo.setText("Laporan mencakup data cuaca, tanah CWT-7in1, histori pupuk/OPT/catatan lapangan, analisis agronomi, dan rekomendasi.");
    }

    private void createAndSave() {
        try {
            File f = buildPdf(reportMode.getSelectedItemPosition() == 0);
            lastPdf = f;
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            i.setType("application/pdf");
            i.putExtra(android.content.Intent.EXTRA_TITLE, fileName());
            startActivityForResult(i, 9001);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuat PDF: " + safe(e), Toast.LENGTH_LONG).show();
        }
    }

    private void createAndPrint() {
        try {
            lastPdf = buildPdf(reportMode.getSelectedItemPosition() == 0);
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (pm == null) throw new Exception("Layanan cetak Android tidak tersedia");
            pm.print("STASIUN CUACA DESA TURUS", new PdfPrintAdapter(lastPdf), printAttributes());
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka cetak: " + safe(e), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 9001 || resultCode != RESULT_OK || data == null || data.getData() == null || lastPdf == null) return;
        try (InputStream in = new BufferedInputStream(new FileInputStream(lastPdf));
             OutputStream out = new BufferedOutputStream(getContentResolver().openOutputStream(data.getData()))) {
            if (out == null) throw new Exception("Lokasi penyimpanan tidak dapat dibuka");
            byte[] buf = new byte[32768]; int n;
            while ((n = in.read(buf)) >= 0) { if (n == 0) continue; out.write(buf, 0, n); }
            out.flush();
            Toast.makeText(this, "PDF tersimpan.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan PDF: " + safe(e), Toast.LENGTH_LONG).show();
        }
    }

    private PrintAttributes printAttributes() {
        return new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("turus", "STASIUN CUACA", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
    }

    private String fileName() {
        String crop = pref("crop", "Tanaman").trim().replaceAll("[^A-Za-z0-9_-]+", "_");
        String date = LocalDate.now(WIB).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "STASIUN-CUACA-DESA-TURUS_LAPORAN_" + crop + "_" + date + ".pdf";
    }

    private File buildPdf(boolean complete) throws Exception {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        File dir = new File(getCacheDir(), "reports");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Folder laporan gagal dibuat");
        File out = File.createTempFile("turus_report_", ".pdf", dir);

        PdfDocument doc = new PdfDocument();
        ReportCanvas rc = new ReportCanvas(doc);
        rc.heading("STASIUN CUACA DESA TURUS");
        rc.subheading("Laporan " + (complete ? "Lengkap" : "Ringkas") + " - Open-Meteo + Agronomi");
        rc.meta("Dicetak: " + java.time.ZonedDateTime.now(WIB).format(PRINT_DATE) + " WIB");
        rc.meta("Komoditas: " + pref("crop", "Tanaman pertanian") + " | Budidaya: " + pref("farm_cultivation_mode", "Konvensional / PHT"));
        rc.meta("Luas: " + pref("farm_area_ha", "--") + " ha | HST: " + pref("farm_hst", "--") + " | Fase: " + phaseText());
        rc.line();

        addCurrentWeather(rc, p);
        addSoil(rc, p);
        addAgronomyAnalysis(rc, p);
        if (complete) {
            addOptRisk(rc, p);
            addHistory(rc, p);
            addRecommendations(rc, p);
            addAi(rc, p);
            addSources(rc);
        } else {
            addRecommendations(rc, p);
        }

        rc.finish(out);
        return out;
    }

    private void addCurrentWeather(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("1. DATA CUACA TERKINI + FORECAST");
        rc.kv("Suhu udara", pref("om_temp","--")+" °C");
        rc.kv("Suhu terasa", pref("om_apparent_temp","--")+" °C");\n        rc.kv("Radiasi Matahari", pref("om_radiation","--")+" W/m²");\n        rc.kv("PAR estimasi", pref("om_par","--")+" W/m²");\n        rc.kv("PPFD estimasi", pref("om_ppfd","--")+" µmol/m²/s");
        rc.kv("Titik embun", pref("om_dewpoint","--")+" °C");
        rc.kv("Kelembapan", pref("om_rh","--")+" %");
        rc.kv("Tekanan", pref("om_pressure","--")+" hPa");
        rc.kv("Curah hujan harian", pref("om_rain","--")+" mm/hari");
        rc.kv("ET0 FAO-56", pref("om_et0","--")+" mm/hari");
        double vpd=num(pref("om_vpd","")); rc.kv("VPD",pref("om_vpd","--")+" kPa -> "+AgronomyEngine.classifyVpd(vpd));
        rc.kv("Arah angin",pref("om_wind_direction","--")+" | "+pref("om_wind_speed","--")+" m/s | gust "+pref("om_wind_gust","--")+" m/s");
        rc.kv("Awan",pref("om_cloud_cover","--")+" % | UV "+pref("om_uv","--"));
        rc.kv("Visibilitas",pref("om_visibility","--")+" m");
        rc.kv("Lama penyinaran",pref("om_sun_hours","--")+" jam/hari");
        rc.kv("Radiasi",pref("om_radiation","--")+" W/m²");
        rc.kv("Kondisi",pref("om_weather","--"));
        rc.kv("Forecast 7 hari",pref("om_forecast_7d","--"));
        rc.line();
    }


    private void addSoil(ReportCanvas rc, android.content.SharedPreferences p) {
        String crop=pref("crop","Tanaman pertanian"); AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop);
        double ph=num(pref("soil_ph","")),n=num(pref("soil_n","")),pp=num(pref("soil_p","")),k=num(pref("soil_k","")),nLow=num(pref("soil_n_low","")),nHigh=num(pref("soil_n_high",""));
        double ec=num(pref("soil_ec_us_cm","")),ece=num(pref("soil_ece_ds_m","")),moist=num(pref("soil_moisture_pct",""));
        double depth=num(pref("soil_depth_cm","20")),bd=num(pref("soil_bulk_density_g_cm3","1.30")),fc=num(pref("soil_fc_pct","")),pwp=num(pref("soil_pwp_pct",""));
        double phBuf=num(pref("soil_ph_buffer","")),al=num(pref("soil_al_dd","")),hd=num(pref("soil_h_dd","")),cec=num(pref("soil_cec","")),om=num(pref("soil_om_pct","")),lime=num(pref("soil_lime_requirement_kg_ha",""));
        String method=pref("soil_test_method","Metode tidak diketahui");
        rc.section("2. TANAH CWT-7IN1 + LABORATORIUM");
        rc.kv("Sumber sensor",pref("soil_source","CWT-7IN1 RS485 / USB"));
        rc.kv("Metode N/P/K",method);
        rc.kv("pH",show(ph)+" -> "+AgronomyEngine.classifyPH(ph,cp));
        rc.kv("Tindakan pH",AgronomyEngine.limeAdvice(ph,cp,pref("farm_cultivation_mode","Konvensional / PHT"),phBuf,al,hd,cec,om,lime));
        rc.kv("pH-buffer / Al-dd / H-dd / CEC",show(phBuf)+" / "+show(al)+" / "+show(hd)+" / "+show(cec));
        rc.kv("Bahan organik",show(om)+" %");
        rc.kv("N tersedia",show(n)+" mg/kg -> "+AgronomyEngine.classifyN(n,nLow,nHigh));
        rc.kv("P tersedia",show(pp)+" mg/kg -> "+AgronomyEngine.classifyP(pp,method));
        rc.kv("K tersedia",show(k)+" mg/kg -> "+AgronomyEngine.classifyK(k,method));
        rc.kv("EC sensor",show(ec)+" µS/cm -> "+AgronomyEngine.classifyEC(ec,cp)+" (screening)");
        rc.kv("ECe laboratorium",Double.isFinite(ece)?show(ece)+" dS/m -> "+AgronomyEngine.classifyECe(ece):"belum ada");
        rc.kv("Kelembapan",show(moist)+" %");
        rc.kv("FC / PWP",show(fc)+" / "+show(pwp)+" % volume");
        rc.kv("Status air tanah",AgronomyEngine.soilWaterAssessment(moist,fc,pwp,depth,num(pref("om_et0","")),crop));
        rc.kv("Lapisan / bulk density",show(depth)+" cm / "+show(bd)+" g/cm³");
        rc.kv("Stok N / P / K",show(AgronomyEngine.soilStockKgHa(n,bd,depth))+" / "+show(AgronomyEngine.soilStockKgHa(pp,bd,depth))+" / "+show(AgronomyEngine.soilStockKgHa(k,bd,depth))+" kg/ha");
        rc.text("Stok tanah adalah massa unsur pada lapisan yang dihitung; stok bukan sama dengan serapan/tersedia langsung tanaman. EC sensor lapang tidak dikonversi otomatis menjadi ECe.");
        rc.line();
    }


    private void addAgronomyAnalysis(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("3. ANALISIS AGRONOMI");
        String saved = pref("last_field_analysis", "");
        if (saved.trim().isEmpty()) saved = pref("last_agronomy_analysis", "");
        if (saved.trim().isEmpty()) {
            double n = num(pref("soil_n", "")), pp = num(pref("soil_p", "")), k = num(pref("soil_k", ""));
            double nLow = num(pref("soil_n_low", "")), nHigh = num(pref("soil_n_high", ""));
            String method = pref("soil_test_method", "Metode tidak diketahui");
            String crop = pref("crop", "Tanaman pertanian"); int hst = (int)Math.round(num(pref("farm_hst", "-1")));
            rc.text("Belum ada analisis tersimpan. Ringkasan otomatis: fase " + AgronomyEngine.phase(hst, crop) + "; N " + AgronomyEngine.classifyN(n,nLow,nHigh) + "; P " + AgronomyEngine.classifyP(pp,method) + "; K " + AgronomyEngine.classifyK(k,method) + "; VPD " + AgronomyEngine.classifyVpd(num(pref("om_vpd", ""))) + ".");
        } else rc.text(saved);
        rc.line();
    }

    private void addOptRisk(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("4. PREDIKSI POTENSI SERANGAN OPT");
        String crop = pref("crop", "Tanaman pertanian");
        double t = num(pref("om_temp", "")), rh = num(pref("om_rh", "")), rain = num(pref("om_rain", "")), wind = num(pref("om_wind_speed", ""));
        rc.text(AgronomyEngine.optRisk(crop, t, rh, rain, wind, recentOpt(p, crop)));
        rc.text("Catatan: ini adalah peringatan dini berbasis cuaca + histori, bukan diagnosis pasti. Konfirmasi di lapangan melalui gejala, populasi, luas serangan, dan musuh alami.");
        rc.line();
    }

    private void addHistory(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("5. HISTORI LAPANGAN, PEMUPUKAN & OPT");
        List<HistoryItem> items = new ArrayList<>();
        collect(items, p.getString("field_notes_v153", "[]"), "LAPANG", "observation", "action");
        collect(items, p.getString("fert_history", "[]"), "PUPUK", "product", "note");
        collect(items, p.getString("opt_history", "[]"), "OPT", "target", "result");
        Collections.sort(items, Comparator.comparingLong(a -> -a.created));
        if (items.isEmpty()) rc.text("Belum ada histori tersimpan.");
        else for (int i = 0; i < Math.min(30, items.size()); i++) rc.text(items.get(i).toText());
        rc.line();
    }

    private void collect(List<HistoryItem> out, String json, String type, String mainKey, String secondKey) {
        try {
            JSONArray a = new JSONArray(json);
            for (int i=0;i<a.length();i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                HistoryItem h = new HistoryItem();
                h.created = o.optLong("created", 0); h.date = o.optString("date", "--");
                h.type = type; h.main = o.optString(mainKey, ""); h.second = o.optString(secondKey, "");
                h.extra = o.optString("cultivation", "");
                if ("PUPUK".equals(type)) h.extra += " | dosis " + o.optString("dose", "--") + " " + o.optString("unit", "");
                if ("OPT".equals(type)) h.extra += " | serangan " + o.optString("affectedPct", "--") + "% | metode " + o.optString("method", "");
                out.add(h);
            }
        } catch (Exception ignored) {}
    }

    private void addRecommendations(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("6. REKOMENDASI TEKNIS");
        String crop=pref("crop","Tanaman pertanian"); AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop); String mode=pref("farm_cultivation_mode","Konvensional / PHT");
        double ph=num(pref("soil_ph","")),ec=num(pref("soil_ec_us_cm","")),ece=num(pref("soil_ece_ds_m","")),m=num(pref("soil_moisture_pct",""));
        double vpd=num(pref("om_vpd","")),rain=num(pref("om_rain","")),et0=num(pref("om_et0","")); int hst=(int)Math.round(num(pref("farm_hst","-1")));
        if(Double.isFinite(ph)&&ph<cp.phMin)rc.text("- pH rendah: jangan menentukan dosis dolomit hanya dari pH. Pakai pH-buffer, Al-dd/H-dd, CEC atau kebutuhan kapur laboratorium.");
        if(Double.isFinite(ph)&&ph>cp.phMax)rc.text("- pH tinggi: hentikan sementara kapur/dolomit; periksa alkalinitas air dan dasar kebutuhan pengasaman.");
        if(Double.isFinite(ec)&&ec/1000.0>cp.ecThresholdDsM)rc.text("- EC sensor tinggi: kurangi pupuk pekat sekali aplikasi, cek drainase dan kualitas air; konfirmasi dengan ECe bila perlu.");
        if(Double.isFinite(ece)&&ece>=4)rc.text("- ECe menunjukkan salinitas sedikit-sangat tinggi menurut kelas USDA-NRCS; tindakan harus menyesuaikan toleransi tanaman.");
        if(Double.isFinite(m)&&m<20)rc.text("- Tanah cenderung kering: cek air zona akar sebelum memberi pupuk yang membutuhkan air untuk larut.");
        if(Double.isFinite(m)&&m>85)rc.text("- Tanah sangat lembap: cek drainase dan tunda input yang berisiko hilang.");
        if(Double.isFinite(vpd)&&vpd>2)rc.text("- VPD tinggi: pantau layu/kehilangan air; kombinasi VPD tinggi + tanah kering menjadi prioritas.");
        if(Double.isFinite(rain)&&Double.isFinite(et0)&&rain<et0)rc.text("- Hujan < ET0: cek cadangan air tanah dan forecast, jangan menetapkan irigasi dari hujan saja.");
        rc.text("- Fase: "+AgronomyEngine.phase(hst,crop)+". Kebutuhan pupuk mengikuti fase, target hasil, status tanah, efisiensi dan histori.");
        rc.text("- Model dosis: "+AgronomyEngine.nutrientFormulaText());
        rc.text(mode.toLowerCase(Locale.US).contains("organik") ? "- ORGANIK: gunakan input/proses yang diizinkan standar organik; prioritaskan bahan organik matang, sanitasi, rotasi, varietas toleran dan agen hayati." : "- KONVENSIONAL/PHT: monitoring dulu, kemudian tindakan; pestisida mengikuti label, interval pra-panen, APD dan rotasi bahan aktif.");
        rc.text("- OPT: gunakan hasil screening sebagai peringatan dini. Verifikasi gejala, populasi, luas serangan, stadia dan musuh alami.");
        rc.line();
    }


    private void addAi(ReportCanvas rc, android.content.SharedPreferences p) {
        String ai = pref("last_ai_advice", "");
        if (ai.trim().isEmpty()) return;
        rc.section("7. REKOMENDASI AI TERAKHIR");
        rc.text(ai);
        rc.line();
    }

    private void addSources(ReportCanvas rc) {
        rc.section("8. DASAR ILMIAH / CATATAN KETERBATASAN");
        rc.text("- FAO-56 Penman-Monteith untuk ET0 dan kerangka Kc/ETc.");
        rc.text("- Pendekatan status hara tanah / pemupukan spesifik lokasi; persamaan STCR yang presisi memerlukan kalibrasi tanah-komoditas-setempat.");
        rc.text("- PHT/IPM dan ambang ekonomi: keputusan pengendalian mengikuti hasil monitoring, bukan cuaca saja.");
        rc.text("- Model OPT berbasis cuaca adalah peringatan dini; verifikasi organisme dan serangan di lapangan tetap wajib.");
        rc.text("- pH: kebutuhan kapur sebaiknya memakai uji buffer/kemasaman tertukar jika dosis koreksi diperlukan.");
        rc.text("- EC sensor lapang adalah screening/tren; pembuktian salinitas formal menggunakan metode laboratorium yang sesuai.");
        rc.text("- FAO-56: Penman-Monteith, TAW/RAW/available soil water.\n- USDA-NRCS: kelas ECe salinitas.\n- UMN/soil testing guidance: pH-buffer diperlukan untuk kebutuhan kapur.\n- Mehlich-3: kelas P/K berbasis metode tidak disamakan dengan metode lain.\n- SNI 6729:2016: sistem pertanian organik Indonesia.\n- Disease forecasting: suhu + RH + hujan/leaf wetness; degree-day untuk serangga bila model Tbase tersedia.\n- Sumber pustaka rinci: SCIENTIFIC_SOURCES.md.");
    }

    private String recentOpt(android.content.SharedPreferences p, String crop) {
        try {
            JSONArray a = new JSONArray(p.getString("opt_history", "[]"));
            StringBuilder s = new StringBuilder(); int count=0;
            for (int i=a.length()-1;i>=0 && count<3;i--) {
                JSONObject o=a.optJSONObject(i); if(o==null)continue;
                if(!crop.equalsIgnoreCase(o.optString("crop", crop))) continue;
                if(count++>0)s.append("; ");
                s.append(o.optString("target","OPT"));
            }
            return s.length()==0?"tidak ada histori OPT terbaru":s.toString();
        } catch(Exception e){return "histori OPT tidak terbaca";}
    }

    private String phaseText(){ int h=(int)Math.round(num(pref("farm_hst","-1"))); return AgronomyEngine.phase(h,pref("crop","Tanaman pertanian")); }
    private String pref(String k,String def){ return getSharedPreferences(PREFS,MODE_PRIVATE).getString(k,def); }
    private static double num(String s){try{return s==null||s.trim().isEmpty()?Double.NaN:Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private static String show(double x){return Double.isFinite(x)?String.format(Locale.US,"%.2f",x):"--";}
    private static String safe(Throwable e){return e.getMessage()==null?"kesalahan tidak diketahui":e.getMessage();}

    private static final class HistoryItem {
        long created; String date,type,main,second,extra;
        String toText(){StringBuilder s=new StringBuilder();s.append(date).append(" | ").append(type).append(" | ").append(main);if(second!=null&&!second.isEmpty())s.append(" | ").append(second);if(extra!=null&&!extra.trim().isEmpty())s.append(" | ").append(extra);return s.toString();}
    }

    private static final class PdfPrintAdapter extends PrintDocumentAdapter {
        private final File file;
        private PdfRenderer renderer;
        private int pages;
        PdfPrintAdapter(File file){this.file=file;}
        @Override public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, android.os.Bundle extras) {
            try {
                if (cancellationSignal.isCanceled()) { callback.onLayoutCancelled(); return; }
                ParcelFileDescriptor pfd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
                renderer=new PdfRenderer(pfd); pages=renderer.getPageCount(); renderer.close(); renderer=null;
                PrintDocumentInfo info=new PrintDocumentInfo.Builder(file.getName()).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages).build();
                callback.onLayoutFinished(info,true);
            } catch(Exception e){callback.onLayoutFailed(e.getMessage());}
        }
        @Override public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
            try(InputStream in=new BufferedInputStream(new FileInputStream(file));OutputStream out=new BufferedOutputStream(new ParcelFileDescriptor.AutoCloseOutputStream(destination))){
                byte[] buf=new byte[32768];int n;while((n=in.read(buf))>=0){if(cancellationSignal.isCanceled()){callback.onWriteCancelled();return;}if(n>0)out.write(buf,0,n);}out.flush();callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }catch(Exception e){callback.onWriteFailed(e.getMessage());}
        }
    }

    private static final class ReportCanvas {
        private static final int W=595,H=842,LEFT=40,RIGHT=40,TOP=42,BOTTOM=42;
        private final PdfDocument doc; private PdfDocument.Page page; private Canvas c; private Paint title,head,body,small,line; private float y;
        ReportCanvas(PdfDocument doc){this.doc=doc; title=p(20,true);head=p(13,true);body=p(10,false);small=p(8,false);line=p(1,false);newPage();}
        private Paint p(float size,boolean bold){Paint x=new Paint(Paint.ANTI_ALIAS_FLAG);x.setColor(0xFF16232D);x.setTextSize(size);x.setTypeface(bold?Typeface.create(Typeface.DEFAULT,Typeface.BOLD):Typeface.DEFAULT);return x;}
        private void newPage(){if(page!=null)doc.finishPage(page);page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,doc.getPages().size()+1).create());c=page.getCanvas();y=TOP;}
        private void need(float h){if(y+h>BOTTOM){newPage();y=TOP;}}
        void heading(String s){need(34);c.drawText(s,LEFT,y,title);y+=26;}
        void subheading(String s){need(28);c.drawText(s,LEFT,y,head);y+=22;}
        void meta(String s){need(16);wrap(s,small,16);}
        void section(String s){need(25);y+=8;c.drawText(s,LEFT,y,head);y+=17;}
        void line(){need(12);c.drawLine(LEFT,y,W-RIGHT,y,line);y+=9;}
        void kv(String k,String v){need(17);c.drawText(k+":",LEFT,y,body);wrap("  "+v,body,17,145);}
        void text(String s){wrap(s,body,16);}
        private void wrap(String s,Paint paint,float lh){wrap(s,paint,lh,LEFT);}
        private void wrap(String s,Paint paint,float lh,float x){if(s==null)return;String t=s.replace('\n',' ');String[] words=t.split("\\s+");String cur="";float max=W-RIGHT-x;for(String w:words){String nxt=cur.isEmpty()?w:cur+" "+w;if(paint.measureText(nxt)>max){need(lh);c.drawText(cur,x,y,paint);y+=lh;cur=w;}else cur=nxt;}if(!cur.isEmpty()){need(lh);c.drawText(cur,x,y,paint);y+=lh;}}
        void finish(File out)throws Exception{if(page!=null)doc.finishPage(page);try(FileOutputStream f=new FileOutputStream(out)){doc.writeTo(f);}doc.close();}
    }
}
