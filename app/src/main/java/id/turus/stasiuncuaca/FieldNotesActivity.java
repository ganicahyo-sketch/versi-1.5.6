package id.turus.stasiuncuaca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

/**
 * Catatan Lapangan + mesin analisis agronomi v1.5.6.
 *
 * Semua nilai CWT-7in1 (RS485/USB) dianggap benar sebagai input proyek.
 * Interpretasi tetap diberi label screening bila standar sangat tergantung
 * metode ekstraksi/laboratorium.
 */
public class FieldNotesActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    private static final String KEY_NOTES = "field_notes_v153";
    private static final String KEY_FERT = "fert_history";
    private static final String KEY_OPT = "opt_history";
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    private EditText date, time, crop, plantDate, hst, area, observation, action, targetYield;
    private EditText soilPh, soilMoisture, soilTemp, soilEc, soilN, soilP, soilK, soilDepth, soilBulkDensity, soilFc, soilPwp, soilPhBuffer, soilAlDd, soilHDd, soilCec, soilOm, soilEce, soilLimeReq, soilNLow, soilNHigh;
    private Spinner soilTestMethod, ageUnit;
    private EditText airTemp, airRh, pressure, rain24, et0, lux, par, sunHours, windSpeed;
    private Spinner noteType, severity, soilSource, windDirection, cultivation;
    private TextView autoPhase, analysisView, timelineView, soilSummaryView;

    private static final String[] NOTE_TYPES = {
            "Pengamatan umum", "Tanah", "Pemupukan", "OPT", "Irigasi/air",
            "Pertumbuhan", "Panen", "Lainnya"
    };
    private static final String[] SEVERITY = {
            "Tidak ada", "Ringan", "Sedang", "Berat", "Sangat berat"
    };
    private static final String[] SOIL_SOURCE = {
            "CWT-7in1 RS485", "CWT-7in1 USB", "Input manual"
    };
    private static final String[] CULTIVATION = {
            "Konvensional / PHT", "Organik"
    };
    private static final String[] WIND = {
            "Tenang", "Utara", "Utara-Timur Laut", "Timur Laut", "Timur-Timur Laut",
            "Timur", "Timur-Tenggara", "Tenggara", "Selatan-Tenggara", "Selatan",
            "Selatan-Barat Daya", "Barat Daya", "Barat-Barat Daya", "Barat",
            "Barat-Barat Laut", "Barat Laut", "Utara-Barat Laut", "Variabel"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_notes);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        date = findViewById(R.id.fnDate);
        time = findViewById(R.id.fnTime);
        crop = findViewById(R.id.fnCrop);
        plantDate = findViewById(R.id.fnPlantDate);
        hst = findViewById(R.id.fnHst);
        area = findViewById(R.id.fnArea);
        observation = findViewById(R.id.fnObservation);
        action = findViewById(R.id.fnAction);
        targetYield = findViewById(R.id.fnTargetYield);

        soilPh = findViewById(R.id.fnSoilPh);
        soilMoisture = findViewById(R.id.fnSoilMoisture);
        soilTemp = findViewById(R.id.fnSoilTemp);
        soilEc = findViewById(R.id.fnSoilEc);
        soilN = findViewById(R.id.fnSoilN);
        soilP = findViewById(R.id.fnSoilP);
        soilK = findViewById(R.id.fnSoilK);
        soilDepth = findViewById(R.id.fnSoilDepth);
        soilBulkDensity = findViewById(R.id.fnSoilBulkDensity);
        soilFc = findViewById(R.id.fnSoilFc);
        soilPwp = findViewById(R.id.fnSoilPwp);
        soilPhBuffer = findViewById(R.id.fnSoilPhBuffer);
        soilAlDd = findViewById(R.id.fnSoilAlDd);
        soilHDd = findViewById(R.id.fnSoilHDd);
        soilCec = findViewById(R.id.fnSoilCec);
        soilOm = findViewById(R.id.fnSoilOm);
        soilEce = findViewById(R.id.fnSoilEce);
        soilLimeReq = findViewById(R.id.fnSoilLimeReq);
        soilTestMethod = findViewById(R.id.fnSoilTestMethod);
        soilNLow = findViewById(R.id.fnSoilNLow); soilNHigh = findViewById(R.id.fnSoilNHigh);
        ageUnit = findViewById(R.id.fnAgeUnit);

        airTemp = findViewById(R.id.fnAirTemp);
        airRh = findViewById(R.id.fnAirRh);
        pressure = findViewById(R.id.fnPressure);
        rain24 = findViewById(R.id.fnRain24);
        et0 = findViewById(R.id.fnEt0);
        lux = findViewById(R.id.fnLux);
        par = findViewById(R.id.fnPar);
        sunHours = findViewById(R.id.fnSunHours);
        windSpeed = findViewById(R.id.fnWindSpeed);

        noteType = findViewById(R.id.fnNoteType);
        severity = findViewById(R.id.fnSeverity);
        soilSource = findViewById(R.id.fnSoilSource);
        windDirection = findViewById(R.id.fnWindDirection);
        cultivation = findViewById(R.id.fnCultivation);

        autoPhase = findViewById(R.id.fnAutoPhase);
        analysisView = findViewById(R.id.fnAnalysis);
        timelineView = findViewById(R.id.fnTimeline);
        soilSummaryView = findViewById(R.id.fnSoilSummary);

        bindSpinner(noteType, NOTE_TYPES);
        bindSpinner(severity, SEVERITY);
        bindSpinner(soilSource, SOIL_SOURCE);
        bindSpinner(soilTestMethod, new String[]{"Metode tidak diketahui", "Mehlich-3", "Mehlich-3 ICP", "Olsen", "NH4OAc"});
        bindSpinner(windDirection, WIND);
        bindSpinner(cultivation, CULTIVATION);
        String savedSoilSource = prefs.getString("soil_source", "");
        if (!savedSoilSource.isEmpty()) selectSpinner(soilSource, savedSoilSource);
        String savedCultivation = prefs.getString("farm_cultivation_mode", "");
        if (!savedCultivation.isEmpty()) {
            for (int i = 0; i < CULTIVATION.length; i++) {
                if (CULTIVATION[i].equalsIgnoreCase(savedCultivation)) { cultivation.setSelection(i); break; }
            }
        }

        LocalDateTime now = LocalDateTime.now(WIB);
        date.setText(now.format(DATE_FMT));
        time.setText(now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)));
        crop.setText(prefs.getString("crop", ""));
        area.setText(prefs.getString("farm_area_ha", ""));
        plantDate.setText(prefs.getString("farm_planting_date", ""));
        hst.setText(prefs.getString("farm_hst", ""));
        String savedAgeValue=prefs.getString("farm_age_value", ""); if (!savedAgeValue.isEmpty()) hst.setText(savedAgeValue);
        selectSpinner(ageUnit,prefs.getString("farm_age_unit","HST / hari"));
        targetYield.setText(prefs.getString("farm_target_yield_t_ha", ""));
        soilPh.setText(prefs.getString("soil_ph", ""));
        soilMoisture.setText(prefs.getString("soil_moisture_pct", ""));
        soilEc.setText(prefs.getString("soil_ec_us_cm", ""));
        soilN.setText(prefs.getString("soil_n", ""));
        soilP.setText(prefs.getString("soil_p", ""));
        soilK.setText(prefs.getString("soil_k", "")); soilNLow.setText(prefs.getString("soil_n_low","")); soilNHigh.setText(prefs.getString("soil_n_high",""));
        soilDepth.setText(prefs.getString("soil_depth_cm", "20"));
        soilBulkDensity.setText(prefs.getString("soil_bulk_density_g_cm3", "1.30"));
        soilFc.setText(prefs.getString("soil_fc_pct", ""));
        soilPwp.setText(prefs.getString("soil_pwp_pct", ""));
        soilPhBuffer.setText(prefs.getString("soil_ph_buffer", ""));
        soilAlDd.setText(prefs.getString("soil_al_dd", ""));
        soilHDd.setText(prefs.getString("soil_h_dd", ""));
        soilCec.setText(prefs.getString("soil_cec", ""));
        soilOm.setText(prefs.getString("soil_om_pct", ""));
        soilEce.setText(prefs.getString("soil_ece_ds_m", ""));
        soilLimeReq.setText(prefs.getString("soil_lime_requirement_kg_ha", ""));
        selectSpinner(soilTestMethod, prefs.getString("soil_test_method", "Metode tidak diketahui"));

        airTemp.setText(prefs.getString("om_temp", ""));
        airRh.setText(prefs.getString("om_rh", ""));
        pressure.setText(prefs.getString("om_pressure", ""));
        rain24.setText(prefs.getString("om_rain", ""));
        et0.setText(prefs.getString("om_et0", ""));
        vpdFromOpenMeteo();
        windSpeed.setText(prefs.getString("om_wind_speed", ""));
        sunHours.setText(prefs.getString("om_sun_hours", ""));

        findViewById(R.id.fnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fnSave).setOnClickListener(v -> saveFieldNote(prefs));
        findViewById(R.id.fnAddFertilizer).setOnClickListener(v -> showFertilizerDialog(prefs));
        findViewById(R.id.fnAddOpt).setOnClickListener(v -> showOptDialog(prefs));
        findViewById(R.id.fnAnalyze).setOnClickListener(v -> runAnalysis(prefs));
        plantDate.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateAutoPhase(); });
        hst.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateAutoPhase(); });
        crop.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateAutoPhase(); });

        updateAutoPhase();
        refreshViews(prefs);
    }

    private void vpdFromOpenMeteo() {
        // VPD is calculated directly by Open-Meteo and also by the local engine from T/RH.
        // The field is named below using the existing fnVpd view when available.
        int id = getResources().getIdentifier("fnVpd", "id", getPackageName());
        if (id != 0) { TextView v = findViewById(id); if (v instanceof EditText) ((EditText)v).setText(prefsSafe("om_vpd")); }
    }

    private String prefsSafe(String key) { return getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, ""); }

    private void selectSpinner(Spinner spinner, String value) { if (value == null) return; android.widget.Adapter a = spinner.getAdapter(); if (a == null) return; for (int i=0;i<a.getCount();i++) if (value.equalsIgnoreCase(String.valueOf(a.getItem(i)))) { spinner.setSelection(i); break; } }

    private void bindSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    private void updateAutoPhase() {
        String c = crop.getText().toString().trim();
        int age = currentHst();
        if (age >= 0) {
            autoPhase.setText("Prediksi fase: " + AgronomyEngine.phase(age, c) + " • HST " + age + " (input " + hst.getText().toString().trim() + " " + ageUnit.getSelectedItem() + ")");
        } else {
            autoPhase.setText("Prediksi fase: isi tanggal tanam atau umur tanaman.");
        }
    }

    private int currentHst() {
        if (validDate(plantDate.getText().toString().trim())) {
            try {
                int d=(int)java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(plantDate.getText().toString().trim(),DATE_FMT),LocalDate.now(WIB));
                if(d>=0) return d;
            } catch(Exception ignored) {}
        }
        double raw=num(hst.getText().toString());
        return Double.isNaN(raw)?-1:ageToHst(raw);
    }

        private int ageToHst(double raw) {
        if (!Double.isFinite(raw) || raw < 0) return -1;
        String u=String.valueOf(ageUnit.getSelectedItem()).toLowerCase(Locale.US);
        if(u.contains("minggu")) return (int)Math.round(raw*7.0);
        if(u.contains("bulan")) return (int)Math.round(raw*30.44);
        if(u.contains("tahun")) return (int)Math.round(raw*365.25);
        return (int)Math.round(raw);
    }


    private void saveFieldNote(SharedPreferences prefs) {
        String c = crop.getText().toString().trim();
        if (c.isEmpty()) { crop.setError("Tanaman wajib diisi"); crop.requestFocus(); return; }
        if (!validDate(date.getText().toString().trim())) { date.setError("Gunakan YYYY-MM-DD"); date.requestFocus(); return; }
        if (!plantDate.getText().toString().trim().isEmpty() && !validDate(plantDate.getText().toString().trim())) {
            plantDate.setError("Gunakan YYYY-MM-DD"); return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("date", date.getText().toString().trim());
            o.put("time", time.getText().toString().trim());
            o.put("crop", c);
            o.put("cultivation", cultivation.getSelectedItem().toString());
            o.put("plantingDate", plantDate.getText().toString().trim());
            putTextNumber(o, "hst", hst);
            putTextNumber(o, "targetYieldTHa", targetYield);
            putTextNumber(o, "areaHa", area);
            o.put("phase", agePhase());
            o.put("type", noteType.getSelectedItem().toString());
            o.put("severity", severity.getSelectedItem().toString());
            o.put("observation", observation.getText().toString().trim());
            o.put("action", action.getText().toString().trim());
            o.put("structuredTemplate", "tanaman/fase; lokasi; gejala/OPT; luas/populasi; kondisi cuaca; tindakan; hasil");
            o.put("soilSource", soilSource.getSelectedItem().toString());
            putTextNumber(o, "soilPh", soilPh);
            putTextNumber(o, "soilMoisturePct", soilMoisture);
            putTextNumber(o, "soilTemperatureC", soilTemp);
            putTextNumber(o, "soilEcUsCm", soilEc);
            putTextNumber(o, "soilNmgKg", soilN);
            putTextNumber(o, "soilPmgKg", soilP);
            putTextNumber(o, "soilKmgKg", soilK);
            putTextNumber(o, "soilDepthCm", soilDepth);
            putTextNumber(o, "soilBulkDensityGcm3", soilBulkDensity);
            putTextNumber(o, "soilFcPct", soilFc);
            putTextNumber(o, "soilPwpPct", soilPwp);
            putTextNumber(o, "soilPhBuffer", soilPhBuffer);
            putTextNumber(o, "soilAlDd", soilAlDd);
            putTextNumber(o, "soilHDd", soilHDd);
            putTextNumber(o, "soilCec", soilCec);
            putTextNumber(o, "soilOmPct", soilOm);
            putTextNumber(o, "soilEceDsM", soilEce);
            putTextNumber(o, "soilLimeRequirementKgHa", soilLimeReq);
            o.put("soilTestMethod", soilTestMethod.getSelectedItem().toString());
            putTextNumber(o, "airTempC", airTemp);
            putTextNumber(o, "airRhPct", airRh);
            putTextNumber(o, "pressureHpa", pressure);
            putTextNumber(o, "rain24mm", rain24);
            putTextNumber(o, "et0Mm", et0);
            putTextNumber(o, "lux", lux);
            putTextNumber(o, "parUmol", par);
            putTextNumber(o, "sunHours", sunHours);
            putTextNumber(o, "windSpeedMs", windSpeed);
            o.put("windDirection", windDirection.getSelectedItem().toString());
            o.put("created", System.currentTimeMillis());

            appendHistory(prefs, KEY_NOTES, o, 500);
            prefs.edit()
                    .putString("crop", c)
                    .putString("farm_area_ha", area.getText().toString().trim())
                    .putString("farm_cultivation_mode", cultivation.getSelectedItem().toString())
                    .putString("farm_planting_date", plantDate.getText().toString().trim())
                    .putString("farm_hst", String.valueOf(currentHst())).putString("farm_age_value", hst.getText().toString().trim()).putString("farm_age_unit", ageUnit.getSelectedItem().toString())
                    .putString("farm_target_yield_t_ha", targetYield.getText().toString().trim())
                    .putString("soil_ph", soilPh.getText().toString().trim())
                    .putString("soil_moisture_pct", soilMoisture.getText().toString().trim())
                    .putString("soil_ec_us_cm", soilEc.getText().toString().trim())
                    .putString("soil_n", soilN.getText().toString().trim())
                    .putString("soil_p", soilP.getText().toString().trim())
                    .putString("soil_k", soilK.getText().toString().trim()).putString("soil_source", soilSource.getSelectedItem().toString()).putString("soil_n_low",soilNLow.getText().toString().trim()).putString("soil_n_high",soilNHigh.getText().toString().trim())
                    .putString("soil_depth_cm", soilDepth.getText().toString().trim())
                    .putString("soil_bulk_density_g_cm3", soilBulkDensity.getText().toString().trim())
                    .putString("soil_fc_pct", soilFc.getText().toString().trim())
                    .putString("soil_pwp_pct", soilPwp.getText().toString().trim())
                    .putString("soil_ph_buffer", soilPhBuffer.getText().toString().trim())
                    .putString("soil_al_dd", soilAlDd.getText().toString().trim())
                    .putString("soil_h_dd", soilHDd.getText().toString().trim())
                    .putString("soil_cec", soilCec.getText().toString().trim())
                    .putString("soil_om_pct", soilOm.getText().toString().trim())
                    .putString("soil_ece_ds_m", soilEce.getText().toString().trim())
                    .putString("soil_lime_requirement_kg_ha", soilLimeReq.getText().toString().trim())
                    .putString("soil_test_method", soilTestMethod.getSelectedItem().toString())
                    .apply();

            Toast.makeText(this, "Catatan lapangan tersimpan.", Toast.LENGTH_SHORT).show();
            observation.setText(""); action.setText("");
            refreshViews(prefs); runAnalysis(prefs);
        } catch (Exception ex) {
            Toast.makeText(this, "Catatan lapangan gagal disimpan.", Toast.LENGTH_SHORT).show();
        }
    }

    private String agePhase() {
        double v = num(hst.getText().toString());
        return Double.isNaN(v) ? "" : AgronomyEngine.phase((int)Math.round(v), crop.getText().toString().trim());
    }

    private void showFertilizerDialog(SharedPreferences prefs) {
        LinearLayout root = dialogRoot();
        EditText dte = edit(root, "Tanggal (YYYY-MM-DD)", LocalDate.now(WIB).format(DATE_FMT), false);
        EditText product = edit(root, "Nama pupuk", "", false);
        Spinner cat = spinner(root, "Jenis pupuk", new String[]{"Pupuk N","Pupuk P","Pupuk K","NPK","Dolomit","Pupuk kandang","POC","Lainnya"});
        EditText dose = edit(root, "Dosis produk per ha", "", true);
        Spinner unit = spinner(root, "Satuan", new String[]{"kg/ha","L/ha"});
        EditText nPct = edit(root, "Kandungan N (%)", "", true);
        EditText pPct = edit(root, "Kandungan P2O5 (%)", "", true);
        EditText kPct = edit(root, "Kandungan K2O (%)", "", true);
        EditText method = edit(root, "Cara pemberian", "", false);
        EditText stage = edit(root, "Umur/fase tanaman", agePhase(), false);
        EditText result = edit(root, "Hasil/pengamatan setelah aplikasi", "", false);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Tambah Riwayat Pemupukan").setView(wrap(root)).setNegativeButton("BATAL", null).setPositiveButton("SIMPAN", null).create();
        d.setOnShowListener(v -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            double dv = num(dose.getText().toString());
            if (!validDate(dte.getText().toString().trim())) { dte.setError("Tanggal harus YYYY-MM-DD"); return; }
            if (product.getText().toString().trim().isEmpty() || Double.isNaN(dv) || dv <= 0) { product.setError("Nama pupuk dan dosis wajib diisi"); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("date", dte.getText().toString().trim()); o.put("product", product.getText().toString().trim());
                o.put("category", cat.getSelectedItem().toString()); o.put("dose", dv); o.put("unit", unit.getSelectedItem().toString());
                o.put("nPct", safe(nPct)); o.put("pPct", safe(pPct)); o.put("kPct", safe(kPct));
                o.put("method", method.getText().toString().trim()); o.put("stage", stage.getText().toString().trim()); o.put("note", result.getText().toString().trim());
                o.put("crop", crop.getText().toString().trim()); o.put("cultivation", cultivation.getSelectedItem().toString()); o.put("created", System.currentTimeMillis());
                appendHistory(prefs, KEY_FERT, o, 300); d.dismiss(); refreshViews(prefs); runAnalysis(prefs);
            } catch (Exception ex) { Toast.makeText(this, "Riwayat pupuk gagal disimpan.", Toast.LENGTH_SHORT).show(); }
        }));
        d.show();
    }

    private void showOptDialog(SharedPreferences prefs) {
        LinearLayout root = dialogRoot();
        EditText dte = edit(root, "Tanggal (YYYY-MM-DD)", LocalDate.now(WIB).format(DATE_FMT), false);
        EditText target = edit(root, "Nama OPT / penyakit", "", false);
        EditText pop = edit(root, "Populasi (unit/ha, bila ada)", "", true);
        EditText affected = edit(root, "Luas/proporsi serangan (%)", "", true);
        EditText obs = edit(root, "Gejala khas / lokasi serangan", "", false);
        Spinner method = spinner(root, "Pengendalian", new String[]{"Monitoring","Kultur teknis","Manual","Mekanis","Biologis","Kimia","Tidak dilakukan"});
        EditText product = edit(root, "Produk (bila ada)", "", false);
        EditText active = edit(root, "Bahan aktif (bila ada)", "", false);
        EditText dose = edit(root, "Dosis sesuai label", "", true);
        Spinner doseUnit = spinner(root, "Satuan dosis", new String[]{"g/ha","kg/ha","ml/ha","L/ha"});
        EditText water = edit(root, "Volume air (L/ha)", "", true);
        EditText controlCost = edit(root, "Biaya pengendalian (Rp/ha)", "", true);
        EditText cropPrice = edit(root, "Harga komoditas (Rp/kg)", "", true);
        EditText damageCoeff = edit(root, "Koefisien kerusakan (kg/ha per unit OPT/ha)", "", true);
        EditText result = edit(root, "Hasil pengendalian", "", false);
        EditText note = edit(root, "Catatan", "", false);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Tambah Riwayat OPT / Pengendalian").setView(wrap(root)).setNegativeButton("BATAL", null).setPositiveButton("SIMPAN", null).create();
        d.setOnShowListener(v -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            if (target.getText().toString().trim().isEmpty()) { target.setError("Nama OPT/penyakit wajib diisi"); return; }
            if (!validDate(dte.getText().toString().trim())) { dte.setError("Tanggal harus YYYY-MM-DD"); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("date", dte.getText().toString().trim()); o.put("target", target.getText().toString().trim());
                putTextNumber(o, "populationPerHa", pop); putTextNumber(o, "affectedPct", affected); o.put("observation", obs.getText().toString().trim());
                o.put("method", method.getSelectedItem().toString()); o.put("product", product.getText().toString().trim()); o.put("active", active.getText().toString().trim());
                putTextNumber(o, "dose", dose); o.put("doseUnit", doseUnit.getSelectedItem().toString()); putTextNumber(o, "waterLHa", water);
                putTextNumber(o, "controlCostRpHa", controlCost); putTextNumber(o, "commodityPriceRpKg", cropPrice); putTextNumber(o, "damageCoefficient", damageCoeff);
                o.put("result", result.getText().toString().trim()); o.put("note", note.getText().toString().trim()); o.put("crop", crop.getText().toString().trim());
                o.put("cultivation", cultivation.getSelectedItem().toString()); o.put("created", System.currentTimeMillis());
                appendHistory(prefs, KEY_OPT, o, 300); d.dismiss(); refreshViews(prefs); runAnalysis(prefs);
            } catch (Exception ex) { Toast.makeText(this, "Riwayat OPT gagal disimpan.", Toast.LENGTH_SHORT).show(); }
        }));
        d.show();
    }

    private void runAnalysis(SharedPreferences prefs) {
        try {
            String c = crop.getText().toString().trim();
            AgronomyEngine.CropProfile p = AgronomyEngine.profile(c);
            int days = hstInt();
            double ph = num(soilPh.getText().toString()), ec = num(soilEc.getText().toString()),
                    moist = num(soilMoisture.getText().toString()), st = num(soilTemp.getText().toString());
            double n = num(soilN.getText().toString()), pp = num(soilP.getText().toString()), k = num(soilK.getText().toString()); 
            double nLow = num(soilNLow.getText().toString());
            double nHigh = num(soilNHigh.getText().toString());
            double at = num(airTemp.getText().toString()), rh = num(airRh.getText().toString()), pressureHpa = num(pressure.getText().toString()), rain = num(rain24.getText().toString()), e0 = num(et0.getText().toString()), luxVal = num(lux.getText().toString()), parVal = num(par.getText().toString()), sunVal = num(sunHours.getText().toString()), wind = num(windSpeed.getText().toString());
            double vpd = AgronomyEngine.vpd(at, rh);
            double[] credit = fertilizerCredit(prefs, c, 90);
            double targetYieldValue = num(targetYield.getText().toString());

            StringBuilder s = new StringBuilder();
            s.append("ANALISIS AGRONOMI TERPADU • ").append(p.name).append("\n");
            s.append("Budidaya: ").append(cultivation.getSelectedItem()).append("\n");
            s.append("Fase: ").append(AgronomyEngine.phase(days, c)).append(" (HST ").append(days >= 0 ? days : "-").append(")\n\n");

            s.append("1. STATUS TANAH\n");
            s.append("pH: ").append(show(ph)).append(" → ").append(AgronomyEngine.classifyPH(ph, p)).append("\n");
            String methodName = soilTestMethod.getSelectedItem().toString();
            double phBuffer = num(soilPhBuffer.getText().toString()), alDd = num(soilAlDd.getText().toString()), hDd = num(soilHDd.getText().toString()), cec = num(soilCec.getText().toString()), om = num(soilOm.getText().toString());
            double ece = num(soilEce.getText().toString()), limeLab = num(soilLimeReq.getText().toString());
            double depth = num(soilDepth.getText().toString()), bd = num(soilBulkDensity.getText().toString());
            double fc = num(soilFc.getText().toString()), pwp = num(soilPwp.getText().toString());
            s.append("Metode uji tanah: ").append(methodName).append("\n");
            s.append("pH-buffer: ").append(show(phBuffer)).append("; Al-dd: ").append(show(alDd)).append("; H-dd: ").append(show(hDd)).append("; CEC: ").append(show(cec)).append("; bahan organik: ").append(show(om)).append(" %\n");
            s.append("Tindakan pH: ").append(AgronomyEngine.limeAdvice(ph, p, cultivation.getSelectedItem().toString(), phBuffer, alDd, hDd, cec, om, limeLab)).append("\n");
            if (Double.isFinite(ece)) s.append("ECe laboratorium: ").append(show(ece)).append(" dS/m → ").append(AgronomyEngine.classifyECe(ece)).append("\n");
            s.append("EC sensor: ").append(show(ec)).append(" µS/cm → ").append(AgronomyEngine.classifyEC(ec, p)).append(" (screening; tidak dikonversi otomatis menjadi ECe)\n");
            if (Double.isFinite(depth) && Double.isFinite(bd)) {
                s.append("Stok lapisan ").append(show(depth)).append(" cm; bulk density ").append(show(bd)).append(" g/cm³: N=").append(show(AgronomyEngine.soilStockKgHa(n,bd,depth))).append(" kg/ha; P=").append(show(AgronomyEngine.soilStockKgHa(pp,bd,depth))).append(" kg/ha; K=").append(show(AgronomyEngine.soilStockKgHa(k,bd,depth))).append(" kg/ha\n");
            }
            s.append("Air tanah: ").append(AgronomyEngine.soilWaterAssessment(moist, fc, pwp, Math.max(1, depth), e0, c)).append("\n");
            if (!Double.isNaN(ph)) s.append("  Tindakan: ").append(AgronomyEngine.amendmentAdvice(ph, p, cultivation.getSelectedItem().toString())).append("\n");
            s.append("N tersedia: ").append(show(n)).append(" mg/kg → ").append(AgronomyEngine.classifyN(n,nLow,nHigh)).append("\n");
            s.append("P tersedia: ").append(show(pp)).append(" mg/kg → ").append(AgronomyEngine.classifyP(pp, methodName)).append("\n");
            s.append("K tersedia: ").append(show(k)).append(" mg/kg → ").append(AgronomyEngine.classifyK(k, methodName)).append("\n");
            s.append("EC: ").append(show(ec)).append(" µS/cm → ").append(AgronomyEngine.classifyEC(ec, p)).append("\n");
            s.append("Kelembapan tanah: ").append(show(moist)).append(" % → ").append(AgronomyEngine.classifyMoisture(moist, c)).append("\n");
            if (!Double.isNaN(st)) s.append("Suhu tanah: ").append(show(st)).append(" °C\n");

            double nNeed = AgronomyEngine.nutrientNeed(n, "N", c, days, credit[0], targetYieldValue, methodName, depth, bd);
            double pNeed = AgronomyEngine.nutrientNeed(pp, "P", c, days, credit[1], targetYieldValue, methodName, depth, bd);
            double kNeed = AgronomyEngine.nutrientNeed(k, "K", c, days, credit[2], targetYieldValue, methodName, depth, bd);
            s.append("\n2. KECUKUPAN NPK & PERKIRAAN DOSIS FASE\n");
            s.append("Kebutuhan screening fase ini: N ").append(show(nNeed)).append(" kg/ha, P2O5 ").append(show(pNeed)).append(" kg/ha, K2O ").append(show(kNeed)).append(" kg/ha.\n");
            s.append("Kredit pupuk tercatat 90 hari: N ").append(show(credit[0])).append(", P2O5 ").append(show(credit[1])).append(", K2O ").append(show(credit[2])).append(" kg/ha.\n");
            s.append("Interpretasi: status rendah → peluang respons pupuk lebih besar; sedang → dosis sebaiknya berimbang; tinggi → jangan menambah unsur tersebut tanpa alasan agronomis.\n");
            s.append("Catatan model: dosis di atas adalah STCR-style screening, bukan dosis legal/spesifik kabupaten. Bila tersedia rekomendasi PUTS, peta status hara, petak omisi, atau persamaan STCR lokal, gunakan itu sebagai prioritas.\n");

            s.append("\n3. AIR, VPD & CUACA\n");
            if (!Double.isNaN(vpd)) { s.append("VPD: ").append(show(vpd)).append(" kPa -> ").append(AgronomyEngine.classifyVpd(vpd)).append("\n"); s.append("VPD + tanah: ").append(AgronomyEngine.vpdCombinedStatus(vpd,moist,fc,pwp,depth,e0,c)).append("\n");
            } else { s.append("VPD: data suhu + RH belum lengkap.\n");}
            s.append(AgronomyEngine.weatherStatus(at, rh, rain, e0, vpd, c)).append("\n");
            if (!Double.isNaN(rain) && !Double.isNaN(e0)) s.append("Neraca sederhana hujan-ET0: ").append(show(rain - e0)).append(" mm; gunakan bersama kelembapan tanah, jangan memakai hujan saja untuk memutuskan irigasi.\n");
            if (!Double.isNaN(pressureHpa)) s.append("Tekanan udara: ").append(show(pressureHpa)).append(" hPa. Tekanan tunggal tidak menentukan hujan/OPT; gunakan bersama tren.\n");
            if (!Double.isNaN(luxVal)) s.append("Cahaya: ").append(show(luxVal)).append(" lux. ");
            if (!Double.isNaN(parVal)) s.append("PAR: ").append(show(parVal)).append(" µmol m⁻² s⁻¹. ");
            if (!Double.isNaN(sunVal)) s.append("Lama penyinaran: ").append(show(sunVal)).append(" jam/hari.\n");
            s.append("Arah angin: ").append(windDirection.getSelectedItem()).append("; kecepatan: ").append(show(wind)).append(" m/s.\n");
            s.append("Kisaran suhu rujukan screening komoditas: ").append(show(p.tempMin)).append("–").append(show(p.tempMax)).append(" °C.\n");

            s.append("\n4. PREDIKSI POTENSI OPT (BERDASARKAN CUACA + RIWAYAT)\n");
            s.append(AgronomyEngine.optRisk(c, at, rh, rain, wind, recentOptSummary(prefs, c))).append("\n");
            s.append("Penting: skor cuaca adalah peringatan dini, bukan diagnosis. Konfirmasi dengan gejala, populasi, luas serangan, dan keberadaan musuh alami.\n");

            s.append("\n5. CATATAN LAPANGAN TERARAH\n");
            s.append("Isi 1 paragraf dengan pola: [tanaman + fase] [petak/lokasi] [gejala/OPT] [luas/populasi] [cuaca/tanah] [tindakan] [hasil].\n");
            String obs = observation.getText().toString().trim();
            String act = action.getText().toString().trim();
            if (!obs.isEmpty()) s.append("Isi terbaru: ").append(obs.replace("\n", " ")).append("\n");
            if (!act.isEmpty()) s.append("Tindakan: ").append(act.replace("\n", " ")).append("\n");

            s.append("\n6. REKOMENDASI MENYELURUH UNTUK PETANI\n");
            appendRecommendations(s, c, p, ph, ec, moist, n, pp, k, at, rh, rain, e0, vpd, days, cultivation.getSelectedItem().toString(), nLow, nHigh, methodName);

            s.append("\n7. DASAR ILMIAH YANG DIPAKAI\n");
            s.append("• FAO-56 Penman–Monteith untuk ET0 dan Kc/ETc.\n");
            s.append("• Sumber bukti untuk AI: ").append(AgronomyEngine.evidenceCitations()).append("\n");
            s.append("• FAO/IRRI/Permentan Indonesia untuk status hara dan kebutuhan pemupukan spesifik lokasi.\n");
            s.append("• STCR-style: kebutuhan tanaman dikurangi kontribusi tanah, kredit pupuk, lalu dibagi efisiensi pemulihan; koefisien lokal harus diutamakan bila tersedia.\n");
            s.append("• Model penyakit: suhu, RH, hujan, dan periode basah/leaf wetness sebagai indikator risiko; model spesifik perlu parameter patogen setempat.\n");
            s.append("• GDD/thermal time untuk fase tanaman dan perkembangan serangga bila parameter Tbase tersedia.\n");
            s.append("• pH/kapur: kebutuhan dosis tidak ditentukan dari pH saja; gunakan pH-buffer/kemasaman tertukar/Al-dd atau uji kebutuhan kapur.\n");
            s.append("• EC: interpretasi salinitas menggunakan ambang ECe sebagai rujukan; EC sensor lapang dipakai sebagai screening dan tren.\n");

            analysisView.setText(s.toString());
            prefs.edit().putString("last_field_analysis", s.toString()).apply();
            soilSummaryView.setText(buildSoilSummary());
            timelineView.setText(buildTimeline(prefs, c));
        } catch (Exception ex) {
            analysisView.setText("Analisis gagal dihitung: " + (ex.getMessage() == null ? "data belum lengkap" : ex.getMessage()));
        }
    }

    private void appendRecommendations(StringBuilder s, String c, AgronomyEngine.CropProfile p,
                                       double ph, double ec, double moist, double n, double pp, double k,
                                       double at, double rh, double rain, double e0, double vpd, int hst, String cultivation, double nLow, double nHigh, String methodName) {
        if (!Double.isNaN(ph) && ph < p.phMin) s.append("• Koreksi pH: jangan menebak dosis kapur. Utamakan uji buffer/kemasaman; gunakan kapur/dolomit sesuai kebutuhan Ca/Mg.\n");
        if (!Double.isNaN(ph) && ph > p.phMax) s.append("• pH tinggi: hentikan dolomit/kapur. Periksa air irigasi dan pertimbangkan pengasaman hanya setelah hitung kebutuhan.\n");
        if (!Double.isNaN(ec) && ec / 1000.0 > p.ecThresholdDsM) s.append("• EC tinggi: kurangi pupuk pekat sekali aplikasi, periksa kualitas air, drainase, dan lakukan pemantauan ulang setelah hujan/irigasi.\n");
        if (!Double.isNaN(moist) && moist < 20) s.append("• Tanah kering: prioritaskan pemenuhan air sebelum memberi pupuk larut, lalu cek ulang kelembapan.\n");
        if (!Double.isNaN(moist) && moist > 85) s.append("• Tanah terlalu lembap: cek drainase dan tunda pupuk yang mudah hilang sampai kondisi memungkinkan.\n");
        if (!Double.isNaN(n) && AgronomyEngine.classifyN(n,nLow,nHigh).equals("Rendah")) s.append("• N rendah: utamakan sumber N yang sesuai fase dan bagi aplikasi agar efisiensi lebih baik.\n");
        if (!Double.isNaN(pp) && AgronomyEngine.classifyP(pp,methodName).equals("Tinggi")) s.append("• P tinggi: jangan menambah P secara rutin; fokuskan dosis pada unsur yang memang kurang.\n");
        if (!Double.isNaN(k) && AgronomyEngine.classifyK(k,methodName).equals("Tinggi")) s.append("• K tinggi: jangan menambah K tanpa bukti kebutuhan; K dapat mengalami luxury consumption.\n");
        if (!Double.isNaN(vpd) && vpd > 2.0) s.append("• VPD tinggi: pantau layu; bila air tersedia, pertahankan kelembapan zona akar dan lakukan aplikasi pupuk pada waktu lebih sejuk.\n");
        if (!Double.isNaN(rain) && !Double.isNaN(e0) && rain < e0) s.append("• Air masuk < kebutuhan atmosfer: cek cadangan air tanah sebelum menetapkan irigasi.\n");
        if (!Double.isNaN(rh) && rh > 90 && !Double.isNaN(rain) && rain > 5) s.append("• RH + hujan tinggi: jadwalkan scouting penyakit lebih rapat; utamakan sanitasi, sirkulasi udara, dan PHT.\n");
        if (cultivation.toLowerCase(Locale.US).contains("organik")) {
            s.append("• Mode ORGANIK: rekomendasi mengutamakan kompos/pupuk kandang matang, pupuk yang diizinkan skema sertifikasi, sanitasi, varietas toleran, agen hayati, dan pengendalian mekanis. Verifikasi bahan dengan standar sertifikasi organik yang berlaku.\n");
        } else {
            s.append("• Mode KONVENSIONAL/PHT: gunakan pemupukan berimbang dan PHT. Pestisida hanya bila monitoring menunjukkan kebutuhan; patuhi label, interval pra-panen, dan rotasi bahan aktif.\n");
        }
        s.append("• Fase saat ini: ").append(AgronomyEngine.phase(hst, c)).append(". Fokuskan tindakan pada kebutuhan fase, bukan hanya umur kalender.\n");
    }

    private double[] fertilizerCredit(SharedPreferences prefs, String cropName, int days) {
        double n = 0, p = 0, k = 0;
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_FERT, "[]"));
            LocalDate cutoff = LocalDate.now(WIB).minusDays(days);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                if (!sameCrop(o.optString("crop", ""), cropName)) continue;
                if (!"kg/ha".equalsIgnoreCase(o.optString("unit", "kg/ha"))) continue;
                LocalDate d = parseDate(o.optString("date", "")); if (d == null || d.isBefore(cutoff)) continue;
                double dose = o.optDouble("dose", 0);
                n += dose * o.optDouble("nPct", 0) / 100.0;
                p += dose * o.optDouble("pPct", 0) / 100.0;
                k += dose * o.optDouble("kPct", 0) / 100.0;
            }
        } catch (Exception ignored) {}
        return new double[]{n, p, k};
    }

    private String recentOptSummary(SharedPreferences prefs, String cropName) {
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_OPT, "[]"));
            LocalDate cutoff = LocalDate.now(WIB).minusDays(30);
            ArrayList<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null || !sameCrop(o.optString("crop", ""), cropName)) continue;
                LocalDate d = parseDate(o.optString("date", "")); if (d != null && !d.isBefore(cutoff)) list.add(o);
            }
            if (list.isEmpty()) return "tidak ada kejadian OPT 30 hari terakhir.";
            list.sort(Comparator.comparingLong((JSONObject x) -> x.optLong("created", 0)).reversed());
            StringBuilder s = new StringBuilder();
            int n = Math.min(3, list.size());
            for (int i = 0; i < n; i++) {
                JSONObject o = list.get(i);
                if (i > 0) s.append("; ");
                s.append(o.optString("target", "OPT"));
                double aff = o.optDouble("affectedPct", Double.NaN);
                if (!Double.isNaN(aff)) s.append(" ").append(show(aff)).append("% serangan");
            }
            return s.toString();
        } catch (Exception ignored) { return "riwayat OPT belum terbaca."; }
    }

    private String buildSoilSummary() {
        return "SUMBER: " + soilSource.getSelectedItem() + "\n" +
                "pH " + show(num(soilPh.getText().toString())) + " • N " + show(num(soilN.getText().toString())) +
                " • P " + show(num(soilP.getText().toString())) + " • K " + show(num(soilK.getText().toString())) + " mg/kg\n" +
                "EC " + show(num(soilEc.getText().toString())) + " µS/cm • kelembapan " + show(num(soilMoisture.getText().toString())) + " %";
    }

    private String buildTimeline(SharedPreferences prefs, String cropName) {
        ArrayList<JSONObject> all = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_NOTES, "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null && sameCrop(o.optString("crop", ""), cropName)) all.add(wrapTimeline(o.optString("date", ""), o.optString("time", ""), "LAPANG • " + o.optString("type", ""), o.optString("observation", ""), o.optLong("created", 0))); }
            JSONArray f = new JSONArray(prefs.getString(KEY_FERT, "[]"));
            for (int i = 0; i < f.length(); i++) { JSONObject o = f.optJSONObject(i); if (o != null && sameCrop(o.optString("crop", ""), cropName)) all.add(wrapTimeline(o.optString("date", ""), "", "PUPUK • " + o.optString("product", ""), show(o.optDouble("dose", 0)) + " " + o.optString("unit", ""), o.optLong("created", 0))); }
            JSONArray z = new JSONArray(prefs.getString(KEY_OPT, "[]"));
            for (int i = 0; i < z.length(); i++) { JSONObject o = z.optJSONObject(i); if (o != null && sameCrop(o.optString("crop", ""), cropName)) all.add(wrapTimeline(o.optString("date", ""), "", "OPT • " + o.optString("target", ""), o.optString("method", "Monitoring"), o.optLong("created", 0))); }
        } catch (Exception ignored) {}
        all.sort(Comparator.comparingLong((JSONObject x) -> x.optLong("created", 0)).reversed());
        StringBuilder s = new StringBuilder("TIMELINE TERPADU • 50 TERBARU\n");
        if (all.isEmpty()) return s.append("Belum ada histori.").toString();
        for (int i = 0; i < Math.min(50, all.size()); i++) {
            JSONObject o = all.get(i); s.append("\n").append(o.optString("date", "--"));
            if (!o.optString("time", "").isEmpty()) s.append(" ").append(o.optString("time"));
            s.append(" • ").append(o.optString("type", "--")).append("\n  ").append(o.optString("text", "").replace("\n", " ")).append("\n");
        }
        return s.toString().trim();
    }

    private JSONObject wrapTimeline(String date, String time, String type, String text, long created) throws Exception {
        JSONObject x = new JSONObject(); x.put("date", date); x.put("time", time); x.put("type", type); x.put("text", text); x.put("created", created); return x;
    }

    private void refreshViews(SharedPreferences prefs) {
        String c = crop.getText().toString().trim();
        soilSummaryView.setText(buildSoilSummary());
        timelineView.setText(buildTimeline(prefs, c));
    }

    private void appendHistory(SharedPreferences prefs, String key, JSONObject o, int max) throws Exception {
        JSONArray old = new JSONArray(prefs.getString(key, "[]")); JSONArray out = new JSONArray();
        int start = Math.max(0, old.length() - max + 1); for (int i = start; i < old.length(); i++) out.put(old.get(i)); out.put(o);
        prefs.edit().putString(key, out.toString()).apply();
    }

    private LinearLayout dialogRoot() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); int p = dp(18); r.setPadding(p,p,p,p); return r; }
    private ScrollView wrap(LinearLayout root) { ScrollView s = new ScrollView(this); s.addView(root); return s; }
    private EditText edit(LinearLayout root, String hint, String value, boolean numeric) {
        EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setTextColor(0xFFFFFFFF); e.setHintTextColor(0xFF9DB0BC); e.setTextSize(14); e.setMinHeight(dp(48));
        e.setGravity(Gravity.TOP); e.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); root.addView(e, new LinearLayout.LayoutParams(-1, -2)); return e;
    }
    private Spinner spinner(LinearLayout root, String label, String[] values) {
        TextView l = new TextView(this); l.setText(label); l.setTextColor(0xFF29C6C7); l.setTextSize(11); root.addView(l);
        Spinner sp = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values); a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); sp.setAdapter(a); root.addView(sp, new LinearLayout.LayoutParams(-1, dp(48))); return sp;
    }
    private void putTextNumber(JSONObject o, String key, EditText e) throws Exception { double v = num(e.getText().toString()); if (!Double.isNaN(v) && !Double.isInfinite(v)) o.put(key, v); }
    private double safe(EditText e) { double v = num(e.getText().toString()); return Double.isNaN(v) ? 0 : v; }
    private double num(String s) { if (s == null || s.trim().isEmpty()) return Double.NaN; try { return Double.parseDouble(s.trim().replace(',','.')); } catch (Exception ex) { return Double.NaN; } }
    private int hstInt() { return currentHst(); }
    private LocalDate parseDate(String s) { try { return LocalDate.parse(s.trim(), DATE_FMT); } catch (Exception ex) { return null; } }
    private boolean validDate(String s) { return parseDate(s) != null; }
    private boolean sameCrop(String a, String b) { return a != null && b != null && (a.trim().equalsIgnoreCase(b.trim()) || AgronomyEngine.normalizeCrop(a).equalsIgnoreCase(AgronomyEngine.normalizeCrop(b))); }
    private String show(double v) { return Double.isNaN(v) || Double.isInfinite(v) ? "--" : String.format(Locale.US, "%.2f", v); }
    private int dp(int v) { return Math.max(1, Math.round(v * getResources().getDisplayMetrics().density)); }
}
