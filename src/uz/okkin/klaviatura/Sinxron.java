package uz.okkin.klaviatura;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server bilan aloqa (kompyuterga bog'liq emas, inok.uz serveri):
 *  - lug'atda yo'q, ko'p yozilgan so'zlarni anonim yuborish (sozlamada o'chirsa bo'ladi);
 *  - boshqalar yozgan va tasdiqlangan yangi so'zlarni olish (kuniga 1 marta);
 *  - fikr va ilova xatolarini yuborish.
 * Hech qachon to'liq matn, gap yoki parol yuborilmaydi - faqat alohida so'zlar.
 */
final class Sinxron {
    static final String MANZIL = "https://inok.uz/claude-pc/klaviatura";
    private static final long KUN = 24 * 60 * 60 * 1000L;
    private static final String QOSHIMCHA_FAYL = "qoshimcha.json";

    interface Javob { void tayyor(boolean ok); }

    private Sinxron() { }

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("sinxron", Context.MODE_PRIVATE);
    }

    /** Anonim qurilma raqami (tasodifiy, shaxsiy ma'lumot emas). */
    static String qurilma(Context c) {
        String q = sp(c).getString("q", null);
        if (q == null) {
            q = UUID.randomUUID().toString();
            sp(c).edit().putString("q", q).apply();
        }
        return q;
    }

    static String versiya(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    // ---------------- ishga tushganda ----------------

    /** Ilova ochilganda: saqlangan qo'shimcha so'zlarni lug'atga yuklaydi. */
    static void qoshimchaniYukla(Context c) {
        try {
            File f = new File(c.getFilesDir(), QOSHIMCHA_FAYL);
            if (!f.exists()) return;
            JSONObject o = new JSONObject(oqi(new FileInputStream(f)));
            JSONArray a = o.optJSONArray("sozlar");
            List<String> s = new ArrayList<>();
            for (int i = 0; a != null && i < a.length(); i++) s.add(a.getString(i));
            Lugat.ol(c).qoshimchaniQoy(s);
        } catch (Exception ignored) { }
    }

    /** Kuniga bir marta: so'zlarni yuborish, yangi so'zlarni olish, xato hisobotini yuborish. */
    static void kunlik(final Context c) {
        final Context ac = c.getApplicationContext();
        long oxirgi = sp(ac).getLong("oxirgi", 0);
        if (System.currentTimeMillis() - oxirgi < KUN) {
            xatoniYubor(ac);
            return;
        }
        sp(ac).edit().putLong("oxirgi", System.currentTimeMillis()).apply();
        // so'zlar ro'yxati asosiy oqimda olinadi (Lugat xaritasi shu oqimda o'zgaradi)
        final List<String> yuborish = new ArrayList<>();
        if (Sozlamalar.b(ac, Sozlamalar.HISSA)) {
            Set<String> yuborilgan = new HashSet<>(sp(ac).getStringSet("yuborilgan", new HashSet<String>()));
            for (String w : Lugat.ol(ac).yangiSozlar(3)) if (!yuborilgan.contains(w)) yuborish.add(w);
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    if (!yuborish.isEmpty()) {
                        JSONObject b = asos(ac);
                        JSONArray a = new JSONArray();
                        for (int i = 0; i < yuborish.size() && i < 300; i++) a.put(yuborish.get(i));
                        b.put("s", a);
                        if (post("/sozlar", b)) {
                            Set<String> y = new HashSet<>(sp(ac).getStringSet("yuborilgan", new HashSet<String>()));
                            y.addAll(yuborish);
                            sp(ac).edit().putStringSet("yuborilgan", y).apply();
                        }
                    }
                } catch (Exception ignored) { }
                try {
                    JSONObject l = new JSONObject(get("/lugat"));
                    int v = l.optInt("versiya", 0);
                    if (v != sp(ac).getInt("lugat_v", -1)) {
                        FileOutputStream f = new FileOutputStream(new File(ac.getFilesDir(), QOSHIMCHA_FAYL));
                        f.write(l.toString().getBytes("UTF-8"));
                        f.close();
                        sp(ac).edit().putInt("lugat_v", v).apply();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override public void run() { qoshimchaniYukla(ac); }
                        });
                    }
                } catch (Exception ignored) { }
                xatoniYubor(ac);
            }
        }, "sinxron").start();
    }

    // ---------------- fikr ----------------

    static void fikrYubor(final Context c, final String matn, final Javob j) {
        final Context ac = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                boolean ok = false;
                try {
                    JSONObject b = asos(ac);
                    b.put("tur", "fikr");
                    b.put("matn", matn);
                    ok = post("/fikr", b);
                } catch (Exception ignored) { }
                final boolean r = ok;
                if (j != null) new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() { j.tayyor(r); }
                });
            }
        }, "fikr").start();
    }

    // ---------------- ilova xatolari ----------------

    private static boolean ornatildi;

    /** Ilova yiqilsa xatoni saqlab qo'yadi, keyingi ishga tushishda yuboriladi. */
    static void xatoUshlagich(Context c) {
        if (ornatildi) return;
        ornatildi = true;
        final Context ac = c.getApplicationContext();
        final Thread.UncaughtExceptionHandler eski = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String s = sw.toString();
                    if (s.length() > 3000) s = s.substring(0, 3000);
                    sp(ac).edit().putString("xato", s).commit();
                } catch (Exception ignored) { }
                if (eski != null) eski.uncaughtException(t, e);
            }
        });
    }

    private static void xatoniYubor(final Context ac) {
        final String x = sp(ac).getString("xato", null);
        if (x == null) return;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    JSONObject b = asos(ac);
                    b.put("tur", "xato");
                    b.put("matn", x);
                    if (post("/fikr", b)) sp(ac).edit().remove("xato").apply();
                } catch (Exception ignored) { }
            }
        }, "xato").start();
    }

    // ---------------- HTTP ----------------

    private static JSONObject asos(Context c) throws Exception {
        JSONObject b = new JSONObject();
        b.put("q", qurilma(c));
        b.put("v", versiya(c));
        b.put("a", "Android " + Build.VERSION.RELEASE + " " + Build.MANUFACTURER);
        return b;
    }

    private static boolean post(String yol, JSONObject b) throws Exception {
        HttpURLConnection u = (HttpURLConnection) new URL(MANZIL + yol).openConnection();
        u.setConnectTimeout(10000);
        u.setReadTimeout(15000);
        u.setDoOutput(true);
        u.setRequestMethod("POST");
        u.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] d = b.toString().getBytes("UTF-8");
        OutputStream o = u.getOutputStream();
        o.write(d);
        o.close();
        int kod = u.getResponseCode();
        u.disconnect();
        return kod == 200;
    }

    private static String get(String yol) throws Exception {
        HttpURLConnection u = (HttpURLConnection) new URL(MANZIL + yol).openConnection();
        u.setConnectTimeout(10000);
        u.setReadTimeout(15000);
        try {
            return oqi(u.getInputStream());
        } finally {
            u.disconnect();
        }
    }

    private static String oqi(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } finally {
            in.close();
        }
    }
}
