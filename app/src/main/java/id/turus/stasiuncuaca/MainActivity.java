package id.turus.stasiuncuaca;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS="thingspeak_config";
    private static final ZoneId WIB=ZoneId.of("Asia/Jakarta");
    private static final long REFRESH_MS=15L*60L*1000L;
    private static final int LOCATION_REQ=77;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final ExecutorService net=Executors.newFixedThreadPool(2);
    private android.content.SharedPreferences prefs;
    private TextView title,dateTime,location,online,weather,temp,rh,pressure,rain,et0,vpd,wind,sun,radiation,apparent,dewpoint,cloud,visibility,uv,gust,forecast7d,soil,soilStatus,optRisk,overall,tsStatus;
    private TextView[] tsValues=new TextView[8]; private boolean loading;

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);prefs=getSharedPreferences(PREFS,Context.MODE_PRIVATE);bind();
        findViewById(R.id.btnSettings).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        findViewById(R.id.btnFieldNotes).setOnClickListener(v->startActivity(new Intent(this,FieldNotesActivity.class)));
        findViewById(R.id.btnAgronomy).setOnClickListener(v->startActivity(new Intent(this,AgronomyActivity.class)));
        findViewById(R.id.btnCsv).setOnClickListener(v->startActivity(new Intent(this,CsvDownloadActivity.class)));
        findViewById(R.id.btnPdfReport).setOnClickListener(v->startActivity(new Intent(this,PdfReportActivity.class)));
        findViewById(R.id.btnRefresh).setOnClickListener(v->loadAll());
        findViewById(R.id.btnGps).setOnClickListener(v->requestLocation());
        updateClock();handler.post(clockTick);
    }
    private final Runnable clockTick = new Runnable() { @Override public void run() { updateClock(); handler.postDelayed(this, 1000);}};
    private final Runnable refresh = new Runnable() { @Override public void run() { loadAll(); handler.postDelayed(this, REFRESH_MS);}};
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(refresh);loadAll();handler.postDelayed(refresh,REFRESH_MS);}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(refresh);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);net.shutdownNow();super.onDestroy();}

    private void bind(){
        title=findViewById(R.id.title);dateTime=findViewById(R.id.dateTime);location=findViewById(R.id.location);online=findViewById(R.id.online);
        weather=findViewById(R.id.weather);temp=findViewById(R.id.temp);rh=findViewById(R.id.rh);pressure=findViewById(R.id.pressure);rain=findViewById(R.id.rain);et0=findViewById(R.id.et0);vpd=findViewById(R.id.vpd);wind=findViewById(R.id.wind);sun=findViewById(R.id.sun);radiation=findViewById(R.id.radiation);
        apparent=findViewById(R.id.apparent);dewpoint=findViewById(R.id.dewpoint);cloud=findViewById(R.id.cloud);visibility=findViewById(R.id.visibility);uv=findViewById(R.id.uv);gust=findViewById(R.id.gust);forecast7d=findViewById(R.id.forecast7d);
        soil=findViewById(R.id.soil);soilStatus=findViewById(R.id.soilStatus);optRisk=findViewById(R.id.optRisk);overall=findViewById(R.id.overall);tsStatus=findViewById(R.id.tsStatus);
        for(int i=0;i<8;i++)tsValues[i]=findViewById(getResources().getIdentifier("ts"+(i+1),"id",getPackageName()));
    }
    private void updateClock(){ZonedDateTime n=ZonedDateTime.now(WIB);String t=prefs.getString("app_title","STASIUN CUACA").trim();title.setText(t.isEmpty()?"STASIUN CUACA":t);dateTime.setText(cap(n.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy • HH:mm:ss 'WIB'",new Locale("id","ID")))));}
    private String cap(String s){return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);}
    private void loadAll(){if(loading)return;loading=true;online.setText("MEMUAT OPEN-METEO...");net.execute(()->{try{Weather w=fetchOpenMeteo();try{fetchThingSpeak();}catch(Exception ignored){}runOnUiThread(()->{renderWeather(w);online.setText("ONLINE • OPEN-METEO");loading=false;});}catch(Exception e){runOnUiThread(()->{online.setText("OPEN-METEO GAGAL: "+msg(e));online.setTextColor(0xFFFF7777);loading=false;});}});}

    private Weather fetchOpenMeteo()throws Exception{
        double lat=prefs.getFloat("latitude",Float.NaN),lon=prefs.getFloat("longitude",Float.NaN);if(!Double.isFinite(lat)||!Double.isFinite(lon))throw new Exception("Koordinat belum diatur");
        String current="temperature_2m,relative_humidity_2m,apparent_temperature,dew_point_2m,precipitation,weather_code,surface_pressure,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,uv_index,shortwave_radiation,vapour_pressure_deficit,soil_temperature_0_to_10cm,soil_moisture_0_to_10cm";
        String daily="temperature_2m_max,temperature_2m_min,precipitation_sum,sunshine_duration,et0_fao_evapotranspiration,wind_direction_10m_dominant,uv_index_max,sunrise,sunset";
        String u="https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon+"&current="+current+"&daily="+daily+"&timezone="+URLEncoder.encode("Asia/Jakarta","UTF-8")+"&forecast_days=7";
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(15000);c.setRequestMethod("GET");c.setUseCaches(false);int code=c.getResponseCode();if(code!=200)throw new Exception("HTTP "+code);JSONObject root=new JSONObject(readAll(c.getInputStream()));c.disconnect();
        JSONObject cur=root.getJSONObject("current"),d=root.getJSONObject("daily");Weather w=new Weather();
        w.temp=cur.optDouble("temperature_2m",Double.NaN);w.rh=cur.optDouble("relative_humidity_2m",Double.NaN);w.apparent=cur.optDouble("apparent_temperature",Double.NaN);w.dewpoint=cur.optDouble("dew_point_2m",Double.NaN);w.rainNow=cur.optDouble("precipitation",Double.NaN);w.code=cur.optInt("weather_code",-1);w.pressure=cur.optDouble("surface_pressure",Double.NaN);w.cloud=cur.optDouble("cloud_cover",Double.NaN);w.windSpeed=cur.optDouble("wind_speed_10m",Double.NaN);w.windDir=cur.optDouble("wind_direction_10m",Double.NaN);w.gust=cur.optDouble("wind_gusts_10m",Double.NaN);w.visibility=cur.optDouble("visibility",Double.NaN);w.uv=cur.optDouble("uv_index",Double.NaN);w.sw=cur.optDouble("shortwave_radiation",Double.NaN);w.vpd=cur.optDouble("vapour_pressure_deficit",Double.NaN);w.soilTemp=cur.optDouble("soil_temperature_0_to_10cm",Double.NaN);w.soilMoisture=cur.optDouble("soil_moisture_0_to_10cm",Double.NaN);w.elevation=root.optDouble("elevation",Double.NaN);
        w.et0=first(d,"et0_fao_evapotranspiration");w.sunHours=first(d,"sunshine_duration")/3600.0;w.rainDay=first(d,"precipitation_sum");w.forecast=forecastSummary(d);return w;
    }
    private double first(JSONObject o,String key){JSONArray a=o.optJSONArray(key);return a==null||a.length()==0?Double.NaN:a.optDouble(0,Double.NaN);}
    private String forecastSummary(JSONObject d){JSONArray dates=d.optJSONArray("time"),tmax=d.optJSONArray("temperature_2m_max"),tmin=d.optJSONArray("temperature_2m_min"),rain=d.optJSONArray("precipitation_sum"),et=d.optJSONArray("et0_fao_evapotranspiration");StringBuilder s=new StringBuilder();if(dates==null)return "--";for(int i=0;i<dates.length()&&i<7;i++){if(i>0)s.append(" | ");s.append(dates.optString(i,"--")).append(" ").append(fmt(tmin==null?Double.NaN:tmin.optDouble(i,Double.NaN),0)).append("–").append(fmt(tmax==null?Double.NaN:tmax.optDouble(i,Double.NaN),0)).append("°C").append(" hujan ").append(fmt(rain==null?Double.NaN:rain.optDouble(i,Double.NaN),1)).append(" mm ET0 ").append(fmt(et==null?Double.NaN:et.optDouble(i,Double.NaN),1));}return s.toString();}
    private void fetchThingSpeak()throws Exception{String ch=prefs.getString("channel","").trim();if(ch.isEmpty())return;String key=prefs.getString("read_key","").trim();String u="https://api.thingspeak.com/channels/"+URLEncoder.encode(ch,"UTF-8")+"/feeds/last.json?timezone=Asia%2FJakarta&status=true"+(key.isEmpty()?"":"&api_key="+URLEncoder.encode(key,"UTF-8"));HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(8000);c.setRequestMethod("GET");if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());JSONObject o=new JSONObject(readAll(c.getInputStream()));c.disconnect();final String[] v=new String[8];for(int i=0;i<8;i++)v[i]=o.optString("field"+(i+1),"");runOnUiThread(()->{for(int i=0;i<8;i++)tsValues[i].setText(v[i].isEmpty()?"--":v[i]);tsStatus.setText("ThingSpeak: data terbaru berhasil dibaca");});}

    private void renderWeather(Weather w){
        temp.setText("Suhu "+fmt(w.temp,1)+" °C");rh.setText("RH "+fmt(w.rh,1)+" %");pressure.setText("Tekanan "+fmt(w.pressure,1)+" hPa");rain.setText("Hujan "+fmt(w.rainDay,1)+" mm/hari");et0.setText("ET0 FAO-56 "+fmt(w.et0,2)+" mm/hari");vpd.setText("VPD "+fmt(w.vpd,2)+" kPa • "+AgronomyEngine.classifyVpd(w.vpd));wind.setText("Angin "+compass(w.windDir)+" • "+fmt(w.windSpeed,1)+" m/s");sun.setText("Penyinaran "+fmt(w.sunHours,1)+" jam/hari");radiation.setText("Radiasi "+fmt(w.sw,0)+" W/m²");weather.setText(wmo(w.code));
        apparent.setText("Suhu terasa "+fmt(w.apparent,1)+" °C");dewpoint.setText("Titik embun "+fmt(w.dewpoint,1)+" °C");cloud.setText("Awan "+fmt(w.cloud,0)+" %");visibility.setText("Visibilitas "+fmt(w.visibility,0)+" m");uv.setText("UV "+fmt(w.uv,1));gust.setText("Gust "+fmt(w.gust,1)+" m/s");forecast7d.setText("Forecast 7 hari: "+w.forecast);
        String crop=prefs.getString("crop","Tanaman pertanian");double n=num(prefs.getString("soil_n","")),p=num(prefs.getString("soil_p","")),k=num(prefs.getString("soil_k","")),ph=num(prefs.getString("soil_ph","")),ec=num(prefs.getString("soil_ec_us_cm","")),moist=num(prefs.getString("soil_moisture_pct","")),ece=num(prefs.getString("soil_ece_ds_m",""));AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop);
        prefs.edit().putString("om_temp",fmt(w.temp,2)).putString("om_rh",fmt(w.rh,2)).putString("om_apparent_temp",fmt(w.apparent,2)).putString("om_dewpoint",fmt(w.dewpoint,2)).putString("om_pressure",fmt(w.pressure,2)).putString("om_rain",fmt(w.rainDay,2)).putString("om_et0",fmt(w.et0,2)).putString("om_vpd",fmt(w.vpd,3)).putString("om_wind_speed",fmt(w.windSpeed,2)).putString("om_wind_direction",compass(w.windDir)).putString("om_wind_gust",fmt(w.gust,2)).putString("om_cloud_cover",fmt(w.cloud,0)).putString("om_visibility",fmt(w.visibility,0)).putString("om_uv",fmt(w.uv,1)).putString("om_sun_hours",fmt(w.sunHours,2)).putString("om_radiation",fmt(w.sw,2)).putString("om_soil_temp",fmt(w.soilTemp,2)).putString("om_soil_moisture",fmt(w.soilMoisture,2)).putString("om_weather",wmo(w.code)).putString("om_forecast_7d",w.forecast).apply();
        soil.setText("pH "+fmt(ph,2)+" • N "+fmt(n,1)+" • P "+fmt(p,1)+" • K "+fmt(k,1)+" mg/kg\\nEC "+fmt(ec,0)+" µS/cm • kelembapan "+fmt(moist,1)+" %");
        soilStatus.setText("pH: "+AgronomyEngine.classifyPH(ph,cp)+"\\nP: "+AgronomyEngine.classifyP(p,prefs.getString("soil_test_method",""))+" • K: "+AgronomyEngine.classifyK(k,prefs.getString("soil_test_method",""))+" • N: "+AgronomyEngine.classifyN(n)+"\\nEC sensor: "+AgronomyEngine.classifyEC(ec,cp)+"\\nECe lab: "+(Double.isFinite(ece)?AgronomyEngine.classifyECe(ece):"belum ada")+"\\nAir tanah: "+AgronomyEngine.soilWaterAssessment(moist,num(prefs.getString("soil_fc_pct","")),num(prefs.getString("soil_pwp_pct","")),num(prefs.getString("soil_depth_cm","20")),w.et0,crop));
        int hst = (int)Math.round(num(prefs.getString("farm_hst","-1")));
        optRisk.setText(AgronomyEngine.optRisk(crop,w.temp,w.rh,w.rainDay,w.windSpeed,recentOpt(),hst));overall.setText(buildOverall(w,crop));location.setText("Lokasi: "+fmt(prefs.getFloat("latitude",0),5)+", "+fmt(prefs.getFloat("longitude",0),5)+" • elevasi "+fmt(w.elevation,0)+" m");
    }
    private String buildOverall(Weather w,String crop){int hst = (int)Math.round(num(prefs.getString("farm_hst","-1")));StringBuilder s=new StringBuilder("REKOMENDASI KESELURUHAN\n");;AgronomyEngine.CropProfile p=AgronomyEngine.profile(crop);double ph=num(prefs.getString("soil_ph","")),ec=num(prefs.getString("soil_ec_us_cm","")),moist=num(prefs.getString("soil_moisture_pct",""));if(Double.isFinite(w.vpd)&&w.vpd>2)s.append("• VPD tinggi. Bersamaan tanah kering, risiko stress lebih serius. Cek air zona akar.\\n");if(Double.isFinite(w.et0)&&Double.isFinite(w.rainDay)&&w.rainDay<w.et0)s.append("• Hujan < ET0. Jangan menambah irigasi hanya dari angka hujan; cek cadangan air tanah.\\n");if(Double.isFinite(ph)&&(ph<p.phMin||ph>p.phMax))s.append("• pH di luar kisaran. Jangan menghitung kapur dari pH saja; gunakan pH-buffer/Al-dd/H-dd/CEC atau rekomendasi laboratorium.\\n");if(Double.isFinite(ec)&&ec/1000.0>p.ecThresholdDsM)s.append("• EC sensor tinggi. Kurangi pemupukan pekat dan cek air/drainase; konfirmasi ECe bila perlu.\\n");if(Double.isFinite(moist)&&moist<20)s.append("• Sensor menunjukkan tanah kering. Prioritaskan cek air sebelum pupuk larut.\\n");if(w.rh>90&&w.rainDay>5)s.append("• RH + hujan tinggi. Tingkatkan scouting penyakit dan sanitasi.\\n");s.append("• ").append(AgronomyEngine.optRisk(crop,w.temp,w.rh,w.rainDay,w.windSpeed,"",hst));return s.toString();}
    private String recentOpt(){try{JSONArray a=new JSONArray(prefs.getString("opt_history","[]"));StringBuilder s=new StringBuilder();for(int i=Math.max(0,a.length()-5);i<a.length();i++)s.append(a.optJSONObject(i)).append("; ");return s.toString();}catch(Exception e){return "";}}

    private void requestLocation(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQ);return;}try{LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);Location best=null;for(String p:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){if(!lm.isProviderEnabled(p))continue;Location x=lm.getLastKnownLocation(p);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}if(best==null){Toast.makeText(this,"Lokasi terakhir HP belum tersedia.",Toast.LENGTH_SHORT).show();return;}prefs.edit().putFloat("latitude",(float)best.getLatitude()).putFloat("longitude",(float)best.getLongitude()).apply();Toast.makeText(this,"Koordinat HP disimpan.",Toast.LENGTH_SHORT).show();loadAll();}catch(Exception e){Toast.makeText(this,"Gagal membaca lokasi HP.",Toast.LENGTH_SHORT).show();}}
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] grants){super.onRequestPermissionsResult(req,perms,grants);if(req==LOCATION_REQ){for(int g:grants)if(g==PackageManager.PERMISSION_GRANTED){requestLocation();return;}Toast.makeText(this,"Izin lokasi tidak diberikan.",Toast.LENGTH_SHORT).show();}}
    private double num(String s){try{return s==null||s.trim().isEmpty()?Double.NaN:Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private String fmt(double v,int d){return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"--";}
    private String msg(Exception e){return e.getMessage()==null?"kesalahan tidak diketahui":e.getMessage();}
    private String readAll(InputStream in)throws Exception{if(in==null)return "";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)b.append(l);}return b.toString();}
    public static String compass(double deg){if(!Double.isFinite(deg))return "--";String[] p={"Utara","Utara-Timur Laut","Timur Laut","Timur-Timur Laut","Timur","Timur-Tenggara","Tenggara","Selatan-Tenggara","Selatan","Selatan-Barat Daya","Barat Daya","Barat-Barat Daya","Barat","Barat-Barat Laut","Barat Laut","Utara-Barat Laut"};int i=(int)Math.floor((deg+11.25)/22.5)%16;return p[i];}
    private String wmo(int c){switch(c){case 0:return "Cerah";case 1:case 2:return "Cerah berawan / sebagian berawan";case 3:return "Berawan";case 45:case 48:return "Kabut";case 51:case 53:case 55:return "Gerimis";case 61:case 63:case 65:return "Hujan";case 66:case 67:return "Hujan beku";case 71:case 73:case 75:return "Salju";case 80:case 81:case 82:return "Hujan deras lokal";case 95:return "Badai petir";default:return "Kode WMO "+c;}}
    private static final class Weather{double temp,rh,apparent,dewpoint,rainNow,pressure,cloud,windSpeed,windDir,gust,visibility,uv,sw,vpd,soilTemp,soilMoisture,et0,sunHours,rainDay,elevation;int code;String forecast;}
}
