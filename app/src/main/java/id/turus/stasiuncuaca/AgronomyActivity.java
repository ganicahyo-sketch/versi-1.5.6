package id.turus.stasiuncuaca;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Comprehensive agronomy dashboard and evidence-assisted AI report. */
public class AgronomyActivity extends Activity {
    private static final String PREFS="thingspeak_config";
    private final ExecutorService net=Executors.newSingleThreadExecutor();
    private android.content.SharedPreferences prefs;
    private TextView report, aiStatus;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        setContentView(R.layout.activity_agronomy);
        report=findViewById(R.id.agroReport); aiStatus=findViewById(R.id.agroAiStatus);
        findViewById(R.id.agroBack).setOnClickListener(v->finish());
        findViewById(R.id.agroFieldNotes).setOnClickListener(v->startActivity(new Intent(this,FieldNotesActivity.class)));
        findViewById(R.id.agroPdf).setOnClickListener(v->startActivity(new Intent(this,PdfReportActivity.class)));
        findViewById(R.id.agroAiButton).setOnClickListener(v->requestAi());
        render();
    }
    @Override protected void onDestroy(){net.shutdownNow();super.onDestroy();}

    private void render(){
        String crop=prefs.getString("crop","Tanaman pertanian");
        int hst=(int)Math.round(num(prefs.getString("farm_hst","-1")));
        AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop);
        String mode=prefs.getString("farm_cultivation_mode","Konvensional / PHT");
        String method=prefs.getString("soil_test_method","Metode tidak diketahui");
        double ph=num(prefs.getString("soil_ph","")),n=num(prefs.getString("soil_n","")),p=num(prefs.getString("soil_p","")),k=num(prefs.getString("soil_k","")); 
        double nLow = num(prefs.getString("soil_n_low",""));
        double nHigh = num(prefs.getString("soil_n_high",""));
        double ec=num(prefs.getString("soil_ec_us_cm","")),ece=num(prefs.getString("soil_ece_ds_m","")),moist=num(prefs.getString("soil_moisture_pct",""));
        double bd=num(prefs.getString("soil_bulk_density_g_cm3","1.30")),depth=num(prefs.getString("soil_depth_cm","20")),fc=num(prefs.getString("soil_fc_pct","")),pwp=num(prefs.getString("soil_pwp_pct",""));
        double phBuf=num(prefs.getString("soil_ph_buffer","")),al=num(prefs.getString("soil_al_dd","")),hd=num(prefs.getString("soil_h_dd","")),cec=num(prefs.getString("soil_cec","")),om=num(prefs.getString("soil_om_pct","")),lime=num(prefs.getString("soil_lime_requirement_kg_ha",""));
        double temp=num(prefs.getString("om_temp","")),rh=num(prefs.getString("om_rh","")),rain=num(prefs.getString("om_rain","")),et0=num(prefs.getString("om_et0","")),vpd=num(prefs.getString("om_vpd",""));
        StringBuilder s=new StringBuilder();
        s.append("ANALISIS AGRONOMI TERPADU v1.5.6\n\n");
        s.append("KOMODITAS: ").append(crop).append("\nMODE: ").append(mode).append("\nHST: ").append(hst<0?"--":hst).append("\nFASE: ").append(AgronomyEngine.phase(hst,crop)).append("\nLUAS: ").append(pref("farm_area_ha","--")).append(" ha\nTARGET HASIL: ").append(pref("farm_target_yield_t_ha","--")).append(" t/ha\n\n");
        s.append("1. TANAH\n");
        s.append("pH ").append(show(ph)).append(" -> ").append(AgronomyEngine.classifyPH(ph,cp)).append("\n");
        s.append("Tindakan pH: ").append(AgronomyEngine.limeAdvice(ph,cp,mode,phBuf,al,hd,cec,om,lime)).append("\n");
        s.append("Metode N/P/K: ").append(method).append("\n");
        s.append("N tersedia ").append(show(n)).append(" mg/kg -> ").append(AgronomyEngine.classifyN(n,nLow,nHigh)).append("\n");
        s.append("P tersedia ").append(show(p)).append(" mg/kg -> ").append(AgronomyEngine.classifyP(p,method)).append("\n");
        s.append("K tersedia ").append(show(k)).append(" mg/kg -> ").append(AgronomyEngine.classifyK(k,method)).append("\n");
        s.append("EC sensor ").append(show(ec)).append(" µS/cm -> ").append(AgronomyEngine.classifyEC(ec,cp)).append("\n");
        if(Double.isFinite(ece))s.append("ECe lab ").append(show(ece)).append(" dS/m -> ").append(AgronomyEngine.classifyECe(ece)).append("\n");
        else s.append("ECe lab: belum tersedia; EC sensor tidak dikonversi otomatis menjadi ECe.\n");
        s.append("Kelembapan tanah ").append(show(moist)).append(" % -> ").append(AgronomyEngine.classifyMoisture(moist)).append("\n");
        s.append("FC/PWP: ").append(show(fc)).append(" / ").append(show(pwp)).append(" % volume -> ").append(AgronomyEngine.soilWaterAssessment(moist,fc,pwp,depth,et0,crop)).append("\n");
        if(Double.isFinite(depth)&&Double.isFinite(bd)){
            s.append("Stok lapisan ").append(show(depth)).append(" cm; BD ").append(show(bd)).append(" g/cm³: N ").append(show(AgronomyEngine.soilStockKgHa(n,bd,depth))).append("; P ").append(show(AgronomyEngine.soilStockKgHa(p,bd,depth))).append("; K ").append(show(AgronomyEngine.soilStockKgHa(k,bd,depth))).append(" kg/ha.\n");
            s.append("Stok = konsentrasi × BD × kedalaman × 0,01; stok ≠ serapan langsung tanaman.\n");
        }
        s.append("\n2. NPK & PREDIKSI KEBUTUHAN FASE\n");
        double target=num(pref("farm_target_yield_t_ha",""));
        double nn=AgronomyEngine.nutrientNeed(n,"N",crop,hst,fertCredit(crop)[0],target,method,depth,bd);
        double pp=AgronomyEngine.nutrientNeed(p,"P",crop,hst,fertCredit(crop)[1],target,method,depth,bd);
        double kk=AgronomyEngine.nutrientNeed(k,"K",crop,hst,fertCredit(crop)[2],target,method,depth,bd);
        s.append("N skrining fase: ").append(show(nn)).append(" kg N/ha\nP2O5 skrining fase: ").append(show(pp)).append(" kg P2O5/ha\nK2O skrining fase: ").append(show(kk)).append(" kg K2O/ha\n");
        s.append("Rumus skrining: ").append(AgronomyEngine.nutrientFormulaText()).append("\n");
        s.append("PENTING: bukan QUEFTS/STCR penuh. Gunakan persamaan STCR/PUTS lokal bila tersedia.\n");
        s.append("\n3. CUACA, VPD, AIR & EKOLOGI\n");
        s.append("Suhu ").append(show(temp)).append(" °C -> ").append(AgronomyEngine.temperatureStatus(temp,crop)).append("; kisaran profil ").append(show(cp.tempMin)).append("–").append(show(cp.tempMax)).append(" °C\n");
        s.append("RH ").append(show(rh)).append(" %; hujan ").append(show(rain)).append(" mm/hari; ET0 ").append(show(et0)).append(" mm/hari\n");
        s.append("VPD ").append(show(vpd)).append(" kPa -> ").append(AgronomyEngine.classifyVpd(vpd)).append("\n");
        s.append(AgronomyEngine.vpdCombinedStatus(vpd,moist,fc,pwp,depth,et0,crop)).append("\n");
        s.append("Angin ").append(pref("om_wind_direction","--")).append(" • ").append(pref("om_wind_speed","--")).append(" m/s • gust ").append(pref("om_wind_gust","--")).append(" m/s\n");
        s.append("Terasa ").append(pref("om_apparent_temp","--")).append(" °C • titik embun ").append(pref("om_dewpoint","--")).append(" °C • awan ").append(pref("om_cloud_cover","--")).append(" % • visibilitas ").append(pref("om_visibility","--")).append(" m • UV ").append(pref("om_uv","--")).append("\n");
        s.append("Radiasi ").append(pref("om_radiation","--")).append(" W/m² • sunshine ").append(pref("om_sun_hours","--")).append(" jam/hari\n");
        s.append("Forecast 7 hari: ").append(pref("om_forecast_7d","belum ada")).append("\n");
        s.append("\n4. PREDIKSI POTENSI OPT — RISK SCREENING\n");
        s.append(AgronomyEngine.optRisk(crop,temp,rh,rain,num(pref("om_wind_speed","")),recent("opt_history",6),hst)).append("\n");
        s.append("\n5. REKOMENDASI TEKNIS\n");
        addRecommendations(s,crop,mode,ph,ec,ece,moist,fc,pwp,vpd,temp,rh,rain,et0,hst,method);
        s.append("\n6. CATATAN ILMIAH\n").append(AgronomyEngine.evidenceBrief()).append("\n");
        s.append("\n7. HISTORI TERPADU (ringkas)\n").append(recent("field_notes_v153",8)).append("\nPemupukan:\n").append(recent("fert_history",8)).append("\nOPT:\n").append(recent("opt_history",8));
        report.setText(s.toString()); prefs.edit().putString("last_agronomy_analysis",s.toString()).apply();
    }

    private void addRecommendations(StringBuilder s,String crop,String mode,double ph,double ec,double ece,double moist,double fc,double pwp,double vpd,double t,double rh,double rain,double et0,int hst,String method){
        AgronomyEngine.CropProfile p=AgronomyEngine.profile(crop);
        if(Double.isFinite(ph)&&ph<p.phMin) s.append("• pH rendah: lakukan uji pH-buffer/Al-dd/H-dd/CEC atau gunakan kebutuhan kapur laboratorium; jangan hitung dolomit dari pH saja.\n");
        if(Double.isFinite(ph)&&ph>p.phMax) s.append("• pH tinggi: stop sementara kapur/dolomit; periksa alkalinitas air dan dasar kebutuhan pengasaman.\n");
        if(Double.isFinite(ec)&&ec/1000.0>p.ecThresholdDsM) s.append("• EC sensor tinggi: kurangi pemupukan pekat, cek air/drainase, dan pantau tren. Konfirmasi dengan ECe bila salinitas dicurigai.\n");
        if(Double.isFinite(ece)&&ece>=4) s.append("• ECe menunjukkan salinitas sedikit sampai sangat tinggi menurut kelas USDA-NRCS; pilih tindakan berdasarkan toleransi komoditas dan pemeriksaan zona akar.\n");
        String water=AgronomyEngine.soilWaterAssessment(moist,fc,pwp,20,et0,crop);
        if(water.startsWith("KURANG")) s.append("• Air tanah kurang: cek zona akar dan kebutuhan irigasi sebelum menambah pupuk larut.\n");
        if(Double.isFinite(vpd)&&vpd>2) s.append("• VPD tinggi: pantau layu/kerontokan bunga atau gejala kehilangan air; kombinasi dengan tanah kering lebih serius.\n");
        if(Double.isFinite(t)&& (t<p.tempMin||t>p.tempMax)) s.append("• Suhu di luar kisaran ekologis profil: kurangi pekerjaan stres pada tanaman dan pantau gejala.\n");
        if(Double.isFinite(rh)&&rh>90&&Double.isFinite(rain)&&rain>=5) s.append("• RH + hujan tinggi: tingkatkan scouting penyakit, sanitasi dan sirkulasi udara.\n");
        if(mode.toLowerCase(Locale.US).contains("organik")) s.append("• ORGANIK: gunakan input/proses yang diizinkan standar organik yang berlaku; prioritaskan bahan organik matang, sanitasi, varietas toleran, mekanis dan agen hayati. Verifikasi status input sebelum aplikasi.\n");
        else s.append("• KONVENSIONAL/PHT: gunakan pemupukan berimbang, monitoring ambang tindakan, rotasi bahan aktif dan patuhi label/interval pra-panen.\n");
        s.append("• Fase: ").append(AgronomyEngine.phase(hst,crop)).append(". Prioritaskan tindakan berdasarkan fase dan data lapang, bukan umur saja.\n");
    }

    private double[] fertCredit(String crop){double n=0,p=0,k=0;try{JSONArray a=new JSONArray(prefs.getString("fert_history","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!same(o.optString("crop",""),crop)||!"kg/ha".equalsIgnoreCase(o.optString("unit","kg/ha")))continue;double d=o.optDouble("dose",0);n+=d*o.optDouble("nPct",0)/100;p+=d*o.optDouble("pPct",0)/100;k+=d*o.optDouble("kPct",0)/100;}}catch(Exception ignored){}return new double[]{n,p,k};}
    private boolean same(String a,String b){return AgronomyEngine.normalizeCrop(a).equalsIgnoreCase(AgronomyEngine.normalizeCrop(b));}
    private String recent(String key,int max){try{JSONArray a=new JSONArray(prefs.getString(key,"[]"));StringBuilder s=new StringBuilder();for(int i=Math.max(0,a.length()-max);i<a.length();i++)s.append(a.optJSONObject(i)).append("\n");return s.toString().trim().isEmpty()?"(tidak ada)":s.toString();}catch(Exception e){return "(tidak ada)";}}
    private void requestAi(){
        String key=prefs.getString("ai_api_key","").trim(); if(key.isEmpty()){aiStatus.setText("Masukkan OpenAI API key di Pengaturan.");return;}
        final TextView button=findViewById(R.id.agroAiButton); button.setEnabled(false); aiStatus.setText("Mencari pustaka ilmiah dan menyusun analisis AI...");
        final String model=prefs.getString("ai_model","gpt-6-luna").trim().isEmpty()?"gpt-6-luna":prefs.getString("ai_model","gpt-6-luna").trim();
        final String context=report.getText().toString();
        net.execute(()->{try{String lit=fetchOpenAlex(prefs.getString("crop","Tanaman pertanian"), context);String ans=callAi(key,model,context,lit);prefs.edit().putString("last_ai_advice",ans).putLong("last_ai_advice_epoch",System.currentTimeMillis()).apply();runOnUiThread(()->{aiStatus.setText(ans);button.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{aiStatus.setText("AI gagal: "+msg(e));button.setEnabled(true);});}});
    }
    private String fetchOpenAlex(String crop,String context)throws Exception{
        String q=""+crop+" agriculture soil fertility pest disease weather forecasting";
        String url="https://api.openalex.org/works?search="+URLEncoder.encode(q,"UTF-8")+"&filter=from_publication_date:2018-01-01&per-page=6&sort=relevance_score:desc";
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod("GET");if(c.getResponseCode()!=200)throw new Exception("OpenAlex HTTP "+c.getResponseCode());JSONObject r=new JSONObject(readAll(c.getInputStream()));JSONArray a=r.optJSONArray("results");StringBuilder out=new StringBuilder();if(a!=null)for(int i=0;i<a.length();i++){JSONObject w=a.optJSONObject(i);if(w==null)continue;out.append(i+1).append(") ").append(w.optString("display_name",""));out.append(" | year=").append(w.optInt("publication_year",0));String doi=w.optString("doi","");if(!doi.isEmpty())out.append(" | ").append(doi);out.append("\n");}return out.length()==0?"Tidak ada hasil OpenAlex yang cocok.":out.toString();
    }
    private String callAi(String key,String model,String ctx,String lit)throws Exception{
        JSONObject p=new JSONObject();p.put("model",model);p.put("store",false);p.put("max_output_tokens",1800);
        p.put("instructions","Anda adalah agronom pendamping petani Indonesia. Pisahkan FAKTA TERUKUR, HASIL MODEL SCREENING, dan ASUMSI/DATA YANG BELUM ADA. Jelaskan pH berdasarkan profil komoditas; jangan menghitung kapur dari pH saja; gunakan pH-buffer/Al-dd/H-dd/CEC/OM atau kebutuhan laboratorium bila ada. Bedakan EC sensor dari ECe; jangan konversi otomatis. N harus diklasifikasikan hanya bila metode/ambang memadai. P/K gunakan kelas metode bila dipilih. Bedakan stok tanah dari serapan tanaman. Untuk kebutuhan pupuk, sebut model sebagai screening bila koefisien lokal/QUEFTS/STCR tidak tersedia. Prediksi OPT harus menyebut nama hama/penyakit bila pustaka/riwayat mendukung, tetapi tetap risk screening, bukan diagnosis. Untuk mode organik patuhi persyaratan sistem organik yang berlaku. Berikan langkah sederhana khas petani untuk 0-24 jam dan 1-7 hari. Jangan membuat dosis pestisida baru.");
        p.put("input","DATA DAN ANALISIS AWAL:\n"+ctx+"\n\nHASIL PUSTAKA OPENALEX:\n"+lit+"\n\nBuat rekomendasi komprehensif, teknis tetapi mudah dipahami.");
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.openai.com/v1/responses").openConnection();c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(40000);c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");byte[] b=p.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream o=c.getOutputStream()){o.write(b);}int code=c.getResponseCode();if(code<200||code>=300){String er=readAll(c.getErrorStream());throw new Exception("OpenAI HTTP "+code+(er.isEmpty()?"":" — "+er.substring(0,Math.min(400,er.length()))));}JSONObject r=new JSONObject(readAll(c.getInputStream()));String t=r.optString("output_text","").trim();if(!t.isEmpty())return t;JSONArray out=r.optJSONArray("output");if(out!=null){StringBuilder z=new StringBuilder();for(int i=0;i<out.length();i++){JSONObject it=out.optJSONObject(i);JSONArray cc=it==null?null:it.optJSONArray("content");if(cc==null)continue;for(int j=0;j<cc.length();j++){JSONObject part=cc.optJSONObject(j);if(part!=null&&"output_text".equals(part.optString("type")))z.append(part.optString("text","")).append("\n");}}if(z.length()>0)return z.toString().trim();}throw new Exception("Respons AI tidak berisi teks.");
    }
    private String pref(String k,String d){return prefs.getString(k,d);}
    private double num(String s){try{return s==null||s.trim().isEmpty()?Double.NaN:Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private String show(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"--";}
    private String readAll(InputStream in)throws Exception{if(in==null)return "";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)b.append(l);}return b.toString();}
    private String msg(Exception e){return e.getMessage()==null?"kesalahan tidak diketahui":e.getMessage();}
}
