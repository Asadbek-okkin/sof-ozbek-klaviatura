package uz.okkin.klaviatura.admin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Sof Özbek klaviatura — admin ilovasi. Server (inok.uz) bilan to'g'ridan-to'g'ri ishlaydi,
 * kompyuter yoki Claude kerak emas. Kirish: bot /klaviatura_admin bergan kalit.
 */
public class Asosiy extends Activity {

    private static final String MANZIL = "https://inok.uz/claude-pc/klaviatura/admin/";
    private static final int FON = 0xFF111317, KARTA = 0xFF1E2127, MATN = 0xFFE8EAED, KULRANG = 0xFF9AA0A6,
            KOK = 0xFF8AB4F8, YASHIL = 0xFF34A853, QIZIL = 0xFFEF5350;

    private final Handler h = new Handler(Looper.getMainLooper());
    private SharedPreferences sp;
    private LinearLayout ichki;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // himoya: ekran rasmini olish va "so'nggi ilovalar" ko'rinishida ma'lumot ko'rinmasin
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(FON);
        getWindow().setNavigationBarColor(FON);
        sp = getSharedPreferences("admin", MODE_PRIVATE);
        if (sp.getString("kalit", "").isEmpty()) kirish();
        else asosiy();
    }

    // ------------------------------------------------------------- kirish

    private void kirish() {
        LinearLayout l = ustun();
        l.setPadding(dp(28), dp(80), dp(28), dp(28));
        l.addView(matn("📊 Sof Özbek Admin", 26, MATN, true));
        TextView iz = matn("Kirish kalitini Telegram botdan oling: /klaviatura_admin", 15, KULRANG, false);
        iz.setPadding(0, dp(10), 0, dp(24));
        l.addView(iz);
        final EditText e = new EditText(this);
        e.setHint("Kalit");
        e.setTextColor(MATN);
        e.setHintTextColor(KULRANG);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        l.addView(e);
        TextView kir = tugma("Kiriş");
        kir.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String k = e.getText().toString().trim();
                if (k.length() < 20) { toast("Kalit noto'g'ri"); return; }
                sp.edit().putString("kalit", k).apply();
                asosiy();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.topMargin = dp(18);
        l.addView(kir, lp);
        setContentView(l);
    }

    // ------------------------------------------------------------- asosiy

    private void asosiy() {
        LinearLayout ildiz = ustun();
        LinearLayout tepa = new LinearLayout(this);
        tepa.setGravity(Gravity.CENTER_VERTICAL);
        tepa.setPadding(dp(18), dp(16), dp(10), dp(8));
        tepa.addView(matn("📊 Klaviatura statistikasi", 20, MATN, true), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView yangila = matn("↻", 26, KOK, true);
        yangila.setPadding(dp(14), 0, dp(14), 0);
        yangila.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { yukla(); }
        });
        tepa.addView(yangila);
        TextView chiq = matn("⎋", 22, KULRANG, false);
        chiq.setPadding(dp(10), 0, dp(10), 0);
        chiq.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sp.edit().remove("kalit").apply(); kirish(); }
        });
        tepa.addView(chiq);
        ildiz.addView(tepa);
        ScrollView sv = new ScrollView(this);
        ichki = ustun();
        ichki.setPadding(dp(12), 0, dp(12), dp(24));
        sv.addView(ichki);
        ildiz.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(ildiz);
        yukla();
    }

    private void yukla() {
        ichki.removeAllViews();
        ichki.addView(matn("Yuklanmoqda...", 15, KULRANG, false));
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final JSONObject s = new JSONObject(sorov("GET", "statistika", null));
                    h.post(new Runnable() { @Override public void run() { chiz(s); } });
                } catch (final Exception e) {
                    h.post(new Runnable() {
                        @Override public void run() {
                            String m = String.valueOf(e.getMessage());
                            if (m.contains("403")) { sp.edit().remove("kalit").apply(); toast("Kalit noto'g'ri yoki almaştirilgan"); kirish(); return; }
                            ichki.removeAllViews();
                            ichki.addView(matn("Ulanib bōlmadi: " + m + "\n\n↻ ni bosib qayta urinib kōring.", 15, QIZIL, false));
                        }
                    });
                }
            }
        }).start();
    }

    private void chiz(JSONObject s) {
        ichki.removeAllViews();
        // umumiy
        LinearLayout k = karta("👥 Foydalanuvchilar");
        LinearLayout q = new LinearLayout(this);
        q.addView(raqam("Jami", s.optInt("jami")), new LinearLayout.LayoutParams(0, -2, 1f));
        q.addView(raqam("Bugun", s.optInt("bugun")), new LinearLayout.LayoutParams(0, -2, 1f));
        q.addView(raqam("7 kun", s.optInt("hafta")), new LinearLayout.LayoutParams(0, -2, 1f));
        q.addView(raqam("30 kun", s.optInt("oy")), new LinearLayout.LayoutParams(0, -2, 1f));
        k.addView(q);
        k.addView(matn("🎤 Ovozli yozişlar: " + s.optInt("ovozSoni") + "    📚 Lug'at qōşimçasi: v"
                + s.optJSONObject("lugat").optInt("versiya") + " (" + s.optJSONObject("lugat").optInt("soni") + " sōz)", 14, KULRANG, false));
        k.addView(matn("Keyingi 4 kunlik reja: " + sana(s.optLong("keyingiReja")), 14, KULRANG, false));

        // kunlik yangi foydalanuvchilar
        LinearLayout kk = karta("📈 Yangi ōrnatganlar (30 kun)");
        JSONArray kun = s.optJSONArray("kunlik");
        int maks = 1;
        for (int i = 0; kun != null && i < kun.length(); i++) maks = Math.max(maks, kun.optJSONArray(i).optInt(1));
        for (int i = 0; kun != null && i < kun.length(); i++) {
            JSONArray x = kun.optJSONArray(i);
            kk.addView(chiziq(x.optString(0).substring(5), x.optInt(1), maks));
        }

        royxat("📲 Platformalar", s.optJSONArray("platformalar"), 5);
        royxat("🌍 Davlatlar", s.optJSONArray("davlatlar"), 30);
        royxat("🏙 Viloyatlar", s.optJSONArray("viloyatlar"), 40);
        royxat("📍 Şaharlar", s.optJSONArray("shaharlar"), 30);
        royxat("📱 Versiyalar (30 kun)", s.optJSONArray("versiyalar"), 20);
        royxat("🤖 Telefonlar", s.optJSONArray("telefonlar"), 20);

        // fikrlar
        LinearLayout fk = karta("💬 Fikrlar");
        JSONArray f = s.optJSONArray("fikrlar");
        if (f == null || f.length() == 0) fk.addView(matn("Hali fikr yōq", 14, KULRANG, false));
        for (int i = 0; f != null && i < f.length(); i++) {
            JSONObject x = f.optJSONObject(i);
            TextView t = matn(x.optString("matn"), 15, MATN, false);
            t.setPadding(0, dp(8), 0, 0);
            fk.addView(t);
            fk.addView(matn(sana(x.optLong("vaqt")) + " • v" + x.optString("v") + " • " + x.optString("a"), 12, KULRANG, false));
        }

        // lug'at: yangi qo'shilganlar va nomzodlar (✕ - olib tashlash)
        LinearLayout lk = karta("📚 Lug'atga qōşilganlar (✕ — olib taşlaş)");
        JSONArray ox = s.optJSONObject("lugat").optJSONArray("oxirgilar");
        for (int i = 0; ox != null && i < ox.length(); i++) lk.addView(sozQatori(ox.optString(i), null));
        LinearLayout nk = karta("⏳ Nomzod sōzlar (neçta odam yozgan)");
        JSONArray nz = s.optJSONArray("nomzodlar");
        for (int i = 0; nz != null && i < nz.length(); i++) {
            JSONArray x = nz.optJSONArray(i);
            nk.addView(sozQatori(x.optString(0), String.valueOf(x.optInt(1))));
        }
        TextView reja = tugma("4 kunlik rejani hozir işga tuşiriş");
        reja.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                amal("reja", null, "Reja işga tuşdi — hisobot Telegramga keladi");
            }
        });
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, dp(50));
        rlp.topMargin = dp(10);
        nk.addView(reja, rlp);

        // xatolar
        LinearLayout xk = karta("🐞 Ilova xatolari");
        JSONArray xa = s.optJSONArray("xatolar");
        if (xa == null || xa.length() == 0) xk.addView(matn("Xato yōq ✅", 14, YASHIL, false));
        for (int i = 0; xa != null && i < xa.length(); i++) {
            JSONObject x = xa.optJSONObject(i);
            String m = x.optString("matn");
            xk.addView(matn(sana(x.optLong("vaqt")) + " • v" + x.optString("v") + " • " + x.optString("a"), 12, KULRANG, false));
            TextView t = matn(m.length() > 400 ? m.substring(0, 400) + "…" : m, 12, QIZIL, false);
            t.setPadding(0, 0, 0, dp(8));
            xk.addView(t);
        }
        ichki.addView(matn("Yangilangan: " + sana(s.optLong("vaqt")) + "  •  har 60 soniyada ō'zi yangilanadi  •  admin v" + versiya(), 12, KULRANG, false));
    }

    // ilova ochiq turganda statistika har daqiqada o'zi yangilanadi
    private final Runnable avto = new Runnable() {
        @Override public void run() {
            if (ichki != null && !sp.getString("kalit", "").isEmpty()) yuklaJim();
            h.postDelayed(this, 60000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        h.postDelayed(avto, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(avto);
    }

    /** Ekranni tozalamasdan yangilash (aylantirilgan joy saqlanadi). */
    private void yuklaJim() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final JSONObject s = new JSONObject(sorov("GET", "statistika", null));
                    h.post(new Runnable() { @Override public void run() { chiz(s); } });
                } catch (Exception ignored) { }
            }
        }).start();
    }

    private String versiya() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return "?"; }
    }

    private void royxat(String nom, JSONArray a, int max) {
        LinearLayout k = karta(nom);
        if (a == null || a.length() == 0) { k.addView(matn("Hali ma'lumot yōq", 14, KULRANG, false)); return; }
        int eng = 1;
        for (int i = 0; i < a.length(); i++) eng = Math.max(eng, a.optJSONArray(i).optInt(1));
        for (int i = 0; i < a.length() && i < max; i++) {
            JSONArray x = a.optJSONArray(i);
            k.addView(chiziq(x.optString(0), x.optInt(1), eng));
        }
    }

    private View sozQatori(final String soz, String son) {
        LinearLayout q = new LinearLayout(this);
        q.setGravity(Gravity.CENTER_VERTICAL);
        q.addView(matn(soz + (son != null ? "   (" + son + ")" : ""), 15, MATN, false), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView x = matn("✕", 18, QIZIL, false);
        x.setPadding(dp(14), dp(6), dp(8), dp(6));
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(Asosiy.this).setMessage("«" + soz + "» lug'atdan olib taşlansinmi? (qaytib qōşilmaydi)")
                        .setPositiveButton("Olib taşlaş", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                try {
                                    JSONObject b = new JSONObject();
                                    b.put("soz", soz);
                                    amal("ochir", b, "«" + soz + "» olib taşlandi");
                                } catch (Exception ignored) { }
                            }
                        }).setNegativeButton("Yōq", null).show();
            }
        });
        q.addView(x);
        return q;
    }

    private void amal(final String yol, final JSONObject b, final String xabar) {
        new Thread(new Runnable() {
            @Override public void run() {
                boolean ok;
                try { sorov("POST", yol, b); ok = true; } catch (Exception e) { ok = false; }
                final boolean r = ok;
                h.post(new Runnable() { @Override public void run() { toast(r ? xabar : "Bajarib bōlmadi"); if (r) yukla(); } });
            }
        }).start();
    }

    // ------------------------------------------------------------- tarmoq

    private String sorov(String usul, String yol, JSONObject b) throws Exception {
        HttpURLConnection u = (HttpURLConnection) new URL(MANZIL + yol).openConnection();
        u.setConnectTimeout(10000);
        u.setReadTimeout(20000);
        u.setRequestMethod(usul);
        u.setRequestProperty("X-Admin-Kalit", sp.getString("kalit", ""));
        if (b != null || "POST".equals(usul)) {
            byte[] d = (b == null ? "{}" : b.toString()).getBytes("UTF-8");
            u.setDoOutput(true);
            u.setRequestProperty("Content-Type", "application/json");
            OutputStream o = u.getOutputStream();
            o.write(d);
            o.close();
        }
        int kod = u.getResponseCode();
        if (kod != 200) { u.disconnect(); throw new Exception("server " + kod); }
        InputStream in = u.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        u.disconnect();
        return out.toString("UTF-8");
    }

    // ------------------------------------------------------------- ko'rinish yordamchilari

    private LinearLayout ustun() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(FON);
        return l;
    }

    private LinearLayout karta(String sarlavha) {
        LinearLayout k = ustun();
        k.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable g = new GradientDrawable();
        g.setColor(KARTA);
        g.setCornerRadius(dp(16));
        k.setBackground(g);
        TextView t = matn(sarlavha, 16, MATN, true);
        t.setPadding(0, 0, 0, dp(8));
        k.addView(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        ichki.addView(k, lp);
        return k;
    }

    private View raqam(String nom, int n) {
        LinearLayout l = ustun();
        l.setBackgroundColor(0);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.addView(matn(String.valueOf(n), 26, KOK, true));
        l.addView(matn(nom, 13, KULRANG, false));
        return l;
    }

    private View chiziq(String nom, int n, int maks) {
        LinearLayout q = new LinearLayout(this);
        q.setGravity(Gravity.CENTER_VERTICAL);
        q.setPadding(0, dp(3), 0, dp(3));
        TextView t = matn(nom, 14, MATN, false);
        q.addView(t, new LinearLayout.LayoutParams(0, -2, 1.3f));
        LinearLayout ustun = new LinearLayout(this);
        View bar = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(KOK);
        g.setCornerRadius(dp(4));
        bar.setBackground(g);
        float ulush = maks == 0 ? 0 : (float) n / maks;
        ustun.addView(bar, new LinearLayout.LayoutParams(0, dp(10), Math.max(ulush, 0.01f)));
        ustun.addView(new View(this), new LinearLayout.LayoutParams(0, dp(10), Math.max(1 - ulush, 0.01f)));
        q.addView(ustun, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView son = matn(String.valueOf(n), 14, KULRANG, false);
        son.setGravity(Gravity.END);
        q.addView(son, new LinearLayout.LayoutParams(dp(48), -2));
        return q;
    }

    private TextView matn(String s, int size, int rang, boolean qalin) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(rang);
        if (qalin) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView tugma(String s) {
        TextView t = matn(s, 16, 0xFF0E1A2B, true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(KOK);
        g.setCornerRadius(dp(14));
        t.setBackground(g);
        return t;
    }

    private static String sana(long t) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(t));
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
